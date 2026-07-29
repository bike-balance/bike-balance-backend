package org.dev.bike.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RealtimeBikeCountCache {

    private static final String NULL_VALUE = "__NULL__";
    private static final String CACHE_KEY_PREFIX = "bike:station:realtime-count:";
    private static final String LOCK_KEY_PREFIX = "bike:station:realtime-count:lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${seoul-bike.realtime-cache.ttl-seconds:10}")
    private long cacheTtlSeconds;

    @Value("${seoul-bike.realtime-cache.null-ttl-seconds:5}")
    private long nullCacheTtlSeconds;

    @Value("${seoul-bike.realtime-cache.lock-ttl-seconds:3}")
    private long lockTtlSeconds;

    @Value("${seoul-bike.realtime-cache.wait-timeout-millis:2500}")
    private long waitTimeoutMillis;

    @Value("${seoul-bike.realtime-cache.poll-interval-millis:50}")
    private long pollIntervalMillis;

    public Integer get(String stationId, Supplier<Integer> loader) {
        if (stationId == null || stationId.isBlank()) {
            return null;
        }

        // 1. redis 캐시 조회: 만약 캐시가 존재하면 바로 리턴 아니면 아래 수행
        String cacheKey = CACHE_KEY_PREFIX + stationId;
        CachedValue cachedValue = readCachedValue(cacheKey);
        if (cachedValue.found()) {
            return cachedValue.value();
        }

        // 2. lock: redis 상의 캐시가 존재하지 않다면, lock을 검.
        String lockKey = LOCK_KEY_PREFIX + stationId;
        String lockToken = UUID.randomUUID().toString();
        LockAttempt lockAttempt = tryLock(lockKey, lockToken);
        if (lockAttempt == LockAttempt.UNAVAILABLE) {
            return loader.get();
        }
        // lock 흭득한 경우: api 호출 후 따릉이 실시간 자전거 대수 캐싱 후 finally => unlock
        if (lockAttempt == LockAttempt.ACQUIRED) {
            try {
                Integer loadedValue = loader.get();
                write(cacheKey, loadedValue);
                return loadedValue;
            } finally {
                unlock(lockKey, lockToken);
            }
        }

        // lock 을 못잡은 요청 = 서울시 api 호출 안하고 대기
        CachedValue waitedValue = waitForCachedValue(cacheKey);
        if (waitedValue.found()) {
            return waitedValue.value();
        }

        lockToken = UUID.randomUUID().toString();
        lockAttempt = tryLock(lockKey, lockToken);
        if (lockAttempt == LockAttempt.UNAVAILABLE) {
            return loader.get();
        }
        if (lockAttempt == LockAttempt.ACQUIRED) {
            try {
                Integer loadedValue = loader.get();
                write(cacheKey, loadedValue);
                return loadedValue;
            } finally {
                unlock(lockKey, lockToken);
            }
        }

        return loader.get();
    }

    private CachedValue waitForCachedValue(String cacheKey) {
        long deadline = System.nanoTime() + Duration.ofMillis(waitTimeoutMillis).toNanos();
        while (System.nanoTime() < deadline) {
            sleep();
            CachedValue cachedValue = readCachedValue(cacheKey);
            if (cachedValue.found()) {
                return cachedValue;
            }
        }
        return CachedValue.miss();
    }

    private CachedValue readCachedValue(String cacheKey) {
        String cachedValue = read(cacheKey);
        if (cachedValue == null) {
            return CachedValue.miss();
        }
        return CachedValue.hit(deserialize(cachedValue));
    }

    private String read(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void write(String cacheKey, Integer value) {
        try {
            if (value == null) {
                redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, Duration.ofSeconds(nullCacheTtlSeconds));
                return;
            }
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(value), Duration.ofSeconds(cacheTtlSeconds));
        } catch (RuntimeException ignored) {
        }
    }

    // redis 분산락 획득 시도 함수 ( lock 획득 결과 상태값 리턴 )
    // setIfAbsent(): SET lockKey lockToken NX EX 5 (레디스 명령어 내부 수행) => NX(키가 없을 때만 저장) 을 의미함.
    // 검증 및 저장 과정이 임계영역으로 수행 됨. 즉, 원자적으로 수행되기 때문에 락 획득 용도로 사용이 가능함.
    // TTL 설정: 서버 장애 시 락이 영원히 남는것을 방지하기 위함.
    private LockAttempt tryLock(String lockKey, String lockToken) {
        try {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(lockTtlSeconds));
            return Boolean.TRUE.equals(locked) ? LockAttempt.ACQUIRED : LockAttempt.LOCKED;
        } catch (RuntimeException e) {
            return LockAttempt.UNAVAILABLE;
        }
    }

    private void unlock(String lockKey, String lockToken) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockToken);
        } catch (RuntimeException ignored) {
        }
    }

    // 문자열 value => 정수 value 역직렬화 함수
    private Integer deserialize(String value) {
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(pollIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record CachedValue(boolean found, Integer value) {
        static CachedValue hit(Integer value) {
            return new CachedValue(true, value);
        }

        static CachedValue miss() {
            return new CachedValue(false, null);
        }
    }

    private enum LockAttempt {
        ACQUIRED, // lock 획득 성공
        LOCKED, // lock 획득 실패
        UNAVAILABLE // redis 사용 불가인 경우 -> redis 서버 오류 및 사용 불가 상태
    }
}

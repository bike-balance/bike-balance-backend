# 🚲 Bike Balance Backend

서울시 공공자전거 따릉이 대여소의 위치와 실시간 자전거 대수를 제공하고, 사용자 권한과 관리자 대시보드를 지원하는 Spring Boot API 서버입니다.

## 주요 기능

<details>
<summary><strong>사용자 기능</strong></summary>


- 회원가입 및 로그인
- JWT 기반 인증 상태 유지
- 현재 로그인 사용자의 최신 정보와 권한 조회
- 지도 화면 영역에 포함된 대여소 조회
- 현재 위치에서 가까운 대여소 조회
- 현재 위치의 기온, 강수량 및 하늘 상태 조회
- 대여소 상세 정보 및 실시간 자전거 대수 조회

</details>

<details>
<summary><strong>관리자 기능</strong></summary>


- `ADMIN`과 `SUPER_ADMIN`의 관리자 대시보드 접근
- 전체 회원 수 조회
- 전체 대여소 수 조회
- `SUPER_ADMIN` 전용 전체 회원 목록 조회
- 회원 목록 10개 단위 페이지네이션
- 일반 사용자와 관리자 간 권한 변경
- 본인 및 다른 최고 관리자의 권한 변경 방지

</details>

<details>
<summary><strong>공통 기술 기능</strong></summary>


- PostGIS를 이용한 공간 검색과 거리 계산
- Redis 캐시 및 분산 락을 이용한 서울시 API 중복 호출 제어
- Spring Security와 JWT를 이용한 인증·인가
- `USER`, `ADMIN`, `SUPER_ADMIN` 기반 인가

</details>

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring JDBC `JdbcClient`
- Spring Security
- Spring Validation
- PostgreSQL 16
- PostGIS 3.4
- Redis 7
- Gradle 9.5.1
- JJWT 0.12.6
- Spring dotenv 5.1.0
- Lombok

## 시스템 구조

```text
Frontend
   |
   | HTTP + Bearer JWT
   v
Spring Boot Backend
   |
   |-- PostgreSQL + PostGIS
   |     |-- 사용자 및 권한 저장
   |     |-- 대여소 위치와 상세 정보 저장
   |     |-- 지도 영역 및 거리순 공간 조회
   |
   |-- Redis
   |     |-- 실시간 자전거 대수 캐시
   |     |-- 캐시 미스 중복 호출 방지용 분산 락
   |
   `-- 서울 열린데이터광장 API
         `-- 대여소별 실시간 자전거 대수 조회
```

## 프로젝트 패키지

```text
org.dev.bike
├── admin       # ADMIN 이상 대시보드 통계
├── redis       # 실시간 자전거 대수 외부 API 및 Redis 캐시
├── security    # JWT 인증 필터와 Spring Security 설정
├── station     # 대여소 공간 조회 및 상세 조회
├── superadmin  # 회원 목록 및 권한 변경
└── user        # 회원가입, 로그인, 현재 사용자 조회
```

## 실행 방법

<details>
<summary><strong>1. 사전 준비</strong></summary>


- Java 17
- PostgreSQL 및 PostGIS
- Redis
- 서울 열린데이터광장 API 키

</details>

<details>
<summary><strong>2. 데이터베이스 준비</strong></summary>


Hibernate 설정이 `ddl-auto=validate`이므로 애플리케이션 실행 전에 테이블을 직접 준비해야 합니다. 현재 저장소에는 별도의 마이그레이션 또는 SQL 스크립트가 포함되어 있지 않습니다.

먼저 PostGIS 확장을 활성화합니다.

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

현재 엔티티와 조회 쿼리에서 사용하는 주요 컬럼은 다음과 같습니다.

```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'))
);

CREATE TABLE bike_station (
    id BIGSERIAL PRIMARY KEY,
    rent_id VARCHAR(100),
    rent_no VARCHAR(100),
    rent_nm VARCHAR(255),
    sta_loc VARCHAR(255),
    hold_num INTEGER,
    sta_add1 VARCHAR(500),
    sta_add2 VARCHAR(500),
    geom geometry(Point, 4326)
);

CREATE INDEX bike_station_geom_idx
    ON bike_station USING GIST (geom);
```

운영 환경에서는 스키마 변경 이력을 Flyway나 Liquibase 같은 마이그레이션 도구로 관리하는 것을 권장합니다.

</details>

<details>
<summary><strong>3. 환경 변수 설정</strong></summary>


`bike` 디렉터리에 `.env` 파일을 생성합니다.

```properties
DB_URL=jdbc:postgresql://localhost:5432/{database_name}
DB_USERNAME={database_username}
DB_PASSWORD={database_password}

# Base64로 인코딩된 충분히 긴 HMAC 키
JWT_SECRET={base64_encoded_jwt_secret}
JWT_EXPIRATION_MILLIS=3600000

SEOUL_BIKE_API_KEY={seoul_open_api_key}
KMA_WEATHER_API_KEY={data_go_kr_kma_api_key}

REDIS_HOST=localhost
REDIS_PORT=6379
```

실시간 자전거 대수 캐시 설정은 `application.properties`에서 조정할 수 있습니다.

| 설정 | 현재 값 | 설명 |
| --- | ---: | --- |
| `seoul-bike.realtime-cache.ttl-seconds` | 15초 | 정상 조회 결과 TTL |
| `seoul-bike.realtime-cache.null-ttl-seconds` | 5초 | 조회 결과가 없을 때의 TTL |
| `seoul-bike.realtime-cache.lock-ttl-seconds` | 3초 | 분산 락 TTL |
| `seoul-bike.realtime-cache.wait-timeout-millis` | 2500ms | 다른 요청의 캐시 생성을 기다리는 시간 |
| `seoul-bike.realtime-cache.poll-interval-millis` | 50ms | 캐시 생성 여부 확인 간격 |

</details>

<details>
<summary><strong>4. 서버 실행</strong></summary>


```bash
./gradlew bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다.

</details>

<details>
<summary><strong>5. 테스트 실행</strong></summary>


```bash
./gradlew test
```

</details>

## 인증과 권한

<details>
<summary><strong>권한 구조</strong></summary>


| 권한 | 공개 API | 현재 사용자 조회 | 관리자 통계 | 회원 관리 및 권한 변경 |
| --- | :---: | :---: | :---: | :---: |
| `USER` | O | O | X | X |
| `ADMIN` | O | O | O | X |
| `SUPER_ADMIN` | O | O | O | O |

`SUPER_ADMIN`은 내부적으로 `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`을 모두 가지므로 `/api/admin/**`에도 접근할 수 있습니다.

</details>

<details>
<summary><strong>공개 및 보호 경로</strong></summary>


- 인증 불필요
  - `/api/users/register`
  - `/api/users/login`
  - `/api/stations/**`
  - `/api/weather/current?lat={latitude}&lng={longitude}`
- 로그인 필요
  - `/api/users/me`
- `ADMIN` 이상
  - `/api/admin/**`
- `SUPER_ADMIN` 전용
  - `/api/super-admin/**`

보호 API에는 다음 헤더를 전달해야 합니다.

```http
Authorization: Bearer {token}
```

JWT에는 사용자 ID, 이름, 권한이 포함되지만, 인증 필터는 요청마다 JWT의 이메일을 기준으로 사용자를 DB에서 다시 조회합니다. 따라서 DB에서 권한이 변경되면 기존 JWT를 사용한 이후 요청에도 최신 권한이 적용됩니다.

프론트엔드에서 최신 이름과 권한을 표시할 때는 `/api/users/me` 응답을 기준으로 로컬 인증 상태를 갱신해야 합니다.

</details>

<details>
<summary><strong>최초 최고 관리자 생성</strong></summary>


공개 회원가입 계정은 항상 `USER`로 생성되며 비밀번호는 BCrypt로 해시됩니다. 최초 최고 관리자는 회원가입으로 계정을 만든 다음 DB에서 권한만 변경하는 방식이 가장 간단합니다.

```sql
UPDATE users
SET role = 'SUPER_ADMIN'
WHERE email = 'super-admin@example.com';
```

</details>

## API 요약

| Method | Endpoint | 권한 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/users/register` | 공개 | 회원가입 및 JWT 발급 |
| `POST` | `/api/users/login` | 공개 | 로그인 및 JWT 발급 |
| `GET` | `/api/users/me` | 로그인 | 현재 사용자 최신 정보 조회 |
| `GET` | `/api/stations` | 공개 | 지도 영역 대여소 조회 |
| `GET` | `/api/stations/nearby` | 공개 | 현재 위치 기준 가까운 대여소 조회 |
| `GET` | `/api/stations/{id}` | 공개 | 대여소 상세 및 실시간 대수 조회 |
| `GET` | `/api/admin/users/count` | ADMIN 이상 | 전체 회원 수 조회 |
| `GET` | `/api/admin/stations/count` | ADMIN 이상 | 전체 대여소 수 조회 |
| `GET` | `/api/super-admin/users` | SUPER_ADMIN | 회원 목록 페이지 조회 |
| `PATCH` | `/api/super-admin/users/{userId}/role` | SUPER_ADMIN | 사용자 권한 변경 |

## API 상세

<details>
<summary><strong>회원가입</strong> — POST /api/users/register</summary>


```http
POST /api/users/register
Content-Type: application/json
```

```json
{
  "username": "tester",
  "password": "password",
  "email": "tester@example.com"
}
```

검증 조건:

- 이름: 필수, 최대 50자
- 이메일: 필수, 이메일 형식, 최대 100자, 중복 불가
- 비밀번호: 필수, 2~255자

Response `201 Created`

```json
{
  "userId": 1,
  "username": "tester",
  "email": "tester@example.com",
  "role": "USER",
  "token": "jwt-token"
}
```

</details>

<details>
<summary><strong>로그인</strong> — POST /api/users/login</summary>


```http
POST /api/users/login
Content-Type: application/json
```

```json
{
  "email": "tester@example.com",
  "password": "password"
}
```

Response `200 OK`

```json
{
  "userId": 1,
  "username": "tester",
  "email": "tester@example.com",
  "role": "USER",
  "token": "jwt-token"
}
```

</details>

<details>
<summary><strong>현재 사용자 조회</strong> — GET /api/users/me</summary>


DB에 저장된 최신 이름, 이메일, 권한을 반환합니다.

```http
GET /api/users/me
Authorization: Bearer {token}
```

Response `200 OK`

```json
{
  "userId": 1,
  "username": "tester",
  "email": "tester@example.com",
  "role": "ADMIN"
}
```

</details>

<details>
<summary><strong>현재 지도 영역 대여소 조회</strong> — GET /api/stations</summary>


```http
GET /api/stations?south={south}&west={west}&north={north}&east={east}
```

| Query | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `south` | number | O | 남쪽 위도 |
| `west` | number | O | 서쪽 경도 |
| `north` | number | O | 북쪽 위도 |
| `east` | number | O | 동쪽 경도 |

- 위도는 `-90~90`, 경도는 `-180~180` 범위만 허용합니다.
- `south <= north`, `west <= east`여야 합니다.
- 한 번에 최대 1,000개를 반환합니다.

Response `200 OK`

```json
[
  {
    "id": 1,
    "lat": 37.566,
    "lng": 126.978
  }
]
```

</details>

<details>
<summary><strong>가까운 대여소 조회</strong> — GET /api/stations/nearby</summary>


```http
GET /api/stations/nearby?lat={lat}&lng={lng}&limit=5
```

| Query | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | :---: | ---: | --- |
| `lat` | number | O | - | 현재 위치 위도 |
| `lng` | number | O | - | 현재 위치 경도 |
| `limit` | integer | X | 5 | 조회 개수, 1~20 |

Response `200 OK`

```json
[
  {
    "id": 1,
    "rentNm": "서울시청 대여소",
    "lat": 37.566,
    "lng": 126.978,
    "distanceMeters": 120.5
  }
]
```

`distanceMeters`는 두 좌표 사이의 직선거리이며 도보 경로나 도로 상황은 반영하지 않습니다.

</details>

<details>
<summary><strong>대여소 상세 조회</strong> — GET /api/stations/{id}</summary>


```http
GET /api/stations/{id}
```

Response `200 OK`

```json
{
  "id": 1,
  "rentId": "ST-1",
  "rentNo": "001",
  "rentNm": "서울시청 대여소",
  "staLoc": "중구",
  "holdNum": 10,
  "staAdd1": "서울특별시 중구 ...",
  "staAdd2": "",
  "lat": 37.566,
  "lng": 126.978,
  "realtimeBikeCount": 3
}
```

대여소가 없으면 `404 Not Found`를 반환합니다. 서울시 API 또는 Redis에 일시적인 문제가 생기면 대여소 정보는 반환하되 `realtimeBikeCount`가 `null`일 수 있습니다.

</details>

<details>
<summary><strong>전체 회원 수 조회</strong> — GET /api/admin/users/count</summary>


```http
GET /api/admin/users/count
Authorization: Bearer {admin-token}
```

Response `200 OK`

```json
{
  "totalUserCount": 120
}
```

</details>

<details>
<summary><strong>전체 대여소 수 조회</strong> — GET /api/admin/stations/count</summary>


```http
GET /api/admin/stations/count
Authorization: Bearer {admin-token}
```

Response `200 OK`

```json
{
  "totalStationCount": 2750
}
```

</details>

<details>
<summary><strong>회원 목록 조회</strong> — GET /api/super-admin/users</summary>


`SUPER_ADMIN`만 호출할 수 있습니다. 회원은 `userId` 내림차순으로 정렬되며 페이지당 10명씩 반환됩니다.

```http
GET /api/super-admin/users?page=0
Authorization: Bearer {super-admin-token}
```

| Query | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | :---: | ---: | --- |
| `page` | integer | X | 0 | 0부터 시작하는 페이지 번호 |

Response `200 OK`

```json
{
  "users": [
    {
      "userId": 15,
      "username": "member",
      "email": "member@example.com",
      "role": "USER"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 21,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

음수 페이지 번호는 `400 Bad Request`를 반환합니다.

</details>

<details>
<summary><strong>사용자 권한 변경</strong> — PATCH /api/super-admin/users/{userId}/role</summary>


`SUPER_ADMIN`만 호출할 수 있으며 `USER`와 `ADMIN` 사이의 변경만 허용됩니다.

```http
PATCH /api/super-admin/users/{userId}/role
Authorization: Bearer {super-admin-token}
Content-Type: application/json
```

```json
{
  "role": "ADMIN"
}
```

Response `200 OK`

```json
{
  "userId": 2,
  "username": "manager",
  "email": "manager@example.com",
  "role": "ADMIN"
}
```

다음 변경은 허용되지 않습니다.

- 자신의 권한 변경
- 다른 `SUPER_ADMIN`의 권한 변경
- API를 통한 `SUPER_ADMIN` 권한 부여
- `USER`, `ADMIN` 이외의 값 지정

</details>

## 주요 구현 설명

### PostGIS 공간 조회

- 지도 조회는 `ST_MakeEnvelope`로 화면 영역을 만들고 `geom && envelope`로 공간 인덱스를 먼저 활용합니다.
- `ST_Contains`로 실제 화면 영역 안에 포함된 대여소만 반환합니다.
- 가까운 대여소는 PostGIS KNN 연산자 `<->`로 정렬합니다.
- `ST_Distance(geography, geography)`로 미터 단위 거리를 계산합니다.

### Redis 캐시와 분산 락

- 대여소 상세 조회 시 대여소 ID를 키로 실시간 자전거 대수를 캐시합니다.
- 캐시 미스가 동시에 발생하면 Redis `SET NX` 방식의 락으로 외부 API 중복 호출을 줄입니다.
- 락 소유자만 해제할 수 있도록 UUID 토큰과 Lua 스크립트를 사용합니다.
- 조회 결과가 없을 때도 `__NULL__` 값을 짧게 캐시하여 반복 호출을 줄입니다.
- Redis 사용이 불가능하면 캐시와 락을 건너뛰고 서울시 API를 직접 호출합니다.
- 서울시 API 요청 제한 시간은 3초입니다.

### JWT 인증과 최신 권한 반영

- 회원가입과 로그인 성공 시 HMAC 서명 JWT를 발급합니다.
- JWT의 subject에는 이메일이 저장됩니다.
- JWT에는 `userId`, `username`, `role` 클레임도 포함됩니다.
- 요청마다 이메일을 사용해 DB에서 사용자를 다시 조회하고 현재 권한으로 Spring Security 인증 객체를 만듭니다.
- 관리자에 의해 권한이 변경된 사용자는 다시 로그인하지 않아도 다음 API 요청부터 최신 권한이 적용됩니다.
- 프론트 화면의 권한 표시는 `/api/users/me`를 호출해 동기화합니다.

## HTTP 상태 코드

| 상태 | 의미 |
| --- | --- |
| `200 OK` | 조회, 로그인 또는 권한 변경 성공 |
| `201 Created` | 회원가입 성공 |
| `400 Bad Request` | 입력값, 좌표, 페이지 번호 또는 권한 변경 요청 오류 |
| `401 Unauthorized` | 인증 정보가 유효하지 않거나 현재 사용자를 찾을 수 없음 |
| `403 Forbidden` | 필요한 관리자 권한이 없음 |
| `404 Not Found` | 대여소 또는 대상 사용자를 찾을 수 없음 |

## 테스트

현재 테스트에는 다음 항목이 포함됩니다.

- Spring 애플리케이션 컨텍스트 로딩
- 관리자 대시보드 회원·대여소 집계 서비스
- 최고 관리자 회원 목록의 10개 단위 페이지네이션과 정렬

```bash
./gradlew test
```

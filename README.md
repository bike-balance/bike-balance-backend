# 🚲 bike-balance-backend

서울시 공공자전거 따릉이 대여소 위치와 실시간 자전거 대수를 제공하는 Spring Boot 백엔드 API 서버입니다.

## ✨ 핵심 기능

- 지도 현재 화면 영역 안의 따릉이 대여소 마커 조회
- 현재 위치 기준 가까운 대여소 5곳 조회
- 대여소 상세 정보 조회
- 서울 열린데이터광장 따릉이 API 연동을 통한 실시간 자전거 대수 조회
- Redis 캐시와 분산락을 이용한 실시간 대수 API 호출량 제어
- 회원가입, 로그인, JWT 발급
- Spring Security 기반 인증/인가 처리

## 🛠️ 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- PostgreSQL 16
- PostGIS 3.4
- Redis 7
- Gradle 9.5.1
- JWT 0.12.6
- Spring dotenv 5.1.0

## 🏗️ 시스템 아키텍처

```text
Frontend
   |
   | HTTP API
   v
Spring Boot Backend
   |
   |-- PostgreSQL + PostGIS
   |     - 대여소 위치/상세 정보 저장
   |     - 지도 영역 조회
   |     - 현재 위치 기준 거리순 조회
   |
   |-- Redis
   |     - 실시간 자전거 대수 캐시
   |     - 캐시 미스 시 중복 외부 API 호출 방지용 락
   |
   |-- Seoul Open API
         - 대여소별 실시간 자전거 대수 조회
```

## 🚀 실행 방법

### 1. 사전 준비

- Java 17
- PostgreSQL
- PostGIS
- Redis
- 서울 열린데이터광장 API Key

### 2. 데이터베이스 준비

PostgreSQL 데이터베이스에 PostGIS 확장을 활성화합니다.

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

대여소와 사용자 테이블은 프로젝트에 포함된 SQL 파일을 기준으로 생성합니다.

```text
user.sql
bike_station.sql
```

### 3. 환경 변수 설정

`bike` 폴더 아래에 `.env` 파일을 만들고 아래 값을 설정합니다.

```properties
DB_URL=jdbc:postgresql://localhost:5432/{database_name}
DB_USERNAME={database_username}
DB_PASSWORD={database_password}

JWT_SECRET={jwt_secret}
JWT_EXPIRATION_MILLIS=3600000

SEOUL_BIKE_API_KEY={seoul_open_api_key}

REDIS_HOST=localhost
REDIS_PORT=6379
```

### 4. 서버 실행

```bash
./gradlew bootRun
```

기본 서버 주소는 아래와 같습니다.

```text
http://localhost:8080
```

### 5. 테스트 실행

```bash
./gradlew test
```

## 📚 API 문서

### 사용자 회원가입

```http
POST /api/users/register
Content-Type: application/json
```

Request

```json
{
  "username": "tester",
  "password": "password",
  "email": "tester@example.com"
}
```

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

### 사용자 로그인

```http
POST /api/users/login
Content-Type: application/json
```

Request

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

공개 회원가입으로 생성되는 계정은 항상 `USER` 권한을 가집니다. 관리자 계정은 DB에서
`role` 값을 `ADMIN`으로 직접 지정하며, `/api/admin/**` 경로는 관리자만 접근할 수 있습니다.

기존 `users` 테이블에는 서버 실행 전에 권한 컬럼을 추가해야 합니다.

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'));
```

관리자로 지정할 계정은 비밀번호를 BCrypt 해시로 저장하고 `role`을 `ADMIN`으로 설정합니다.

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

### 현재 지도 영역 대여소 조회

```http
GET /api/stations?south={south}&west={west}&north={north}&east={east}
```

Query Parameters

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `south` | number | 남서쪽 위도 |
| `west` | number | 남서쪽 경도 |
| `north` | number | 북동쪽 위도 |
| `east` | number | 북동쪽 경도 |

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

### 가까운 대여소 조회

```http
GET /api/stations/nearby?lat={lat}&lng={lng}&limit=5
```

Query Parameters

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `lat` | number | 현재 위치 위도 |
| `lng` | number | 현재 위치 경도 |
| `limit` | number | 조회 개수, 기본값 5, 최대 20 |

Response `200 OK`

```json
[
  {
    "id": 1,
    "rentNm": "대여소명",
    "lat": 37.566,
    "lng": 126.978,
    "distanceMeters": 120.5
  }
]
```

`distanceMeters`는 좌표 간 일직선 기준 거리이며, 도보 경로, 건물, 도로, 횡단보도는 고려하지 않습니다.

### 대여소 상세 조회

```http
GET /api/stations/{id}
```

Path Parameters

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 대여소 ID |

Response `200 OK`

```json
{
  "id": 1,
  "rentId": "ST-1",
  "rentNo": "001",
  "rentNm": "대여소명",
  "staLoc": "중구",
  "holdNum": 10,
  "staAdd1": "서울특별시 ...",
  "staAdd2": "",
  "lat": 37.566,
  "lng": 126.978,
  "realtimeBikeCount": 3
}
```

## ⚙️ 주요 기술 구현

### PostGIS 기반 공간 조회

- `ST_MakeEnvelope`로 현재 지도 영역을 사각형 범위로 생성합니다.
- `geom && envelope` 조건으로 공간 인덱스를 먼저 활용합니다.
- `ST_Contains`로 실제 화면 영역 안에 포함된 대여소만 조회합니다.

### 현재 위치 기준 가까운 대여소 조회

- `geom <-> 현재좌표` 연산자를 사용해 좌표 기준 가까운 대여소를 정렬합니다.
- `ST_Distance(geography, geography)`로 미터 단위 거리를 계산합니다.
- 기본 조회 개수는 5개이며, API에서는 최대 20개까지 허용합니다.

### Redis 캐시와 분산락

- 대여소 상세 조회 시 서울시 API에서 실시간 자전거 대수를 가져옵니다.
- 같은 대여소에 대한 반복 요청은 Redis 캐시에서 처리합니다.
- 캐시 미스가 동시에 발생하면 Redis `setIfAbsent` 기반 락으로 중복 외부 API 호출을 줄입니다.
- 실시간 값이 없을 때도 짧은 TTL로 null 값을 캐시해 불필요한 재호출을 줄입니다.

### JWT 인증

- 회원가입과 로그인 성공 시 JWT를 발급합니다.
- `/api/users/register`, `/api/users/login`, `/api/stations/**`는 인증 없이 접근할 수 있습니다.
- 그 외 API는 `Authorization: Bearer {token}` 헤더가 필요합니다.

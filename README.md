# PLANWITH API Gateway

PLANWITH 마이크로서비스의 단일 진입점 역할을 하는 API Gateway입니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.7
- Spring Cloud 2025.1.2
- Spring Cloud Gateway Server Web MVC
- Spring Cloud Netflix Eureka Client
- Spring Security OAuth2 Resource Server (JWT)
- Gradle Wrapper

## 현재 구성

| 항목 | 값 |
|------|-----|
| 애플리케이션 이름 | `gateway` |
| 포트 | `8000` |
| Eureka | `http://localhost:8761/eureka/` |
| User BE 라우트 | `lb://fo-user-be` → `/api/v1/**`, `/oauth2/jwks` |
| JWT JWKS | `${JWT_JWK_SET_URI}` (기본 `http://localhost:8080/oauth2/jwks`) |

### 연동 동작

1. 공개 경로(회원가입/로그인/Refresh/약관/JWKS 등)는 JWT 없이 통과
2. 그 외 경로는 Access Token(RS256) 검증
3. 클라이언트 `X-Auth-*` / `X-Gateway-Internal-Token` 헤더는 제거·무시
4. Gateway가 `X-Gateway-Internal-Token`과 (인증 시) `X-Auth-*`, `X-Request-Id`를 Backend로 전달
5. CORS는 Frontend Origin만 허용 (`credentials` 포함)

`GATEWAY_INTERNAL_TOKEN`은 Backend(`fo-user-be`)와 **동일**해야 합니다.

## 실행 순서

1. Discovery (`planwith_discovery`) — `8761`
2. FO User Backend (`fo-user-be`) — `8080`, Eureka 등록
3. Gateway — `8000`

```powershell
$env:GATEWAY_INTERNAL_TOKEN = "local-gateway-internal-token"
$env:JWT_JWK_SET_URI = "http://localhost:8080/oauth2/jwks"
$env:JWT_ISSUER = "http://localhost:8080"
$env:JWT_AUDIENCE = "planwith-api"
.\gradlew.bat bootRun
```

Gateway 기본 주소: http://localhost:8000

## 테스트

```powershell
.\gradlew.bat test
```

`test` 프로파일에서는 Eureka Client와 JWT Resource Server를 비활성화합니다.

## 환경변수

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8000` | Gateway 포트 |
| `EUREKA_DEFAULT_ZONE` | `http://localhost:8761/eureka/` | Eureka 주소 |
| `GATEWAY_INTERNAL_TOKEN` | `local-gateway-internal-token` | Backend Trust 공유 Secret |
| `JWT_JWK_SET_URI` | `http://localhost:8080/oauth2/jwks` | fo-user-be JWKS |
| `JWT_ISSUER` | `http://localhost:8080` | Access Token iss |
| `JWT_AUDIENCE` | `planwith-api` | Access Token aud |
| `CORS_ALLOWED_ORIGINS` | (yaml 목록) | Frontend Origin |

상세 예시는 `.env.example`을 참고하세요.

# PLANWITH API Gateway

PLANWITH 마이크로서비스의 단일 진입점 역할을 하는 API Gateway입니다.

현재는 Gateway 실행과 Eureka 연결에 필요한 최소 설정만 적용되어 있습니다. 실제 서비스 라우팅, Spring Security, Swagger UI 통합은 하위 서비스와 인증 정책이 확정된 후 추가합니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.7
- Spring Cloud 2025.1.2
- Spring Cloud Gateway Server WebFlux
- Spring Cloud Netflix Eureka Client
- Gradle 9.5.1

## 현재 구성

- 애플리케이션 이름: `gateway`
- 기본 Gateway 포트: `8000`
- 기본 Eureka 주소: `http://localhost:8761/eureka/`
- Eureka 등록 시 IP 주소 우선 사용
- 테스트 실행 시 Eureka Client 비활성화

하위 서비스 정보가 아직 없으므로 Gateway 라우트는 등록되어 있지 않습니다. 라우트가 없는 상태에서는 Gateway가 정상 실행되더라도 전달할 API 경로가 없어 요청에 `404 Not Found`가 반환될 수 있습니다.

## 실행 환경

다음 프로그램이 필요합니다.

- JDK 17
- 프로젝트에 포함된 Gradle Wrapper
- 서비스 검색 기능을 사용할 경우 Eureka Discovery Server

Java 버전을 확인합니다.

```powershell
java -version
```

## 실행 방법

### 1. Discovery Server 실행

기본 설정에서는 Eureka Discovery Server가 다음 주소에서 실행 중이어야 합니다.

```text
http://localhost:8761
```

### 2. Gateway 실행

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

빌드된 JAR로 실행:

```powershell
.\gradlew.bat clean bootJar
java -jar .\build\libs\gateway-0.0.1-SNAPSHOT.jar
```

Gateway 기본 주소:

```text
http://localhost:8000
```

### 3. 테스트

```powershell
.\gradlew.bat test
```

테스트에서는 외부 Discovery Server 없이 Spring Context를 검증할 수 있도록 Eureka Client를 비활성화합니다.

## 환경변수

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8000` | Gateway 실행 포트 |
| `EUREKA_DEFAULT_ZONE` | `http://localhost:8761/eureka/` | Eureka 서비스 등록 주소 |
| `EUREKA_PREFER_IP_ADDRESS` | `true` | Eureka 등록 시 호스트명 대신 IP 주소 사용 여부 |

환경변수 적용 예시:

```powershell
$env:SERVER_PORT = "8080"
$env:EUREKA_DEFAULT_ZONE = "http://discovery:8761/eureka/"
$env:EUREKA_PREFER_IP_ADDRESS = "true"
.\gradlew.bat bootRun
```

## 라우트 추가 방법

이 프로젝트는 Gateway Server WebFlux를 사용하므로 라우트 설정 경로는 `spring.cloud.gateway.server.webflux.routes`입니다.

다음은 향후 `USER-SERVICE`가 Eureka에 등록되었을 때 사용할 수 있는 예시입니다. 서비스 ID와 외부 공개 경로는 실제 서비스가 확정된 후 변경해야 합니다.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service
              uri: lb://USER-SERVICE
              predicates:
                - Path=/api/users/**
```

`lb://USER-SERVICE`는 Eureka에서 조회한 서비스 인스턴스를 대상으로 로드 밸런싱합니다. 대상 서비스가 등록되지 않은 경우 Gateway는 해당 요청을 정상적으로 전달할 수 없습니다.

---

## 추후 Spring Security 적용 계획

> 아래 내용은 아직 프로젝트에 적용되지 않은 예정 사항입니다.

Gateway에서 JWT를 검증하는 OAuth 2.0 Resource Server 방식을 권장합니다. 토큰 발급은 인증 서버가 담당하고, Gateway는 서명·만료 시간·발급자와 접근 권한을 검증한 후 요청을 하위 서비스로 전달합니다.

### 추가할 의존성

`build.gradle`의 `dependencies`에 다음 항목을 추가합니다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
testImplementation 'org.springframework.security:spring-security-test'
```

`spring-boot-starter-oauth2-resource-server`가 Spring Security의 Resource Server 구성에 필요한 의존성을 제공하므로 `spring-boot-starter-security`를 중복해서 추가하지 않습니다.

### JWT 설정 예시

인증 서버가 OpenID Connect 또는 Authorization Server Metadata를 제공하는 경우:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
```

인증 서버의 JWK 주소를 직접 사용해야 하는 경우에는 발급자와 JWK 주소를 함께 설정합니다.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}
```

비밀키를 Gateway 소스 코드나 `application.yaml`에 직접 저장하지 않습니다. 운영 환경에서는 환경변수 또는 Secret Manager를 사용합니다.

### Security 설정 예시

예상 파일:

```text
src/main/java/com/planwith/gateway/config/SecurityConfig.java
```

```java
package com.planwith.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/docs/**"
    };

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(
                NoOpServerSecurityContextRepository.getInstance()
            )
            .authorizeExchange(authorize -> authorize
                .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            )
            .build();
    }
}
```

위 예시는 Bearer JWT를 사용하는 무상태 API Gateway 기준입니다. 브라우저 쿠키 기반 로그인이나 세션 인증을 추가한다면 CSRF를 일괄 비활성화하면 안 되며 인증 방식에 맞게 다시 설계해야 합니다.

### Security 적용 시 확인사항

- 공개 API, 로그인·회원가입 API, Swagger 경로를 구체적으로 구분합니다.
- 기본 정책은 `anyExchange().authenticated()`로 유지합니다.
- 역할 또는 Scope 기반 권한 검사가 필요하면 `hasRole`, `hasAuthority` 또는 `SCOPE_` 권한을 사용합니다.
- CORS 허용 Origin을 `*`로 열지 않고 프론트엔드 주소로 제한합니다.
- `Authorization` 헤더와 토큰 값은 로그에 남기지 않습니다.
- Gateway 검증만 신뢰하지 않고 중요 권한은 하위 서비스에서도 다시 검증합니다.
- 인증 실패 `401`과 권한 부족 `403`의 JSON 응답 형식을 통일합니다.
- Swagger 공개 여부는 운영 프로필에서 별도로 제어합니다.

---

## 추후 Swagger/OpenAPI 적용 계획

> 아래 내용은 아직 프로젝트에 적용되지 않은 예정 사항입니다.

현재 프로젝트는 WebFlux 기반이므로 Spring Boot 4 호환 `springdoc-openapi` WebFlux Starter를 사용합니다.

### 추가할 의존성

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
```

적용 후 Gateway 자체 API 문서는 기본적으로 다음 경로에서 확인할 수 있습니다.

```text
Swagger UI: http://localhost:8000/swagger-ui.html
OpenAPI JSON: http://localhost:8000/v3/api-docs
```

Gateway에 Controller가 없다면 Gateway 자체 OpenAPI 문서는 비어 있을 수 있습니다. 마이크로서비스 전체 API를 표시하려면 각 하위 서비스가 `/v3/api-docs`를 제공해야 하며, Gateway에서 각 문서 경로를 라우팅한 뒤 Swagger UI에 등록해야 합니다.

### 하위 서비스 OpenAPI 문서 라우트 예시

다음 예시는 Gateway의 `/docs/user-service` 요청을 `USER-SERVICE`의 `/v3/api-docs`로 전달합니다.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-docs
              uri: lb://USER-SERVICE
              predicates:
                - Path=/docs/user-service
              filters:
                - RewritePath=/docs/user-service, /v3/api-docs
```

### Swagger UI 문서 목록 예시

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    urls:
      - name: user-service
        url: /docs/user-service
  api-docs:
    enabled: true
```

서비스가 추가되면 문서 라우트와 `springdoc.swagger-ui.urls` 항목을 함께 추가합니다.

### Swagger 적용 시 확인사항

- 각 서비스가 동일한 외부 경로 규칙과 OpenAPI 3 규격을 사용하도록 정리합니다.
- Swagger UI의 `Try it out` 요청이 Gateway 공개 경로를 사용하도록 각 서비스의 OpenAPI `servers` 값을 확인합니다.
- JWT 인증 API에는 OpenAPI Bearer Security Scheme을 선언합니다.
- 개발 환경에서는 Swagger 경로를 공개할 수 있지만 운영 환경에서는 비활성화하거나 관리자 권한으로 제한합니다.
- API 문서 라우트 역시 일반 서비스 라우트와 동일하게 타임아웃과 장애 상황을 고려합니다.
- 서비스 ID, 문서 경로 및 보안 공개 범위가 확정되기 전에는 예시 설정을 그대로 운영에 사용하지 않습니다.

## 권장 적용 순서

1. 하위 서비스와 Eureka 서비스 ID 확정
2. 실제 API Gateway 라우트 추가
3. 인증 서버와 JWT의 `issuer`, `audience`, Scope 또는 Role 규칙 확정
4. Spring Security Resource Server 적용
5. 각 하위 서비스에 OpenAPI 문서 적용
6. Gateway에 OpenAPI 문서 라우트와 Swagger UI 목록 구성
7. 개발·운영 프로필별 Swagger 공개 정책 분리
8. 인증 성공·실패, 권한, CORS, 라우팅 및 문서 조회 통합 테스트

## 참고 문서

- [Spring Cloud Gateway Server WebFlux](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux.html)
- [Spring Security WebFlux](https://docs.spring.io/spring-security/reference/reactive/index.html)
- [Spring Security Reactive OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html)
- [springdoc-openapi Spring Boot 4 문서](https://springdoc.org/v4/index.html)

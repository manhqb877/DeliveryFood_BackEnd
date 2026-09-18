# BỘ QUY TẮC CLEAN CODE — JAVA SPRING BOOT MICROSERVICE

> Tài liệu chuẩn hoá cách viết code backend Java (Spring Boot) cho microservice: cấu trúc package, đặt tên, SOLID, Controller/Service/Repository pattern, DTO/Entity mapping bằng ModelMapper, Converter component, exception handling và ví dụ đầy đủ luồng **Login**.

---

## 1. KIẾN TRÚC TỔNG QUAN (LAYERED ARCHITECTURE)

```
Client
  │
  ▼
Controller        → nhận request, validate, gọi Service, trả ApiResponse
  │
  ▼
Service (interface) + ServiceImpl  → business logic
  │
  ▼
Converter (component)              → xử lý mapping/logic convert phức tạp (tách khỏi Service)
  │
  ▼
Repository (interface, Spring Data JPA)  → truy vấn DB
  │
  ▼
Entity ⇄ DTO (ModelMapper)
```

**Nguyên tắc cốt lõi:**
- Controller **KHÔNG** chứa business logic, chỉ điều phối (orchestration).
- Service xử lý nghiệp vụ, nhưng nếu logic convert/map dữ liệu phức tạp (nhiều field, tính toán, merge nhiều nguồn) → tách ra `Converter` (đánh dấu `@Component`) để inject vào Service, giữ Service mỏng, dễ test, dễ đọc.
- Mọi Service và Repository custom đều có **interface + implementation riêng** (Dependency Inversion – SOLID).
- Repository interface kế thừa `JpaRepository`/`CrudRepository` thì **không cần** viết Impl (Spring Data tự sinh). Chỉ viết `XxxRepositoryCustom` + `XxxRepositoryCustomImpl` khi cần query phức tạp (Specification, QueryDSL, native query...).

---

## 2. CẤU TRÚC PACKAGE CHUẨN CHO 1 MICROSERVICE

```
com.company.authservice
 ├── config/                 # Cấu hình: ModelMapperConfig, SecurityConfig, SwaggerConfig...
 ├── constant/                # Hằng số, enum dùng chung
 ├── controller/              # REST Controller
 ├── dto/
 │    ├── request/            # Request DTO (LoginRequest, RegisterRequest...)
 │    └── response/           # Response DTO (LoginResponse, UserResponse...)
 ├── entity/                  # JPA Entity
 ├── repository/
 │    ├── AuthRepository.java
 │    └── custom/             # Repository custom (nếu có)
 ├── service/
 │    ├── AuthService.java            # interface
 │    └── impl/
 │         └── AuthServiceImpl.java
 ├── converter/                # Converter component (map/transform logic phức tạp)
 ├── exception/
 │    ├── GlobalExceptionHandler.java
 │    ├── BusinessException.java
 │    └── ErrorCode.java
 ├── common/                   # ApiResponse, PageResponse, BaseEntity...
 ├── util/                     # Helper thuần túy (static methods, stateless)
 ├── security/                 # JWT filter, provider...
 └── AuthServiceApplication.java
```

> Đặt tên package **số ít, chữ thường, không gạch dưới**: `controller`, `service`, `repository`... không viết `Controllers`, `Services`.

---

## 3. QUY TẮC ĐẶT TÊN (NAMING CONVENTION)

| Thành phần | Convention | Ví dụ |
|---|---|---|
| Package | lowercase, không dấu gạch | `com.company.authservice.service` |
| Class | PascalCase, danh từ | `UserService`, `LoginRequest` |
| Interface | PascalCase, **không** prefix `I` | `AuthService` (không `IAuthService`) |
| Implementation | Tên interface + `Impl` | `AuthServiceImpl` |
| Method | camelCase, động từ + bổ ngữ | `getUserById()`, `login()`, `existsByEmail()` |
| Biến | camelCase, có nghĩa, không viết tắt tùy tiện | `userList`, không `ul` |
| Hằng số | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPT` |
| DTO Request | `<Action><Domain>Request` | `LoginRequest`, `CreateUserRequest` |
| DTO Response | `<Action><Domain>Response` | `LoginResponse`, `UserDetailResponse` |
| Entity | Danh từ số ít, PascalCase | `User`, `Order` (không `Users`) |
| Repository | `<Entity>Repository` | `UserRepository` |
| Converter | `<Entity>Converter` | `UserConverter` |
| Exception | Kết thúc bằng `Exception` | `UserNotFoundException` |
| Test class | `<ClassName>Test` | `AuthServiceImplTest` |
| Boolean method/field | prefix `is`, `has`, `can` | `isActive`, `hasPermission()` |

**Quy tắc chung:**
- Tên phải **tự giải thích** (self-documenting) — đọc tên là hiểu chức năng, hạn chế viết comment giải thích "làm gì".
- 1 method chỉ làm **1 việc** (Single Responsibility ở cấp method).
- Không đặt tên method generic vô nghĩa như `handle()`, `process()`, `doWork()` — phải cụ thể: `processPaymentRefund()`.
- Tránh Magic Number/String → đưa vào `constant` hoặc `enum`.

---

## 4. ÁP DỤNG SOLID TRONG BACKEND JAVA

### S — Single Responsibility Principle
- Mỗi class chỉ có **1 lý do để thay đổi**.
- Ví dụ: `AuthServiceImpl` chỉ lo nghiệp vụ đăng nhập/đăng ký; việc build đối tượng `User` từ `RegisterRequest` giao cho `UserConverter`, việc sinh JWT giao cho `JwtProvider`.

### O — Open/Closed Principle
- Class nên **mở để mở rộng, đóng để sửa đổi**.
- Ví dụ: dùng `Strategy Pattern` cho nhiều phương thức thanh toán (`PaymentStrategy` interface + `MomoPaymentStrategy`, `VnPayPaymentStrategy`) thay vì if-else dài trong Service.

### L — Liskov Substitution Principle
- Class con/implementation phải thay thế được interface cha mà không phá vỡ hành vi.
- Impl của Service/Repository interface phải tuân đúng contract (không throw exception ngoài dự kiến, không đổi ý nghĩa return).

### I — Interface Segregation Principle
- Không ép 1 interface phải cài đặt quá nhiều method không liên quan.
- Tách `ReadUserService` / `WriteUserService` nếu domain đủ lớn thay vì 1 `UserService` khổng lồ.

### D — Dependency Inversion Principle
- Controller phụ thuộc vào **interface** `AuthService`, không phụ thuộc trực tiếp `AuthServiceImpl`.
- Inject qua constructor (dùng `@RequiredArgsConstructor` của Lombok), **không dùng `@Autowired` field injection**.

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthRepository authRepository;
    private final UserConverter userConverter;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    // ...
}
```

---

## 5. CLEAN CODE — CÁC RULE BẮT BUỘC

1. **Constructor Injection** thay vì Field Injection (`@Autowired` trên field) — dễ test, tránh circular dependency ẩn.
2. **DTO tách biệt hoàn toàn Entity** — không bao giờ trả Entity trực tiếp ra Controller (tránh lộ field nhạy cảm, tránh lazy-loading exception).
3. **Validation** dùng `jakarta.validation` (`@NotBlank`, `@Email`, `@Size`...) ngay trên DTO Request, Controller dùng `@Valid`.
4. **Không try-catch nuốt exception (swallow exception)** — luôn log hoặc throw lại exception có ý nghĩa.
5. **Xử lý exception tập trung** bằng `@RestControllerAdvice` (Global Exception Handler), Controller không try-catch thủ công.
6. **Không hard-code** chuỗi thông báo, mã lỗi → đưa vào `ErrorCode` enum hoặc file `messages.properties` (hỗ trợ i18n).
7. **Method ngắn gọn** (khuyến nghị < 30-40 dòng), nếu dài → tách hàm private rõ nghĩa.
8. **Tránh nested if/else sâu** (> 2-3 cấp) → dùng early return, Optional, Strategy pattern.
9. **Immutable DTO khi có thể** — dùng `@Builder`, hạn chế setter tùy tiện ở Response.
10. **Logging chuẩn** dùng SLF4J (`@Slf4j` Lombok), không dùng `System.out.println`.
11. **Không để logic nghiệp vụ trong Entity/DTO** — Entity chỉ chứa field + quan hệ + annotation JPA, tối đa thêm vài method tiện ích nhỏ (`equals`, business invariant đơn giản).
12. **Transaction rõ ràng**: `@Transactional` đặt ở Service (method write), không đặt ở Controller/Repository.
13. **Không N+1 query** — dùng `@EntityGraph`, `JOIN FETCH`, hoặc DTO projection khi cần.
14. **Mọi API public đều có** request/response DTO riêng, kể cả khi field giống Entity 100% — để dễ mở rộng về sau mà không breaking change.
15. **Unit test cho Service layer** (business logic), không phụ thuộc DB thật (mock Repository).

---

## 6. DTO ⇄ ENTITY VỚI MODELMAPPER

### 6.1. Cấu hình ModelMapper (dùng chung toàn service)

```java
package com.company.authservice.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT) // tránh map nhầm field trùng tên khác nghĩa
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true); // khi update, field null không ghi đè
        return modelMapper;
    }
}
```

### 6.2. Nguyên tắc dùng ModelMapper

- Mapping **1-1, đơn giản** (field tên/kiểu tương đồng) → gọi thẳng `modelMapper.map(source, Target.class)` trong Service, **không** cần tạo Converter riêng.
- Mapping **phức tạp** (tính toán thêm, gọi service khác để enrich data, gộp nhiều entity thành 1 DTO, format lại dữ liệu, xử lý điều kiện nghiệp vụ khi map) → **bắt buộc** đẩy qua `Converter` component để Service không phình to.

```java
// Mapping đơn giản — dùng trực tiếp trong Service, KHÔNG cần Converter
UserResponse response = modelMapper.map(user, UserResponse.class);
```

---

## 7. CONVERTER PATTERN (COMPONENT)

Dùng khi logic convert **có xử lý nghiệp vụ**, không đơn thuần copy field.

```java
package com.company.authservice.converter;

import com.company.authservice.dto.request.RegisterRequest;
import com.company.authservice.dto.response.UserResponse;
import com.company.authservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserConverter {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Convert RegisterRequest -> User Entity
     * Có xử lý nghiệp vụ (encode password, set default role, chuẩn hoá email)
     * nên KHÔNG dùng modelMapper.map() thuần túy.
     */
    public User toEntity(RegisterRequest request) {
        User user = modelMapper.map(request, User.class);
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    /**
     * Convert User Entity -> UserResponse, kèm logic enrich (ẩn thông tin nhạy cảm,
     * format lại fullName từ firstName/lastName).
     */
    public UserResponse toResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setFullName(user.getFirstName() + " " + user.getLastName());
        return response;
    }
}
```

**Inject Converter vào Service** để Service chỉ tập trung điều phối nghiệp vụ:

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final UserConverter userConverter;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .user(userConverter.toResponse(user))
                .build();
    }
}
```

---

## 8. SERVICE — INTERFACE + IMPL

```java
package com.company.authservice.service;

import com.company.authservice.dto.request.LoginRequest;
import com.company.authservice.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
```

```java
package com.company.authservice.service.impl;

import com.company.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    // (như ví dụ mục 7)
}
```

---

## 9. REPOSITORY — INTERFACE (+ CUSTOM IMPL KHI CẦN)

Trường hợp thường gặp (đủ dùng, không cần Impl):

```java
package com.company.authservice.repository;

import com.company.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Trường hợp cần query động phức tạp → tách Custom Repository:

```java
public interface UserRepositoryCustom {
    List<User> searchUsers(UserSearchCriteria criteria);
}
```

```java
@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<User> searchUsers(UserSearchCriteria criteria) {
        // dùng CriteriaBuilder / QueryDSL cho query động
        ...
    }
}
```

```java
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
}
```

---

## 10. CONTROLLER CHUẨN + APIRESPONSE

Giữ nguyên `ApiResponse` bạn đã có. Controller **chỉ điều phối**, không xử lý logic:

```java
package com.company.authservice.controller;

import com.company.authservice.common.ApiResponse;
import com.company.authservice.dto.request.LoginRequest;
import com.company.authservice.dto.response.LoginResponse;
import com.company.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(response)
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Register successfully")
                .data(response)
                .build());
    }
}
```

> **Gợi ý nâng cấp `ApiResponse` (tuỳ chọn, không bắt buộc):** thêm static factory method để Controller gọn hơn:
> ```java
> public static ApiResponse success(String message, Object data) {
>     return ApiResponse.builder().status(200).message(message).data(data).build();
> }
> ```
> Khi đó Controller chỉ cần: `return ResponseEntity.ok(ApiResponse.success("Login successfully", response));`

---

## 11. XỬ LÝ EXCEPTION TẬP TRUNG (GLOBAL EXCEPTION HANDLER)

```java
package com.company.authservice.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(1001, "User not found"),
    INVALID_CREDENTIALS(1002, "Email or password is incorrect"),
    EMAIL_ALREADY_EXISTS(1003, "Email already exists"),
    VALIDATION_ERROR(1004, "Invalid request payload"),
    INTERNAL_SERVER_ERROR(9999, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

```java
package com.company.authservice.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

```java
package com.company.authservice.exception;

import com.company.authservice.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                .status(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                .status(ErrorCode.VALIDATION_ERROR.getCode())
                .message(message)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnknownException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .build());
    }
}
```

---

## 12. DTO REQUEST/RESPONSE MẪU

```java
package com.company.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password must not be blank")
    private String password;
}
```

```java
package com.company.authservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
```

```java
@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
}
```

---

## 13. LOGGING

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        ...
        log.info("Login success for userId: {}", user.getId());
        return response;
    }
}
```

- `log.info` cho luồng nghiệp vụ chính (bắt đầu/kết thúc).
- `log.warn` cho lỗi nghiệp vụ dự đoán được (sai mật khẩu, không tìm thấy user).
- `log.error` kèm exception cho lỗi hệ thống không mong muốn.
- **Không log password, token, thông tin nhạy cảm.**

---

## 14. QUY TẮC RIÊNG CHO MICROSERVICE

1. Mỗi service **1 database** (Database per Service) — không share DB giữa các service.
2. Giao tiếp giữa service: REST/Feign Client (đồng bộ) hoặc Kafka/RabbitMQ (bất đồng bộ) — DTO giao tiếp giữa service (`XxxClientResponse`) **tách biệt** với DTO nội bộ.
3. Đặt `traceId`/`correlationId` vào log (MDC) để trace request xuyên service.
4. Config tập trung qua Config Server / biến môi trường, không hard-code URL service khác.
5. Mỗi service expose `/actuator/health` để phục vụ health check, service discovery.
6. Version API: `/api/v1/...` để dễ thay đổi không phá vỡ client cũ.
7. Idempotency cho các API quan trọng (payment, order) — tránh xử lý trùng khi retry.

---

## 15. CHECKLIST TRƯỚC KHI MERGE CODE (CODE REVIEW)

- [ ] Controller không chứa business logic
- [ ] Service có interface + Impl, inject qua constructor
- [ ] Mapping đơn giản dùng ModelMapper trực tiếp; mapping có logic dùng Converter (`@Component`)
- [ ] Không trả Entity ra ngoài API, chỉ trả DTO
- [ ] Validate input bằng `@Valid` + annotation trên DTO
- [ ] Exception được xử lý qua GlobalExceptionHandler, không try-catch rải rác
- [ ] Không hard-code string/số lỗi, dùng `ErrorCode`/constant
- [ ] Có log ở các bước nghiệp vụ quan trọng, không log dữ liệu nhạy cảm
- [ ] `@Transactional` đặt đúng ở Service, method ghi dữ liệu
- [ ] Đặt tên rõ ràng, method ngắn gọn, không nested if sâu
- [ ] Có unit test cho Service (mock Repository/Converter)
- [ ] Không N+1 query, kiểm tra lại query khi có quan hệ Entity

---

## 16. THƯ VIỆN ĐỀ XUẤT

| Mục đích | Thư viện |
|---|---|
| Mapping DTO ⇄ Entity | ModelMapper hoặc MapStruct (compile-time, nhanh hơn nếu project lớn) |
| Validation | jakarta.validation (Hibernate Validator) |
| Boilerplate reduction | Lombok |
| Security/JWT | Spring Security + jjwt |
| API doc | springdoc-openapi (Swagger UI) |
| Logging | SLF4J + Logback, ELK/Loki cho tập trung log |
| Test | JUnit 5 + Mockito + Testcontainers (integration test với DB thật) |

> Ghi chú: Nếu project scale lớn và cần performance cao hơn, có thể cân nhắc chuyển từ ModelMapper (reflection runtime) sang **MapStruct** (sinh code lúc compile) — giữ nguyên pattern Converter/Interface-Impl ở trên, chỉ đổi cách implement bên trong.

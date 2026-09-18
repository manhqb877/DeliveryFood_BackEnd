---
name: spring-boot-engineer
description: Dùng khi cần pattern cụ thể cho REST API, dependency injection, JPA persistence, cấu hình Spring Security, và best practice testing trong Spring Boot. Phù hợp cho các task cụ thể, chuyên sâu hơn là kiến trúc tổng thể. Kích hoạt khi user nói "làm JWT auth", "cấu hình Spring Security", "viết JPA repository", "test controller". Cảm hứng từ jeffallan/claude-skills (spring-boot-engineer).
---

# Spring Boot Engineer

## Phạm vi
Skill này tập trung vào **kỹ thuật thực thi cụ thể** trong Spring Boot, dùng kèm với skill kiến trúc tổng thể (`java-spring-boot-structure`). Dùng progressive disclosure: phần dưới là core, chi tiết sâu hơn (ví dụ full flow OAuth2) nên tách vào `references/` riêng nếu skill phình to.

## REST API

- Dùng `@RestController` + `@RequestMapping` theo resource (danh từ số nhiều: `/orders`, không phải `/getOrders`).
- Validate input bằng Bean Validation (`@Valid`, `@NotNull`, `@Size`...) ngay tại DTO, không validate thủ công trong service.
- Trả lỗi qua `@ControllerAdvice` + `@ExceptionHandler` tập trung, format lỗi nhất quán (ví dụ theo chuẩn `application/problem+json` - RFC 7807).
- Versioning API rõ ràng (`/api/v1/...`) nếu hệ thống cần tương thích ngược.

## Dependency Injection

- Luôn dùng **constructor injection**, không dùng `@Autowired` trên field (khó test, che giấu dependency).
- Với nhiều implementation của cùng interface, dùng `@Qualifier` hoặc tách theo `@Primary` có chủ đích, tránh Spring tự đoán nhầm bean.

## JPA Persistence

- Entity chỉ chứa mapping dữ liệu, không chứa business logic phức tạp.
- Dùng `@Transactional` ở tầng service, xác định rõ ranh giới transaction (không để transaction kéo dài qua nhiều service không liên quan).
- Tránh N+1 query: dùng `@EntityGraph` hoặc `JOIN FETCH` khi cần load quan hệ.
- Migration schema qua Flyway/Liquibase, không dùng `ddl-auto: update` ở production.

## Spring Security

- Cấu hình `SecurityFilterChain` (không dùng `WebSecurityConfigurerAdapter` đã deprecated).
- JWT: xác thực qua filter riêng (`OncePerRequestFilter`), lưu secret/key trong biến môi trường hoặc Vault, không hardcode.
- Phân quyền theo role/scope rõ ràng bằng `@PreAuthorize` ở tầng service, không chỉ chặn ở controller.
- CORS cấu hình tường minh theo domain cho phép, không dùng `*` ở production.

## Testing

- Unit test service: mock repository/dependency, test business logic thuần tuý.
- `@WebMvcTest` cho controller: test luồng HTTP, validate, exception handling — mock service layer.
- `@DataJpaTest` hoặc Testcontainers cho repository: test query thật với DB (Postgres container), không dùng H2 nếu production dùng Postgres (tránh khác biệt hành vi SQL).
- Test security: dùng `@WithMockUser` hoặc test thực với token JWT giả lập.

## Ví dụ áp dụng
- "Thêm JWT authentication cho API đặt hàng" → tạo filter xác thực JWT, cấu hình `SecurityFilterChain`, test bằng `@WithMockUser` + test riêng cho case token hết hạn.
- "Repository trả về N+1 query, tối ưu giúp tôi" → thêm `@EntityGraph` hoặc chuyển sang `JOIN FETCH`.
- "Viết test cho OrderController" → dùng `@WebMvcTest(OrderController.class)`, mock `OrderService`.

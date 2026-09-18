---
name: java-spring-boot-structure
description: Dùng khi tạo project Spring Boot mới, thêm domain/feature mới, hoặc khi code sinh ra sai vị trí package, mapping tay thay vì MapStruct, business logic lẫn trong controller. Kích hoạt khi user nhắc "cấu trúc project", "package layout", "domain mới", "microservice Spring Boot". Cảm hứng từ israf1l/java-spring-boot-structure.
---

# Java Spring Boot Structure

## Vấn đề skill này giải quyết
Nếu không có rule rõ ràng, AI hoặc dev mới thường sinh code Spring Boot "chạy được" nhưng vi phạm kiến trúc mà senior engineer kỳ vọng: sai vị trí package, mapping Entity/DTO thủ công, business logic nằm trong controller, code liên quan AI/LLM bị rải rác không có "nhà" rõ ràng.

## Cấu trúc package chuẩn (domain/feature-first)

```
src/main/java/com/company/project/
  └── <domain>/                  # ví dụ: order, document, chat
        ├── controller/          # REST endpoint — CHỈ điều phối, không chứa business logic
        ├── service/
        │     ├── abstraction/   # interface, ví dụ OrderService
        │     └── concrete/      # implementation, hậu tố *Handler
        │             └── OrderServiceHandler.java
        ├── repository/          # Spring Data JPA / MyBatis repository
        ├── dto/                 # request/response DTO — ranh giới giao tiếp API
        ├── entity/              # JPA entity — KHÔNG lộ ra ngoài service layer
        ├── mapper/              # MapStruct mapper — bắt buộc, cấm map tay
        └── ai/                  # nếu domain có tích hợp Spring AI: advisor, client, prompt, tool
  └── shared/                    # CHỈ code cross-cutting thật sự: exception handler, common util
```

## Rule bắt buộc (enforce, không phải gợi ý)

1. **Không import chéo domain.** `order` không được import trực tiếp class trong `document`; nếu cần chia sẻ dữ liệu, dùng event hoặc API nội bộ.
2. **MapStruct-only mapping.** Không viết `new OrderDto(entity.getX(), entity.getY()...)` thủ công trong service — luôn có `@Mapper` interface riêng trong `mapper/`.
3. **DTO/Entity tách biệt nghiêm ngặt.** Controller và bên ngoài service layer không bao giờ thấy Entity.
4. **Service tách interface/implementation.** Interface đặt ở `service/abstraction`, implementation ở `service/concrete` với hậu tố `Handler` (hoặc `Impl` nếu team đã quen dùng, nhưng phải nhất quán toàn dự án).
5. **Strategy pattern** khi có nhiều biến thể xử lý cùng interface: đặt dưới `service/strategy/`.
6. **Spring AI code đặt trong `ai/` của từng domain**, không gom hết vào một package `ai` chung ở root — tránh "god package".
7. **File cấu hình AI agent** (`CLAUDE.md`, `.claude/agents/`, `skills/`) đặt ở **root project**, tuyệt đối không đặt trong `src/`.
8. **shared/** chỉ chứa code thực sự dùng chung nhiều domain — nếu chỉ 1-2 domain dùng, để nguyên trong domain đó.

## Quy trình khi thêm domain mới

1. Tạo thư mục domain theo đúng layout ở trên (kể cả khi ban đầu chưa cần hết các package con).
2. Định nghĩa DTO trước (request/response contract), sau đó mới viết Entity.
3. Viết interface Service trước, code lại theo interface đó (test-first nếu có thể).
4. Sinh Mapper bằng MapStruct annotation, không tự map tay dù chỉ 1-2 field.
5. Nếu domain có gọi LLM/RAG, tạo `ai/` package ngay từ đầu, không để lẫn trong `service/`.

## Ví dụ áp dụng
- "Tạo domain quản lý tài liệu (document) có tích hợp AI tóm tắt" → tạo `document/` với đủ `controller, service, repository, dto, entity, mapper, ai/`.
- "Sao code này map Entity sang DTO bằng tay vậy, sửa lại" → thay bằng MapStruct `@Mapper(componentModel = "spring")`.
- "Vì sao AI sinh code cho tôi để CLAUDE.md trong src/main/java?" → di chuyển về root project.

---
name: spring-boot-scaffold
description: Dùng khi cần khởi tạo (scaffold) một service Spring Boot mới từ đầu, hoặc mở rộng service hiện có với cấu hình production-ready (profile, health check, metrics, packaging). Kích hoạt khi user nói "tạo project Spring Boot mới", "setup service", "cấu hình production". Cảm hứng từ bobmatnyc/claude-mpm-skills (spring-boot).
---

# Spring Boot Scaffold

## Mục đích
Scaffold và mở rộng service Spring Boot REST với cấu trúc project đúng chuẩn, dependency injection, persistence, validation, và cấu hình production-ready ngay từ đầu — tránh việc phải "vá" lại cấu hình production sau này.

## Checklist khởi tạo project mới

1. **Build tool**: Maven hoặc Gradle (theo chuẩn team; nếu không rõ, ưu tiên Maven cho team lớn, Gradle nếu cần build linh hoạt/đa module).
2. **Cấu trúc module** (nếu là hệ thống lớn, cân nhắc multi-module: `api`, `core`, `infra`).
3. **Layout package**: theo skill `java-spring-boot-structure` (domain-first).
4. **REST controller + DTO validation**: có sẵn `@ControllerAdvice` xử lý lỗi từ ngày đầu.
5. **JPA/Data access**: cấu hình datasource theo profile (`dev`, `staging`, `prod`), transaction pattern rõ ràng.
6. **Spring Security**: bật baseline (authentication + CSRF phù hợp loại app: API stateless thì tắt CSRF, bật cho web session-based).
7. **Config theo profile**: `application-dev.yml`, `application-prod.yml`, không để secret trong file commit vào git.
8. **Observability hook**: bật Actuator (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).
9. **Testing & packaging**: cấu hình sẵn Surefire/Failsafe (Maven) hoặc test task (Gradle), build ra JAR chạy được kèm layer riêng cho Docker (tối ưu cache layer khi build image).

## Cấu hình production-ready cần có ngay từ đầu

| Hạng mục | Cấu hình tối thiểu |
|---|---|
| Health check | Bật `management.endpoint.health.show-details=when-authorized`, expose `/actuator/health/liveness` và `/readiness` cho k8s probe |
| Metrics | Bật Micrometer + Prometheus registry |
| Logging | JSON structured log (logback-spring.xml), có correlation/trace id |
| Graceful shutdown | `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase` hợp lý |
| Connection pool | Cấu hình HikariCP rõ `maximum-pool-size` theo tải thực tế, không để mặc định nếu traffic cao |
| Resource limit | Đặt JVM heap (`-Xmx`) phù hợp container memory limit (k8s) |
| Config externalization | Dùng biến môi trường / Config Server / Vault cho secret, không hardcode |

## Quy trình mở rộng service hiện có

1. Kiểm tra service đã bật Actuator health/metrics chưa — nếu chưa, thêm ngay trước khi thêm feature mới.
2. Kiểm tra profile cấu hình đã tách `dev/staging/prod` chưa.
3. Đảm bảo mọi endpoint mới có validation input và error handling nhất quán với phần còn lại.
4. Đảm bảo Dockerfile dùng multi-stage build, tách layer dependency và layer code để tận dụng cache.

## Ví dụ áp dụng
- "Tạo mới service Spring Boot cho quản lý inventory" → scaffold đầy đủ theo checklist trên, bật Actuator, cấu hình 3 profile.
- "Service hiện tại thiếu health check cho Kubernetes" → thêm Actuator liveness/readiness probe.
- "JAR build ra bị nặng, build Docker chậm" → chuyển sang multi-stage Dockerfile, tách layer dependency.

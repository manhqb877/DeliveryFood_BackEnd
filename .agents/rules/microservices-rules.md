# Architecture & Implementation Rules for Backend Services

- **Event-Driven Architecture (Kafka):** Bất kỳ service nào có event/action cần báo cho các service khác biết thì PHẢI publish message vào Kafka topic tương ứng để các service khác có thể subscribe.
- **Inter-service Communication:** Khi một service cần lấy thông tin trực tiếp từ service khác, PHẢI sử dụng **OpenFeign**.
- **Caching & Performance (Redis):** 
  - Sử dụng **Redis** để lưu trữ (cache) các dữ liệu cần truy xuất nhanh, đặc biệt là sản phẩm (Product) để hệ thống load nhanh hơn.
  - Những dữ liệu đọc nhiều / thường xuyên đều cần đưa vào Redis.

**Databases:** Neon (PostgreSQL) và MongoDB Atlas.

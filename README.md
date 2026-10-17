# 🍔 Food Delivery - Microservices Backend System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.x-blue.svg)](https://spring.io/projects/spring-cloud)
[![Python](https://img.shields.io/badge/Python-3.10+-yellow.svg)](https://www.python.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon%20Serverless-336791.svg)](https://neon.tech/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248.svg)](https://www.mongodb.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Distributed-231F20.svg)](https://kafka.apache.org/)

Hệ thống Backend Microservices cho nền tảng giao đồ ăn đa dịch vụ (**Food Delivery Platform**), hỗ trợ đơn hàng đơn quán và đơn ghép đa quán (**Multi-Shop Orders**), đối soát thanh toán tự động qua VietQR (SePay), định vị tài xế thời gian thực (Vietmap API) và trợ lý thông minh gợi ý món ăn qua AI Agent (Google Gemini / Anthropic Claude).

---

## 🏛️ Kiến Trúc Hệ Thống (Architecture Overview)

```
                                  +-----------------------+
                                  |   Client Applications  |
                                  | (Customer / Shipper / |
                                  |    Admin Dashboard)   |
                                  +-----------+-----------+
                                              |
                                              v (HTTPS / WSS)
                                   +---------------------+
                                   |  API Gateway (8080) |
                                   +----------+----------+
                                              |
          +-----------------------------------+-----------------------------------+
          |                 |                 |                 |                 |
          v                 v                 v                 v                 v
   +--------------+  +--------------+  +--------------+  +--------------+  +--------------+
   | auth-service |  | core-service |  |order-service |  |tracking-serv |  |payment-serv  |
   |    (8081)    |  |    (8082)    |  |    (8083)    |  |    (8084)    |  |    (8085)    |
   +--------------+  +--------------+  +--------------+  +--------------+  +--------------+
          |                 |                 |                 |                 |
          +-----------------+-----------------+-----------------+-----------------+
                                              |
          +-----------------------------------+-----------------------------------+
          |                                   |                                   |
          v                                   v                                   v
   +----------------------+        +----------------------+        +----------------------+
   | notification-service |        |  analytics-service   |        |   ai-agent-service   |
   |        (8086)        |        |        (8087)        |        |        (8088)        |
   +----------------------+        +----------------------+        +----------------------+

   [ Infrastructure: Eureka Discovery (8761) | Redis Cache (6381) | Kafka Broker (9093) ]
```

---

## 🧩 Danh Sách Các Microservices & Cổng Dịch Vụ

| Dịch vụ | Cổng (Port) | Công nghệ | Cơ sở dữ liệu | Vai trò chính |
| :--- | :---: | :---: | :---: | :--- |
| **`eureka-server`** | `8761` | Spring Cloud Netflix Eureka | Memory | Quản lý đăng ký và phát hiện dịch vụ (Service Registry & Discovery). |
| **`api-gateway`** | `8080` | Spring Cloud Gateway, Redis | Redis | Định tuyến API tập trung, Rate Limiting, xác thực JWT & bảo mật CORS. |
| **`auth-service`** | `8081` | Spring Boot 3, Spring Security | PostgreSQL (`auth_db`) | Quản lý tài khoản Người dùng, Chủ quán, Shipper, Admin; JWT token, gửi mã OTP mail. |
| **`core-service`** | `8082` | Spring Boot 3, JPA | PostgreSQL (`core_db`) | Quản lý thông tin nhà hàng, danh mục món ăn, giá tiền, toppings/options. |
| **`order-service`** | `8083` | Spring Boot 3, JPA, Mongo | PostgreSQL (`order_db`) & MongoDB | Giỏ hàng, đặt đơn lẻ/ghép 2 quán, tính hoa hồng hệ thống, hoàn trả COD, đánh giá món. |
| **`tracking-service`** | `8084` | Spring Boot 3, WebSocket | PostgreSQL (`tracking_db`) & MongoDB | Điều phối đơn cho tài xế, cập nhật GPS thời gian thực, tính lộ trình Vietmap. |
| **`payment-service`** | `8085` | Spring Boot 3, JPA | PostgreSQL (`payment_db`) | Ví tiền Shipper/Chủ quán, nạp/rút, đối soát tự động thanh toán VietQR (SePay Webhook). |
| **`notification-service`** | `8086` | Spring Boot 3, Kafka | MongoDB (`notification_db`) | Tiêu thụ sự kiện từ Kafka để đẩy thông báo trạng thái đơn, khuyến mãi tới người dùng. |
| **`analytics-service`** | `8087` | Spring Boot 3, Mongo | MongoDB (`analytics_db`) | Thống kê doanh thu, tỷ lệ giao thành công, phát hiện cảnh báo gian lận (Fraud Alerts). |
| **`ai-agent-service`** | `8088` | Python 3.10, FastAPI | Redis Session Memory | Chatbot AI tư vấn món ăn thông minh theo sở thích và ngân sách khách hàng. |

---

## 🛠️ Yêu Cầu Môi Trường (Prerequisites)

- **Java JDK**: 17 hoặc 21 LTS
- **Apache Maven**: 3.8+
- **Python**: 3.10+ (cho AI Agent)
- **Docker & Docker Compose**: Khuyến nghị dùng Docker Desktop
- **Node.js**: 18+ (nếu chạy công cụ phụ trợ hoặc ngrok)

---

## ⚙️ Cấu Hình Môi Trường (`.env`)

Tạo file `.env` tại thư mục gốc của repository `DeliveryFood_BackEnd` với nội dung mẫu:

```env
# =====================================================================
# CƠ SỞ DỮ LIỆU POSTGRESQL (Neon Serverless)
# =====================================================================
DB_URL=jdbc:postgresql://<neon-hostname>/auth_db?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=<neon-password>

# =====================================================================
# CẤU HÌNH GỬI EMAIL (Gmail App Password)
# =====================================================================
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# =====================================================================
# BẢO MẬT JWT
# =====================================================================
JWT_SECRET=5K9mRzT8sW3vY1nJ7pQ4xB6cL2hF0eA9dG8uN5oX3yV7wZ1iM4kP6

# =====================================================================
# BẢN ĐỒ & ĐỊNH VỊ (Vietmap API)
# =====================================================================
VIETMAP_API_KEY=your-vietmap-api-key

# =====================================================================
# CỔNG THANH TOÁN TỰ ĐỘNG VIETQR (SePay)
# =====================================================================
SEPAY_BANK_NAME=MBBank
SEPAY_ACCOUNT_NUMBER=025452790502
SEPAY_ACCOUNT_HOLDER=NGUYEN THAI AN
SEPAY_API_TOKEN=your-sepay-api-token

# =====================================================================
# AI AGENT SERVICE (Gemini / Anthropic)
# =====================================================================
GEMINI_API_KEY=your-gemini-api-key
ANTHROPIC_API_KEY=your-anthropic-api-key
LLM_PROVIDER=gemini
```

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)

1. **Khởi động tất cả dịch vụ:**
   ```bash
   cd DeliveryFood_BackEnd
   docker compose up -d --build
   ```

2. **Kiểm tra trạng thái các container:**
   ```bash
   docker compose ps
   ```

3. **Xem log các dịch vụ:**
   ```bash
   # Xem toàn bộ
   docker compose logs -f

   # Hoặc xem riêng từng service cụ thể:
   docker compose logs -f api-gateway
   docker compose logs -f order-service
   docker compose logs -f tracking-service
   ```

4. **Dừng hệ thống:**
   ```bash
   docker compose down
   ```

---

### Cách 2: Chạy cục bộ từng Service khi lập trình (Development Mode)

Nếu muốn debug hoặc lập trình một service riêng lẻ:
1. Chạy cơ sở hạ tầng trước (Redis, Kafka, Zookeeper, Eureka):
   ```bash
   docker compose up -d delivery-zookeeper delivery-kafka delivery-redis eureka-server
   ```
2. Vào thư mục service cần chạy, ví dụ `order-service`:
   ```bash
   cd order-service
   mvn spring-boot:run
   ```

---

## 🌐 Mở Cổng Công Khai Cho Mobile & Webhook (Ngrok Tunnel)

Để ứng dụng Mobile (`AppCustomer`, `AppShipper`) kết nối được từ điện thoại thật hoặc nhận Webhook từ SePay:
```bash
npx ngrok http 8080 --url=https://<your-custom-domain>.ngrok-free.dev
```

Sau đó cấu hình URL này vào file `.env` của `DeliveryFood_Mobile` và `apps/web`.

---

## 🧪 Tài Khoản & Bảng Dữ Liệu Kiểm Thử Mẫu

- **Eureka Dashboard:** `http://localhost:8761`
- **API Gateway Entry:** `http://localhost:8080`
- **Swagger / OpenAPI Documentation:**
  - `http://localhost:8081/swagger-ui/index.html` (Auth)
  - `http://localhost:8082/swagger-ui/index.html` (Core)
  - `http://localhost:8083/swagger-ui/index.html` (Order)
  - `http://localhost:8084/swagger-ui/index.html` (Tracking)
  - `http://localhost:8085/swagger-ui/index.html` (Payment)

---

## 🛡️ Giấy Phép & Bản Quyền
Dự án được xây dựng phục vụ Khóa luận tốt nghiệp chuyên ngành Kỹ thuật Phần mềm / Công nghệ Thông tin. Mọi quyền được bảo lưu.

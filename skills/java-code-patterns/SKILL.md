---
name: java-code-patterns
description: Dùng khi viết mới, review hoặc refactor code Java cần tuân thủ nguyên lý S.O.L.I.D và các design pattern chuẩn (Factory, Builder, Strategy, Observer, Decorator...). Kích hoạt khi user nói "refactor theo SOLID", "áp design pattern", "review clean code", "commit code này giúp tôi". Cảm hứng từ decebals/claude-code-java.
---

# Java Code Patterns

## Khi nào dùng skill này
- User yêu cầu viết code Java mới cần đảm bảo chuẩn kiến trúc/clean code.
- User yêu cầu review code và chỉ ra vi phạm nguyên lý thiết kế.
- User yêu cầu refactor một class/method đang "phình to", nhiều trách nhiệm.
- User gõ lệnh dạng slash: `/git-commit`, `/solid-principles` (nếu môi trường hỗ trợ).

## Nguyên lý S.O.L.I.D — checklist áp dụng

| Nguyên lý | Dấu hiệu vi phạm cần tìm | Cách sửa |
|---|---|---|
| **S**ingle Responsibility | Class có > 1 lý do để thay đổi (vừa xử lý business, vừa format output, vừa gọi DB) | Tách thành nhiều class nhỏ, mỗi class 1 trách nhiệm |
| **O**pen/Closed | Thêm tính năng mới phải sửa code cũ (if/else hoặc switch dài dằng dặc) | Dùng Strategy/Polymorphism thay vì if-else chuỗi |
| **L**iskov Substitution | Subclass override method nhưng làm khác hành vi gốc, ném exception không mong đợi | Đảm bảo subclass thay thế được superclass mà không phá vỡ hợp đồng (contract) |
| **I**nterface Segregation | Interface quá "béo", class phải implement method không dùng tới | Tách interface nhỏ theo nhóm chức năng (role interface) |
| **D**ependency Inversion | Service phụ thuộc trực tiếp vào class cụ thể (`new RepositoryImpl()`) thay vì interface | Dùng constructor injection với interface, để Spring quản lý bean |

## Design Pattern thường dùng trong Java backend

- **Factory** — khi cần khởi tạo object theo điều kiện runtime (ví dụ chọn `PaymentProcessor` theo loại thanh toán).
- **Builder** — khi object có nhiều field optional (DTO phức tạp, request config LLM).
- **Strategy** — khi có nhiều thuật toán/luồng xử lý thay thế nhau (ví dụ nhiều chiến lược chunking cho RAG).
- **Observer** — khi cần thông báo nhiều consumer về một sự kiện (ví dụ sau khi ingest tài liệu xong, bắn event cho nhiều listener).
- **Decorator** — khi cần thêm hành vi (logging, cache, retry) quanh một service mà không sửa class gốc.

## Quy trình review/refactor đề xuất

1. Đọc toàn bộ class/method liên quan, liệt kê trách nhiệm hiện có.
2. Đối chiếu với bảng S.O.L.I.D ở trên, đánh dấu vi phạm.
3. Đề xuất pattern phù hợp (không áp pattern chỉ vì "cho sang" — chỉ dùng khi thực sự giải quyết vấn đề).
4. Viết lại code, giữ nguyên hành vi (behavior-preserving refactor), có test đi kèm trước khi refactor lớn.
5. Tổng hợp thay đổi thành commit message rõ ràng.

## Chuẩn commit message (dùng khi user nhờ "commit giúp tôi")

```
<type>(<scope>): <mô tả ngắn gọn, thì hiện tại>

<giải thích chi tiết nếu cần: vì sao thay đổi, ảnh hưởng gì>

<footer: BREAKING CHANGE / closes #issue nếu có>
```
Các `type` phổ biến: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`.

## Ví dụ áp dụng
- "Class `OrderService` của tôi dài 500 dòng, giúp tách nhỏ" → áp dụng SRP, tách `OrderValidator`, `OrderPricingService`, `OrderNotifier`.
- "Thêm loại thanh toán mới mà không sửa code cũ" → áp dụng Strategy + Open/Closed.
- "Review PR này trước khi merge" → chạy checklist SOLID + đề xuất pattern nếu cần.

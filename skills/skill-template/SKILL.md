---
name: skill-template
description: Meta-skill dùng khi cần tạo một Skill mới cho hệ thống (viết SKILL.md đúng chuẩn, đặt tên, viết description để agent nhận diện tốt). Kích hoạt khi user nói "tạo skill mới", "viết SKILL.md cho...", "thêm skill vào hệ thống". Cảm hứng từ anthropics/skills (template-skill).
---

# Skill Template — Chuẩn tạo Skill mới

## Cấu trúc thư mục 1 skill

```
skill-name/
├── SKILL.md          # BẮT BUỘC — instructions + metadata
├── scripts/           # optional — script hỗ trợ (python/bash)
├── references/        # optional — tài liệu tham khảo chi tiết, chỉ load khi cần
└── resources/          # optional — template, file mẫu, asset
```

## Khuôn mẫu SKILL.md chuẩn

```markdown
---
name: my-skill-name
description: Mô tả rõ ràng skill làm gì VÀ khi nào nên kích hoạt. Đây là phần agent đọc đầu tiên (chỉ ~100 token) để quyết định có load skill hay không — viết cụ thể, tránh chung chung.
---

# My Skill Name

[Nội dung instructions mà Claude/agent sẽ tuân theo khi skill được kích hoạt]

## Examples
- Ví dụ tình huống 1
- Ví dụ tình huống 2

## Guidelines
- Rule 1
- Rule 2
```

## Nguyên tắc viết `description` tốt (quan trọng nhất)

Vì cơ chế **progressive disclosure**: lúc khởi động, agent chỉ thấy `name` + `description` của mọi skill (rất ít token). Agent quyết định có load toàn bộ SKILL.md hay không dựa hoàn toàn vào dòng `description` này. Vì vậy:

1. **Nêu rõ "dùng khi nào"** — liệt kê cụm từ/tình huống cụ thể sẽ kích hoạt skill (ví dụ: "khi user nói...", "khi thấy lỗi...").
2. **Tránh mô tả chung chung** kiểu "skill hỗ trợ Java" — quá rộng, dễ bị bỏ qua hoặc kích hoạt sai lúc.
3. **Không trùng phạm vi** với skill khác trong cùng hệ thống — nếu 2 skill có description giống nhau, agent sẽ khó chọn đúng, nên tách rõ ranh giới hoặc gộp lại.
4. **Ngắn gọn** — description chỉ nên 1–3 câu, phần chi tiết để trong thân SKILL.md.

## Nguyên tắc tổ chức nội dung thân skill

- Phần đầu: **Khi nào dùng** (trigger cụ thể).
- Phần giữa: **Rule/checklist** — nên dùng bảng hoặc danh sách có thứ tự để agent dễ trích xuất, tránh viết văn xuôi dài.
- Phần cuối: **Ví dụ áp dụng** — 2–3 ví dụ thực tế giúp agent generalize đúng cách.
- Nếu nội dung > 5.000 token: tách phần chi tiết/ít dùng ra `references/`, chỉ giữ phần core trong SKILL.md chính (đúng tinh thần progressive disclosure — không nạp thứ chưa cần).

## Quy trình tạo skill mới cho hệ thống

1. Xác định rõ: skill này khác gì với các skill đã có? (tránh chồng lấn)
2. Viết `description` trước tiên, tự hỏi: "Nếu chỉ đọc dòng này, agent có biết khi nào nên dùng không?"
3. Viết phần "Khi nào dùng" + rule cốt lõi.
4. Thêm 2–3 ví dụ cụ thể.
5. Nếu cần script hỗ trợ (ví dụ script kiểm tra convention tự động), đặt trong `scripts/` và tham chiếu từ SKILL.md.
6. Đặt skill vào đúng vị trí: `skills/<skill-name>/SKILL.md` ở root project (không nằm trong `src/`).
7. Test thử: mô tả 1 task thực tế cho agent, xem có kích hoạt đúng skill mong muốn không — nếu không, chỉnh lại `description`.

## Ví dụ áp dụng
- "Tôi muốn có skill riêng cho việc viết Dockerfile chuẩn của team" → tạo `skills/dockerfile-standards/SKILL.md`, description nêu rõ "khi user nhắc Dockerfile, containerize, build image".
- "Hai skill của tôi bị trùng, agent chọn sai" → so sánh description, thu hẹp phạm vi từng skill hoặc gộp thành 1.

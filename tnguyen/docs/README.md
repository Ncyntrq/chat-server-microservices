# 📂 tnguyen — Tài Liệu & Test

Thư mục cá nhân chứa tài liệu phân tích và file test cho **Chat Server Microservices**.

## Cấu trúc

```
tnguyen/
├── docs/                         ← Tài liệu phân tích, thiết kế, hướng dẫn
│   ├── 01_yeu_cau_phan_mem.md
│   ├── 06_phan_tich_task_ha_tang.md
│   ├── 07_thiet_ke_devops.md
│   ├── 08_plan_notification_file_service.md
│   ├── 09_huong_dan_test.md
│   ├── SETUP_vi.md
│   └── RUNNING.md
│
└── test/                         ← File .http chạy trực tiếp trong IntelliJ
    ├── test-system.http          ← Auth + Presence + Log (chạy trước)
    ├── test-notification.http    ← Notification Service (port 8088)
    └── test-file.http            ← File Service (port 8089)
```

## Tài liệu

| File | Mô tả |
|------|-------|
| `docs/01_yeu_cau_phan_mem.md` | Yêu cầu phần mềm — 11 services, ~77 chức năng |
| `docs/06_phan_tich_task_ha_tang.md` | Phân tích task "Làm mới & Nâng cấp hạ tầng" |
| `docs/07_thiet_ke_devops.md` | Thiết kế DevOps — CI/CD, Docker, Monitoring |
| `docs/08_plan_notification_file_service.md` | **Plan triển khai** — Notification + File Service |
| `docs/09_huong_dan_test.md` | **Hướng dẫn test A→Z** bằng IntelliJ IDEA |

## Test Files (IntelliJ HTTP Client)

| File | Mô tả | Chạy khi |
|------|-------|----------|
| `test/test-system.http` | Auth register/login, Presence, Log history | Đầu tiên — verify hệ thống OK |
| `test/test-notification.http` | Notification CRUD, unread count, ack | Sau khi tạo notification-service |
| `test/test-file.http` | Upload/download file, thumbnail, delete | Sau khi tạo file-service |

> **Cách dùng:** Mở file `.http` trong IntelliJ → click ▶ bên cạnh từng request.

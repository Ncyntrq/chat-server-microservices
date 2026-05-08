# Hướng Dẫn Cài Đặt

## Yêu Cầu Trước

- **Java 17** (hoặc phiên bản được chỉ định trong `pom.xml`).
- **Maven** (wrapper đã được bao gồm, bạn có thể sử dụng `./mvnw`).
- **Docker** và **Docker Compose** (cần thiết để chạy các micro‑service).

## Lấy Mã Nguồn

```bash
# Sao chép repository (nếu chưa có)
git clone https://github.com/Ncyntrq/chat-server-microservices.git
cd chat-server-microservices
```

## Xây Dựng Dự Án

Bạn có thể xây dựng tất cả các module bằng Maven wrapper:

```bash
./mvnw clean install -DskipTests
```

Lệnh này sẽ biên dịch mã Java và đóng gói mỗi service thành một image Docker (các Dockerfile đã có sẵn).

## Cấu Trúc Thư Mục

- `common-lib/` – các tiện ích chung.
- `gateway-service/`, `log-service/`, `presence-service/` – các service Spring Boot riêng lẻ.
- `docker-compose.yml` – điều phối các service cùng nhau.
- `doc/` – thư mục này chứa tất cả các file tài liệu.

Bạn có thể bổ sung thêm chi tiết khi dự án phát triển.

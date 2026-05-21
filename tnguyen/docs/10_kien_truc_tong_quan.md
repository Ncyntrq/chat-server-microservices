# 🏗️ Kiến Trúc Tổng Quan — Chat Server Microservices

> **Phiên bản:** 2.0 (Discord-like)
> **Cập nhật:** 17/05/2026
> **Tech Stack:** Java 17 · Spring Boot 3.2.4 · Spring Cloud 2023.0.1 · Docker

---

## 1. Sơ Đồ Kiến Trúc Tổng Thể

```
                          ┌─────────────────────┐
                          │    Client App        │
                          │  (Java Swing / Web)  │
                          └──────────┬───────────┘
                                     │
                          REST (HTTP) + WebSocket
                                     │
                          ┌──────────▼───────────┐
                          │   API Gateway :8080   │
                          │  (Spring Cloud GW)    │
                          │  JWT Filter · Routing │
                          └──────────┬───────────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            │                        │                        │
   ┌────────▼────────┐    ┌─────────▼─────────┐    ┌────────▼────────┐
   │  auth-service   │    │ messaging-service  │    │ server-service  │
   │     :8081       │    │      :8082         │    │     :8085       │
   │ Register/Login  │    │ WebSocket Chat     │    │ CRUD Server     │
   │ JWT · BCrypt    │    │ Edit/Delete/Typing │    │ Join/Leave      │
   └────────┬────────┘    └──┬──────┬──────┬───┘    └────────┬────────┘
            │                │      │      │                 │
            │ DB: chat_      │      │      │ OpenFeign       │ OpenFeign
            │ auth_db        │      │      └─────────────────┘
            │                │      │                         
            │         publish│      │publish          ┌────────────────┐
            │                │      │                 │ channel-service │
            │          ┌─────▼──────▼──────┐          │     :8086       │
            │          │    RabbitMQ        │          │ CRUD Channel    │
            │          │  chat.exchange     │          └────────┬────────┘
            │          │  (Topic Exchange)  │                   │
            │          └──┬──────────┬─────┘          DB: chat_channel_db
            │             │          │
            │    log.#    │          │ notify.#
            │             │          │
            │    ┌────────▼──┐  ┌────▼─────────────┐
            │    │log-service│  │notification-svc  │
            │    │   :8084   │  │     :8088         │
            │    │ Audit Log │  │ @mention · Unread │
            │    └───────────┘  └──────────────────┘
            │
   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
   │presence-service│  │  file-service   │  │   MinIO (S3)   │
   │     :8083      │  │     :8089       │  │  :9000 / :9001 │
   │ Online/Offline │  │ Upload/Download │──│ chat-files     │
   │ Idle/DND       │  │ Thumbnail Gen  │  │ chat-thumbnails│
   └───────┬────────┘  └───────┬────────┘  └────────────────┘
           │                   │
   DB: chat_presence_db   DB: chat_file_db


   ┌─────────────────────────────────────────────────┐
   │              MySQL :3307 (Docker)                │
   │  chat_auth_db · chat_server_db · chat_channel_db │
   │  chat_messaging_db · chat_presence_db            │
   │  chat_notification_db · chat_file_db             │
   └─────────────────────────────────────────────────┘
```

---

## 2. Danh Sách Services

### 2.1. Bảng Tổng Hợp

| # | Service | Port | Database | Trạng thái | Chức năng chính |
|---|---------|------|----------|------------|-----------------|
| 1 | `gateway-service` | **8080** | — | ✅ Done | Entry-point, JWT filter, routing, WS proxy |
| 2 | `auth-service` | 8081 | `chat_auth_db` | ✅ Done | Register, Login, JWT, BCrypt |
| 3 | `messaging-service` | 8082 | `chat_messaging_db` | ✅ Done | WebSocket chat, Edit/Delete, Typing, Persistence |
| 4 | `presence-service` | 8083 | `chat_presence_db` | ✅ Done | Online/Offline/Idle/DND tracking |
| 5 | `log-service` | 8084 | — (file) | ✅ Done | RabbitMQ consumer, audit log, REST query |
| 6 | `server-service` | 8085 | `chat_server_db` | ✅ Done | CRUD Server/Group, Join/Leave, Members |
| 7 | `channel-service` | 8086 | `chat_channel_db` | ✅ Done | CRUD Channel, Category |
| 8 | `notification-service` | 8088 | `chat_notification_db` | 🟡 Scaffold | @mention, @everyone, Unread count, Ack |
| 9 | `file-service` | 8089 | `chat_file_db` | 🟡 Scaffold | Upload/Download MinIO, Thumbnail |
| 10 | `role-service` | 8090 | `chat_role_db` | ❌ Chưa có | Roles, Permissions, Kick/Ban |
| 11 | `user-profile-service` | 8091 | `chat_profile_db` | ❌ Chưa có | Avatar, Bio, Custom status, Search |

### 2.2. Infrastructure Services

| Service | Port (Host) | Port (Docker) | Dùng bởi |
|---------|-------------|---------------|----------|
| **MySQL 8.0** | 3307 | 3306 | Tất cả services (mỗi service DB riêng) |
| **RabbitMQ 3** | 5672 (AMQP) / 15672 (UI) | 5672 / 15672 | messaging, log, notification |
| **MinIO** | 9000 (S3) / 9001 (Console) | 9000 / 9001 | file-service |

---

## 3. Giao Tiếp Giữa Các Service

### 3.1. Synchronous — REST / OpenFeign

```
messaging-service ──OpenFeign──► server-service
    Kiểm tra quyền thành viên trước khi gửi tin

messaging-service ──OpenFeign──► auth-service
    Validate JWT token khi kết nối WebSocket

server-service ──OpenFeign──► channel-service
    Tạo default channel khi tạo server mới
```

| Caller | Callee | Mục đích |
|--------|--------|----------|
| `messaging-service` | `server-service` | Check membership trước khi chat |
| `messaging-service` | `auth-service` | Validate JWT token |
| `server-service` | `channel-service` | Auto-create #general channel |

### 3.2. Asynchronous — RabbitMQ (Topic Exchange)

```
                    ┌───────────────────────────┐
                    │   Exchange: chat.exchange  │
                    │   Type: Topic, Durable     │
                    └─────┬──────────────┬───────┘
                          │              │
              Routing: log.#    Routing: notify.#
                          │              │
              ┌───────────▼──┐  ┌────────▼──────────┐
              │chat.log.queue│  │chat.notification   │
              │              │  │       .queue        │
              └───────┬──────┘  └────────┬───────────┘
                      │                  │
              log-service        notification-service
```

**Producer:** `messaging-service`

| Routing Key | Payload | Consumer |
|-------------|---------|----------|
| `log.chat` / `log.private` / `log.join` / `log.leave` | `LogEntry` | `log-service` |
| `notify.message` | `MessageDTO` | `notification-service` |

### 3.3. WebSocket — Real-time

```
Client ──ws://gateway:8080/ws/chat?token=JWT──► gateway-service ──proxy──► messaging-service:8082
```

Payload format (JSON qua WebSocket):
```json
{ "type": "CHAT", "channelId": 1, "serverId": 1, "content": "Hello!" }
{ "type": "TYPING", "channelId": 1, "serverId": 1 }
{ "type": "EDIT", "messageId": 42, "content": "Sửa rồi" }
{ "type": "DELETE", "messageId": 42 }
```

---

## 4. Gateway Routing Table

| Priority | Route ID | Path Pattern | Target Service |
|----------|----------|--------------|----------------|
| 1 | `messaging-messages` | `/api/channels/*/messages/**` | messaging :8082 |
| 2 | `notification-ack` | `/api/channels/*/ack/**` | notification :8088 |
| 3 | `channel-nested` | `/api/servers/*/channels/**` | channel :8086 |
| 4 | `role-service-nested` | `/api/servers/*/roles/**`, `/api/servers/*/kick/**`, ... | role :8090 |
| 5 | `auth-service` | `/api/auth/**` | auth :8081 |
| 6 | `user-profile-service` | `/api/users/**` | user-profile :8091 |
| 7 | `server-service` | `/api/servers/**` | server :8085 |
| 8 | `channel-service` | `/api/channels/**` | channel :8086 |
| 9 | `messaging-service` | `/api/messages/**` | messaging :8082 |
| 10 | `messaging-ws` | `/ws/**` | ws://messaging :8082 |
| 11 | `presence-service` | `/api/presence/**` | presence :8083 |
| 12 | `notification-service` | `/api/notifications/**` | notification :8088 |
| 13 | `file-service` | `/api/files/**` | file :8089 |
| 14 | `log-service` | `/api/logs/**` | log :8084 |

> ⚠️ Thứ tự quan trọng! Routes cụ thể (nested paths) phải đặt **trước** routes tổng quát.

---

## 5. Shared Library — `common-lib`

Thư viện dùng chung, mọi service đều depend vào `com.chatsever:common-lib:1.0.0-SNAPSHOT`.

```
common-lib/src/main/java/com/chatsever/common/
├── dto/
│   ├── AuthRequest.java       # username, password
│   ├── AuthResponse.java      # token, username
│   ├── ChannelDto.java        # channel metadata
│   ├── LogEntry.java          # timestamp, eventType, sender, receiver, content
│   ├── MemberDto.java         # member info
│   ├── MessageDTO.java        # type, sender, receiver, content, timestamp (5 fields)
│   └── ServerDto.java         # server metadata
├── enums/
│   └── MessageType.java       # CHAT, PRIVATE, JOIN, LEAVE, TYPING, EDIT, DELETE...
└── util/
    └── SecurityUtil.java      # BCrypt password encoding/matching
```

> ⚠️ **`MessageDTO` hiện chưa có `channelId` / `serverId`** — cần thêm nếu notification-service muốn biết tin nhắn thuộc channel nào.

---

## 6. Database Per Service

Mỗi service sở hữu database riêng (Database per Service pattern). **Không JOIN chéo database.**

```
MySQL Instance (:3307)
 ├── chat_auth_db          ← auth-service         (users, credentials)
 ├── chat_server_db        ← server-service       (servers, members)
 ├── chat_channel_db       ← channel-service      (channels, categories)
 ├── chat_messaging_db     ← messaging-service    (messages, attachments)
 ├── chat_presence_db      ← presence-service     (user_presence)
 ├── chat_notification_db  ← notification-service (notifications, read_status)
 ├── chat_file_db          ← file-service         (file_metadata)
 ├── chat_role_db          ← role-service         (roles, permissions) [chưa có]
 └── chat_profile_db       ← user-profile-service (profiles)          [chưa có]
```

Khi cần dữ liệu liên kết:
- **Sync:** Gọi REST API qua OpenFeign
- **Async:** Publish/consume event qua RabbitMQ

---

## 7. Bảo Mật

### 7.1. Authentication Flow

```
Client ──POST /api/auth/login──► auth-service
                                      │
                                 Validate credentials
                                 Hash check (BCrypt)
                                      │
                               ◄── JWT Token (2h expiry)

Client ──Bearer JWT──► gateway-service
                            │
                       JwtAuthFilter
                       Decode & verify
                            │
                       Forward to target service
```

### 7.2. JWT Config

| Thuộc tính | Giá trị |
|------------|---------|
| Algorithm | HS256 |
| Access Token Expiry | 2 giờ |
| Secret Key | `jwt.secret` (env: `JWT_SECRET`) |

### 7.3. Password Security

- **BCrypt** encoding (via `SecurityUtil` trong `common-lib`)
- Không lưu plaintext password

---

## 8. Docker Compose — Topology

```
docker-compose.yml
│
├── Infrastructure
│   ├── mysql-db        (mysql:8.0)         → 3307:3306
│   ├── rabbitmq        (rabbitmq:3-mgmt)   → 5672, 15672
│   └── minio           (minio/minio)       → 9000, 9001
│
├── Gateway
│   └── gateway-service                     → 8080:8080
│       depends_on: auth-service, messaging-service
│
└── Microservices (all on chat-net bridge)
    ├── auth-service        depends_on: mysql-db (healthy)
    ├── server-service      depends_on: mysql-db, channel-service
    ├── channel-service     depends_on: mysql-db
    ├── messaging-service   depends_on: rabbitmq, mysql-db
    ├── presence-service    depends_on: mysql-db
    ├── log-service         depends_on: rabbitmq
    ├── notification-service depends_on: rabbitmq, mysql-db
    ├── file-service        depends_on: minio, mysql-db
    ├── # role-service      (commented out)
    └── # user-profile-svc  (commented out)

Network: chat-net (bridge)
Volumes: mysql-data, rabbitmq-data, minio-data, log-data
```

---

## 9. Build & Run

### 9.1. Thứ tự khởi chạy

```
1. docker compose up -d                          # Infra (MySQL, RabbitMQ, MinIO)
2. ./mvnw -pl common-lib clean install -DskipTests  # Build shared lib
3. ./mvnw -pl auth-service spring-boot:run        # Auth trước
4. ./mvnw -pl presence-service spring-boot:run
5. ./mvnw -pl server-service spring-boot:run
6. ./mvnw -pl channel-service spring-boot:run
7. ./mvnw -pl messaging-service spring-boot:run
8. ./mvnw -pl log-service spring-boot:run
9. ./mvnw -pl notification-service spring-boot:run
10. ./mvnw -pl file-service spring-boot:run
11. ./mvnw -pl gateway-service spring-boot:run     # Gateway cuối cùng
```

### 9.2. Health Check

```bash
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # Auth
curl http://localhost:8084/actuator/health   # Log
curl http://localhost:8088/actuator/health   # Notification
curl http://localhost:8089/actuator/health   # File
```

---

## 10. Roadmap — Những Module Còn Thiếu

| # | Module | Ưu tiên | Mô tả |
|---|--------|---------|-------|
| 1 | **Role & Permission Service** | 🔴 Cao | Phân quyền Owner/Admin/Mod/Member, Kick/Ban |
| 2 | **User Profile Service** | 🟡 Trung bình | Avatar, Bio, Custom status, Search user |
| 3 | **Client App (Swing)** | 🟡 Trung bình | GUI Discord-like: sidebar, chat, members |
| 4 | **Invite Code** (server-service) | 🟢 Thấp | Hệ thống mã mời vào Server |
| 5 | **Hoàn thiện Notification** | 🟡 Trung bình | Test tích hợp, WebSocket push |
| 6 | **Hoàn thiện File Service** | 🟡 Trung bình | Test upload thật với MinIO |

---

*Tài liệu này được tạo tự động từ phân tích codebase ngày 17/05/2026.*
*Tham chiếu: `01_yeu_cau_phan_mem.md`, `08_plan_notification_file_service.md`, `docker-compose.yml`, `gateway application.yml`*

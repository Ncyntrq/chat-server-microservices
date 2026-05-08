# 1. Phân Tích Yêu Cầu Phần Mềm

## 1.1. Mục tiêu

Xây dựng hệ thống Chat **thời gian thực** theo kiến trúc **Microservice**,
lấy cảm hứng từ **Discord** — hỗ trợ **phòng chat (channels)**, **server (nhóm)**,
nhắn tin riêng, quản lý vai trò, chia sẻ file, và nhiều tính năng cộng tác.
Mỗi chức năng nghiệp vụ được tách thành **dịch vụ độc lập**, giao tiếp qua REST API, WebSocket và Message Queue.

## 1.2. Công nghệ sử dụng

| Thành phần | Công nghệ |
|-----------|-----------|
| **Ngôn ngữ** | Java 17+ |
| **Framework** | Spring Boot 3.x, Spring Cloud |
| **API Gateway** | Spring Cloud Gateway |
| **Realtime** | WebSocket (Raw WebSocket — `TextWebSocketHandler`) |
| **Authentication** | JWT (JSON Web Token) |
| **Password Hashing** | BCrypt (`BCryptPasswordEncoder` — Spring Security) |
| **Message Queue** | RabbitMQ (giao tiếp async giữa services) |
| **Database** | H2 (dev) / MySQL (production) — mỗi service DB riêng |
| **File Storage** | Local filesystem (dev) / MinIO - S3 compatible (production) |
| **Build Tool** | Maven (multi-module project) |
| **Containerization** | Docker + Docker Compose |
| **Giao diện** | Java Swing (có thể mở rộng sang Web) |

---

## 1.3. Yêu Cầu Chức Năng

### A. Auth Service — Dịch vụ xác thực (6 chức năng)

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| A1 | Đăng ký tài khoản | Nhận `POST /api/auth/register`, validate, hash password **BCrypt**, lưu DB |
| A2 | Đăng nhập | Nhận `POST /api/auth/login`, xác thực → trả về JWT access token (expiry 2h) |
| A3 | Xác thực token | Nhận `POST /api/auth/validate`, kiểm tra JWT hợp lệ |
| A4 | Lưu trữ tài khoản | Persistence vào DB (H2/MySQL), không lưu plaintext password |
| A5 | Refresh Token | `POST /api/auth/refresh` — cấp access token mới từ refresh token (expiry 7 ngày) |
| A6 | Đổi mật khẩu | `POST /api/auth/change-password` — xác thực mật khẩu cũ → BCrypt hash mới |

### B. User Profile Service — Dịch vụ hồ sơ người dùng (5 chức năng) ★ MỚI

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| UP1 | Xem hồ sơ | `GET /api/users/{username}/profile` — trả về displayName, avatar URL, bio, status |
| UP2 | Cập nhật hồ sơ | `PUT /api/users/profile` — cập nhật displayName, bio, custom status |
| UP3 | Upload avatar | `POST /api/users/avatar` — upload ảnh đại diện (max 2MB, JPEG/PNG) |
| UP4 | Đặt trạng thái tùy chỉnh | `PUT /api/users/status` — set custom status (emoji + text, VD: "🎮 Đang chơi game") |
| UP5 | Tìm kiếm user | `GET /api/users/search?q=keyword` — tìm user theo username hoặc displayName |

### C. Server Service — Dịch vụ quản lý Server/Nhóm (8 chức năng) ★ MỚI

> **Khái niệm:** "Server" tương tự Discord Server — một nhóm chứa nhiều channels, có hệ thống vai trò (roles).

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| SV1 | Tạo server | `POST /api/servers` — tạo server mới, người tạo tự động thành **Owner** |
| SV2 | Xem danh sách server | `GET /api/servers` — trả về danh sách servers mà user đã tham gia |
| SV3 | Xem chi tiết server | `GET /api/servers/{serverId}` — thông tin server + danh sách channels + members |
| SV4 | Cập nhật server | `PUT /api/servers/{serverId}` — đổi tên, icon, mô tả (chỉ Owner/Admin) |
| SV5 | Xóa server | `DELETE /api/servers/{serverId}` — chỉ Owner được xóa |
| SV6 | Tham gia server | `POST /api/servers/{serverId}/join` — user gia nhập server (qua invite code) |
| SV7 | Rời server | `POST /api/servers/{serverId}/leave` — user rời khỏi server |
| SV8 | Tạo invite code | `POST /api/servers/{serverId}/invite` — tạo mã mời (expiry tùy chọn) |

### D. Channel Service — Dịch vụ quản lý kênh/phòng chat (7 chức năng) ★ MỚI

> **Khái niệm:** "Channel" tương tự Discord Channel — mỗi server có nhiều channels (text/voice).

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| CH1 | Tạo channel | `POST /api/servers/{serverId}/channels` — tạo channel mới (TEXT hoặc VOICE) |
| CH2 | Xem danh sách channels | `GET /api/servers/{serverId}/channels` — liệt kê tất cả channels trong server |
| CH3 | Cập nhật channel | `PUT /api/channels/{channelId}` — đổi tên, topic, slowmode |
| CH4 | Xóa channel | `DELETE /api/channels/{channelId}` — chỉ Admin+ được xóa |
| CH5 | Quản lý category | Channel có thể nhóm vào category (VD: "General", "Gaming") |
| CH6 | Ghim tin nhắn | `POST /api/channels/{channelId}/pins/{messageId}` — ghim tin nhắn quan trọng |
| CH7 | Xem tin nhắn đã ghim | `GET /api/channels/{channelId}/pins` — danh sách tin ghim |

### E. Messaging Service — Dịch vụ tin nhắn (10 chức năng) ★ MỞ RỘNG

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| M1 | Kết nối WebSocket | Client kết nối qua `ws://host:8082/ws/chat`, xác thực bằng JWT qua query parameter |
| M2 | Chat trong channel | Nhận tin nhắn từ user → broadcast đến tất cả members đang online trong channel |
| M3 | Chat private (DM) | Nhận tin nhắn private → chuyển chỉ đến đúng 1 user đích |
| M4 | Lưu tin nhắn (Persistence) | Mọi tin nhắn được lưu vào DB với `channelId`, `serverId`, hỗ trợ truy vấn lịch sử |
| M5 | Lịch sử tin nhắn | `GET /api/channels/{channelId}/messages?before={msgId}&limit=50` — tải tin nhắn cũ (scroll up) |
| M6 | Sửa tin nhắn | Người gửi có thể sửa nội dung tin nhắn đã gửi (hiển thị "(đã sửa)") |
| M7 | Xóa tin nhắn | Người gửi hoặc Admin có thể xóa tin nhắn |
| M8 | Gửi log event | Publish event lên RabbitMQ (Topic Exchange) để Log Service ghi lại |
| M9 | Heartbeat | Gửi PING/PONG định kỳ để detect dead connections |
| M10 | Typing indicator | Broadcast trạng thái "đang nhập..." khi user gõ phím trong channel |

### F. Role & Permission Service — Dịch vụ phân quyền (6 chức năng) ★ MỚI

> **Khái niệm:** Mỗi server có hệ thống vai trò (roles) riêng, tương tự Discord.

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| R1 | Tạo role | `POST /api/servers/{serverId}/roles` — tạo vai trò mới (tên, màu, permissions) |
| R2 | Gán role cho member | `PUT /api/servers/{serverId}/members/{userId}/roles` — gán/bỏ role |
| R3 | Cấu hình permissions | Mỗi role có bitmask permissions: `SEND_MESSAGE`, `MANAGE_CHANNEL`, `KICK_MEMBER`, `BAN_MEMBER`, `MANAGE_ROLES`, `ADMIN` |
| R4 | Kiểm tra quyền | `GET /api/servers/{serverId}/permissions/{userId}` — trả về effective permissions |
| R5 | Kick member | `POST /api/servers/{serverId}/kick/{userId}` — đuổi member khỏi server |
| R6 | Ban member | `POST /api/servers/{serverId}/ban/{userId}` — cấm vĩnh viễn + không cho join lại |

**Hệ thống vai trò mặc định:**

| Vai trò | Permissions | Mô tả |
|---------|-------------|-------|
| **Owner** | `ALL` | Người tạo server, toàn quyền |
| **Admin** | `MANAGE_CHANNEL`, `KICK`, `BAN`, `MANAGE_ROLES` | Quản trị viên |
| **Moderator** | `KICK`, `MANAGE_MESSAGES` | Điều hành viên |
| **Member** | `SEND_MESSAGE`, `READ_MESSAGES` | Thành viên thường |

### G. Notification Service — Dịch vụ thông báo (5 chức năng) ★ MỚI

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| N1 | Mention (@user) | Đề cập user trong tin nhắn → gửi notification cho user đó |
| N2 | Mention (@everyone) | Thông báo cho tất cả members trong channel |
| N3 | Unread count | Theo dõi số tin nhắn chưa đọc cho mỗi channel/DM |
| N4 | Đánh dấu đã đọc | `POST /api/channels/{channelId}/ack` — đánh dấu đã đọc đến message cuối cùng |
| N5 | Push notification | Gửi notification realtime qua WebSocket khi có tin nhắn mới ở channel không active |

### H. File Service — Dịch vụ chia sẻ file (4 chức năng) ★ MỚI

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| F1 | Upload file | `POST /api/files/upload` — upload file đính kèm tin nhắn (max 10MB) |
| F2 | Download file | `GET /api/files/{fileId}` — tải file |
| F3 | Preview ảnh | Tự động tạo thumbnail cho ảnh (JPEG, PNG, GIF) |
| F4 | Hỗ trợ nhiều định dạng | Cho phép gửi: ảnh, video nhỏ, PDF, document, code snippet |

### I. Presence Service — Dịch vụ hiện diện (5 chức năng) ★ MỞ RỘNG

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| P1 | Đăng ký online | `POST /api/presence/connect` — ghi nhận user online |
| P2 | Đăng ký offline | `POST /api/presence/disconnect` — ghi nhận user offline |
| P3 | Danh sách online | `GET /api/presence/online` — trả về list user đang online |
| P4 | Kiểm tra trạng thái | `GET /api/presence/status/{username}` — kiểm tra online/offline |
| P5 | Trạng thái chi tiết | Hỗ trợ các trạng thái: `ONLINE`, `IDLE`, `DO_NOT_DISTURB`, `INVISIBLE` (như Discord) |

### J. Log Service — Dịch vụ ghi log (3 chức năng)

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| L1 | Nhận log event | Lắng nghe RabbitMQ queue, ghi vào file/DB |
| L2 | Xem lịch sử | `GET /api/logs/history?page=0&size=50` — trả về lịch sử hoạt động (có pagination) |
| L3 | Health check | `GET /actuator/health` — kiểm tra trạng thái service |

### K. API Gateway (3 chức năng)

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| G1 | Routing | Nhận mọi request từ client → forward đến đúng service |
| G2 | WebSocket Proxy | Proxy WebSocket connection từ client → Messaging Service |
| G3 | Health check | Cung cấp `/actuator/health` cho monitoring |

### L. Client App — Ứng dụng giao diện (15 chức năng) ★ MỞ RỘNG

| ID | Chức năng | Mô tả chi tiết |
|----|-----------|-----------------|
| C1 | Kết nối Gateway | Kết nối đến API Gateway (cổng 8080) |
| C2 | Đăng ký tài khoản | Gửi REST request `POST /api/auth/register` |
| C3 | Đăng nhập | Gửi REST request `POST /api/auth/login`, nhận JWT |
| C4 | Sidebar server list | Hiển thị danh sách servers đã tham gia (bên trái, giống Discord) |
| C5 | Channel list | Hiển thị danh sách channels trong server đang chọn |
| C6 | Gửi tin nhắn trong channel | Gửi qua WebSocket connection (JSON format) |
| C7 | Nhận tin nhắn realtime | Nhận qua WebSocket, hiển thị realtime trên GUI |
| C8 | Scroll load lịch sử | Kéo lên trên để tải tin nhắn cũ hơn (lazy loading) |
| C9 | DM (Direct Message) | Giao diện nhắn tin riêng 1-1 |
| C10 | Danh sách members online | Panel bên phải hiển thị members theo role + trạng thái |
| C11 | Tạo/Quản lý server | Dialog tạo server mới, mời members |
| C12 | Upload file/ảnh | Đính kèm file vào tin nhắn, preview ảnh inline |
| C13 | Notification badge | Hiển thị số tin chưa đọc trên mỗi channel/server |
| C14 | Typing indicator | Hiển thị "User đang nhập..." khi người khác gõ |
| C15 | Thoát | Đóng WebSocket, gọi disconnect API |

---

## 1.4. Yêu Cầu Phi Chức Năng

| # | Yêu cầu | Chi tiết |
|---|---------|----------|
| NF1 | **Đồng thời** | Mỗi service xử lý độc lập, hỗ trợ scale horizontal |
| NF2 | **Loose Coupling** | Các service giao tiếp qua API, không chia sẻ DB |
| NF3 | **Bảo mật** | JWT authentication (access 2h + refresh 7d), password hashing **BCrypt** |
| NF4 | **Ổn định** | Mỗi service có thể restart độc lập, không crash toàn hệ thống |
| NF5 | **Giao thức** | REST (HTTP/JSON) cho sync, WebSocket cho realtime, AMQP cho async |
| NF6 | **Lưu trữ** | Mỗi service có DB/storage riêng (Database per Service pattern) |
| NF7 | **Giao diện** | GUI kiểu Discord: sidebar servers, channel list, vùng chat, member list |
| NF8 | **Containerization** | Mỗi service đóng gói thành Docker container |
| NF9 | **Health Check** | Mỗi service cung cấp `/actuator/health` endpoint cho monitoring |
| NF10 | **Error Handling** | Xử lý lỗi chung qua `@ControllerAdvice`, error message rõ ràng |
| NF11 | **Message Ordering** | Tin nhắn hiển thị đúng thứ tự thời gian trong mỗi channel |
| NF12 | **File Size Limit** | Upload file tối đa 10MB, avatar tối đa 2MB |
| NF13 | **Pagination** | Tất cả API trả về danh sách đều hỗ trợ pagination (cursor-based hoặc offset) |

---

## 1.5. Tổng Quan Service Map

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           CHATSEVER — MICROSERVICES MAP                          │
│                                                                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │ Auth Service │ │ UserProfile  │ │ Server Svc   │ │ Channel Svc  │            │
│  │ (đăng nhập,  │ │ (avatar,     │ │ (tạo server, │ │ (tạo channel,│            │
│  │  JWT, BCrypt)│ │  bio, status)│ │  invite, role)│ │  category)   │            │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘            │
│                                                                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │ Messaging    │ │ Role &       │ │ Notification │ │ File Service │            │
│  │ Service      │ │ Permission   │ │ Service      │ │ (upload,     │            │
│  │ (WS, chat,   │ │ (roles, ban, │ │ (mention,    │ │  download,   │            │
│  │  DM, history)│ │  kick, perms)│ │  unread cnt) │ │  thumbnail)  │            │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘            │
│                                                                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                              │
│  │ Presence Svc │ │ Log Service  │ │ API Gateway  │                              │
│  │ (online/idle/│ │ (audit log,  │ │ (routing,    │                              │
│  │  DND, invis.)│ │  history)    │ │  WS proxy)   │                              │
│  └──────────────┘ └──────────────┘ └──────────────┘                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                        Client App (Swing GUI — Discord-style)           │     │
│  │  Server sidebar │ Channel list │ Chat area │ Member list │ DM panel    │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 1.6. Giới Hạn Phạm Vi (Out of Scope)

Các tính năng sau **không nằm trong phạm vi** phiên bản hiện tại, sẽ xem xét mở rộng sau:

| # | Tính năng | Lý do |
|---|-----------|-------|
| S1 | Voice Chat (gọi thoại) | Cần WebRTC + Media Server (Kurento/Janus), phức tạp |
| S2 | Video Call | Cần WebRTC + TURN/STUN server |
| S3 | Screen Sharing | Cần WebRTC |
| S4 | Bot Framework | Cần thiết kế Bot API riêng |
| S5 | Service Discovery (Eureka) | Dùng hardcode URL cho đơn giản, sẽ thêm khi scale |
| S6 | Circuit Breaker (Resilience4j) | Sẽ thêm khi cần fault tolerance nâng cao |
| S7 | End-to-End Encryption | Phức tạp, cần thiết kế key exchange protocol |
| S8 | Reaction/Emoji trên tin nhắn | Có thể thêm nhanh sau MVP |
| S9 | Thread/Reply (trả lời tin nhắn) | Sẽ thêm khi UX ổn định |
| S10 | Search tin nhắn full-text | Cần Elasticsearch, nằm ngoài scope hiện tại |

---

## 1.7. So Sánh Với Phiên Bản Cũ

| Tính năng | Phiên bản cũ (v1) | Phiên bản mới (v2 — Discord-like) |
|-----------|--------------------|------------------------------------|
| Chat | Broadcast + Private (2 kiểu) | Channel chat + DM + Server context |
| Phòng chat | ❌ Out of scope | ✅ Channels trong Servers |
| Lưu tin nhắn | ❌ Chỉ log file | ✅ DB persistence + lịch sử scroll |
| User profile | ❌ Chỉ username | ✅ Avatar, bio, custom status |
| Vai trò | ❌ Không có | ✅ Owner/Admin/Moderator/Member + custom roles |
| File sharing | ❌ Out of scope | ✅ Upload file/ảnh (max 10MB) |
| Thông báo | ❌ Không có | ✅ @mention, unread count, badges |
| Trạng thái | Online/Offline | Online/Idle/DND/Invisible |
| Typing indicator | ❌ | ✅ "User đang nhập..." |
| Sửa/xóa tin nhắn | ❌ | ✅ Edit + Delete |
| Invite system | ❌ | ✅ Invite code cho server |
| Tổng services | 6 services | 11 services |
| Tổng chức năng | ~26 chức năng | ~77 chức năng |

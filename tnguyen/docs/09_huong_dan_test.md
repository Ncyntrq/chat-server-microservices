# 🧪 Hướng Dẫn Test: Notification Service + File Service (IntelliJ IDEA)

> **Cập nhật:** 2026-05-08 — Đồng bộ với codebase sau `git pull main`
>
> **Hiện trạng codebase:**
> - `common-lib` ✅ — `MessageDTO` (5 field: type, sender, receiver, content, timestamp — **chưa có channelId/serverId**)
> - `auth-service` ✅ (port 8081) — register, login, validate token (JWT)
> - `messaging-service` ✅ (port 8082) — WebSocket chat, publish log event routing key `log.<type>`
> - `presence-service` ✅ (port 8083) — connect, disconnect, online list
> - `log-service` ✅ (port 8084) — consume queue `chat.log.queue`, binding `log.#`
> - `gateway-service` ✅ (port 8080) — route 4 services trên
> - `notification-service` ❌ — **Chưa có, bạn sẽ tạo**
> - `file-service` ❌ — **Chưa có, bạn sẽ tạo**

---

## MỤC LỤC

1. [Chuẩn bị môi trường](#1-chuẩn-bị-môi-trường)
2. [Test toàn bộ hệ thống hiện có trước](#2-test-hệ-thống-hiện-có)
3. [Test Notification Service](#3-test-notification-service)
4. [Test File Service](#4-test-file-service)
5. [Test tích hợp end-to-end](#5-test-tích-hợp-end-to-end)
6. [Chạy Unit Test](#6-chạy-unit-test)

---

## 1. Chuẩn Bị Môi Trường

### 1.1. Khởi động infra bằng Docker

Mở Terminal trong IntelliJ (**Alt+F12**):

```bash
# RabbitMQ — BẮT BUỘC cho messaging-service, log-service, notification-service
docker run -d --name test-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management

# MinIO — cần cho file-service
docker run -d --name test-minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

**Verify:**
- RabbitMQ UI: http://localhost:15672 (login: `guest` / `guest`)
- MinIO Console: http://localhost:9001 (login: `minioadmin` / `minioadmin`)

### 1.2. Tạo bucket MinIO

Mở http://localhost:9001 → **Buckets** → **Create Bucket**:
- Tạo bucket: `chat-files`
- Tạo bucket: `chat-thumbnails`

### 1.3. Import project trong IntelliJ

1. File → Open → chọn thư mục `chat-server-microservices`
2. IntelliJ tự detect multi-module Maven → đợi import xong
3. Nếu không thấy module mới: chuột phải `pom.xml` (root) → **Maven** → **Reload Project**

### 1.4. Thứ tự chạy services (quan trọng!)

Services có dependency lẫn nhau, phải chạy **đúng thứ tự**:

```
1. RabbitMQ (Docker)        ← messaging, log, notification cần
2. auth-service (8081)      ← messaging cần validate token
3. presence-service (8083)  ← messaging cần báo online/offline
4. messaging-service (8082) ← publish event vào RabbitMQ
5. log-service (8084)       ← consume event từ RabbitMQ
6. notification-service (8088) ← consume event từ RabbitMQ (BẠN TẠO)
7. file-service (8089)      ← độc lập, chỉ cần MinIO (BẠN TẠO)
```

Cách chạy trong IntelliJ: mở file `XxxApplication.java` → chuột phải → **Run**.

---

## 2. Test Hệ Thống Hiện Có

> Trước khi test module bạn tạo, hãy confirm hệ thống hiện tại chạy OK.
> Mở file `tnguyen/test/test-system.http` trong IntelliJ → click ▶ từng request.

### 2.1. Test Auth Service (port 8081)

Mở `tnguyen/test/test-system.http`:

```http
### ═══ AUTH SERVICE (port 8081) ═══

### A1. Đăng ký tài khoản mới
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "nguyen",
  "password": "123456"
}

### A2. Đăng nhập → lấy JWT token
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "nguyen",
  "password": "123456"
}
# → Kết quả: {"token": "eyJhbGci...", "username": "nguyen"}
# ⚠ COPY giá trị token để dùng ở bước tiếp theo!

### A3. Validate token (paste token vừa lấy)
POST http://localhost:8081/api/auth/validate
Content-Type: application/json

{
  "token": "PASTE_TOKEN_VÀO_ĐÂY"
}

### A4. Đăng ký user thứ 2 (để test chat)
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "trang",
  "password": "123456"
}

### A5. Đăng nhập user thứ 2
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "trang",
  "password": "123456"
}
```

### 2.2. Test Presence Service (port 8083)

```http
### ═══ PRESENCE SERVICE (port 8083) ═══

### P1. Đánh dấu nguyen online
POST http://localhost:8083/api/presence/connect?username=nguyen

### P2. Danh sách online
GET http://localhost:8083/api/presence/online

### P3. Kiểm tra trạng thái nguyen
GET http://localhost:8083/api/presence/status/nguyen

### P4. Đánh dấu nguyen offline
POST http://localhost:8083/api/presence/disconnect?username=nguyen
```

### 2.3. Test Log Service (port 8084)

```http
### ═══ LOG SERVICE (port 8084) ═══

### L1. Health check
GET http://localhost:8084/actuator/health

### L2. Xem lịch sử log (ban đầu rỗng)
GET http://localhost:8084/api/logs/history?page=0&size=50
```

### 2.4. Test Messaging qua WebSocket

Mở 2 tab trình duyệt hoặc dùng **wscat**:

```bash
# Cài wscat (1 lần)
npm install -g wscat

# Terminal 1: nguyen kết nối (thay TOKEN bằng JWT thật từ bước A2)
wscat -c "ws://localhost:8082/ws/chat?token=PASTE_TOKEN_NGUYEN"

# Terminal 2: trang kết nối
wscat -c "ws://localhost:8082/ws/chat?token=PASTE_TOKEN_TRANG"
```

Gửi tin broadcast (trong terminal nguyen):
```json
{"type":"CHAT","content":"Hello mọi người!"}
```

Gửi tin riêng (trong terminal nguyen):
```json
{"type":"PRIVATE","receiver":"trang","content":"Hi Trang riêng"}
```

→ Sau đó check log-service đã nhận event:
```http
### L3. Xem log sau khi chat
GET http://localhost:8084/api/logs/history?page=0&size=50
```

---

## 3. Test Notification Service (port 8088)

> **LƯU Ý QUAN TRỌNG:** `MessageDTO` hiện tại chỉ có 5 field: `type`, `sender`, `receiver`, `content`, `timestamp`.
> **CHƯA CÓ `channelId` / `serverId`.**
>
> Khi tạo notification-service, bạn cần quyết định:
> - **Option A:** Thêm `channelId`/`serverId` vào `MessageDTO` trong `common-lib` (cần cả team đồng ý)
> - **Option B:** Notification service parse từ `content` hoặc dùng DTO riêng

### 3.0. Chạy Notification Service

Trong IntelliJ: Run `NotificationApplication.java` → chờ log:
```
Started NotificationApplication in X.XXX seconds
Tomcat started on port 8088
```

### 3.1. Health Check

```http
### ═══ NOTIFICATION SERVICE (port 8088) ═══

### N1. Health check — phải trả {"status":"UP"}
GET http://localhost:8088/actuator/health
Accept: application/json
```

Expected: `{"status":"UP"}`

### 3.2. Lấy danh sách notification (lúc đầu rỗng)

```http
### N2. Notifications của user "nguyen" — ban đầu rỗng
GET http://localhost:8088/api/notifications?userId=nguyen&unreadOnly=false
Accept: application/json
```

Expected: `[]`

### 3.3. Lấy unread count (lúc đầu rỗng)

```http
### N3. Unread count — ban đầu rỗng
GET http://localhost:8088/api/notifications/unread-count?userId=nguyen
Accept: application/json
```

Expected: `{}`

### 3.4. Đánh dấu đã đọc (acknowledge)

```http
### N4. Đánh dấu đã đọc — channel 1
POST http://localhost:8088/api/channels/1/ack
Content-Type: application/json

{
  "userId": "nguyen",
  "lastMessageId": 100
}
```

Expected: `200 OK`

### 3.5. Gửi event giả qua RabbitMQ → test consumer

Notification service cần lắng nghe event từ RabbitMQ. Vì messaging-service hiện publish với routing key `log.<type>`, bạn có 2 cách:

**Cách 1 — Dùng routing key riêng `notify.*` (khuyên dùng):**

Vào RabbitMQ UI: http://localhost:15672 → **Exchanges** → `chat.exchange` → **Publish message**

Routing key: `notify.chat`
Properties: `content_type` = `application/json`

Payload — **dùng đúng format `LogEntry` vì messaging-service publish LogEntry**:

```json
{
  "timestamp": "2026-05-08T14:00:00",
  "eventType": "CHAT",
  "sender": "trang",
  "receiver": null,
  "content": "Hello @nguyen, bạn có khỏe không?"
}
```

**Cách 2 — Bind cùng routing key `log.#` như log-service:**

Notification service bind queue vào `log.#` để nhận cùng event messaging-service đang publish. Payload giống trên, routing key `log.chat`.

### 3.6. Kiểm tra notification đã tạo

```http
### N5. Lấy notifications sau khi RabbitMQ publish
GET http://localhost:8088/api/notifications?userId=nguyen&unreadOnly=true
Accept: application/json
```

Expected (ví dụ):
```json
[
  {
    "id": 1,
    "userId": "nguyen",
    "sender": "trang",
    "type": "MENTION",
    "content": "Hello @nguyen, bạn có khỏe không?",
    "read": false,
    "createdAt": "2026-05-08T14:00:00"
  }
]
```

### 3.7. Test @everyone mention

Publish vào RabbitMQ:

```json
{
  "timestamp": "2026-05-08T14:05:00",
  "eventType": "CHAT",
  "sender": "admin",
  "receiver": null,
  "content": "Thông báo cho @everyone: họp lúc 15h!"
}
```

### 3.8. Test tin nhắn DM (private)

```json
{
  "timestamp": "2026-05-08T14:06:00",
  "eventType": "PRIVATE",
  "sender": "bob",
  "receiver": "nguyen",
  "content": "Bạn có rảnh không?"
}
```

### 3.9. Đánh dấu notification đã đọc

```http
### N6. Đánh dấu notification id=1 đã đọc
PUT http://localhost:8088/api/notifications/1/read
```

Expected: `200 OK`

### 3.10. Kiểm tra unread count

```http
### N7. Unread count sau khi có tin nhắn
GET http://localhost:8088/api/notifications/unread-count?userId=nguyen
Accept: application/json
```

---

## 4. Test File Service (port 8089)

### 4.0. Chạy File Service

Trong IntelliJ: Run `FileApplication.java` → chờ log:
```
Started FileApplication in X.XXX seconds
Tomcat started on port 8089
```

### 4.1. Chuẩn bị file test

Tạo ảnh test nhanh (trong IntelliJ Terminal):

```bash
# Tạo ảnh PNG 100x100 pixel (cần ImageMagick)
convert -size 100x100 xc:blue tnguyen/test/test-image.png

# Nếu không có ImageMagick, dùng bất kỳ ảnh PNG/JPEG nào bạn có
# hoặc download 1 ảnh:
curl -o tnguyen/test/test-image.png https://via.placeholder.com/100x100.png
```

### 4.2. Health Check

```http
### ═══ FILE SERVICE (port 8089) ═══

### F1. Health check
GET http://localhost:8089/actuator/health
Accept: application/json
```

### 4.3. Upload ảnh (test thumbnail tự động)

Dùng **curl** trong IntelliJ Terminal (**Alt+F12**):

```bash
curl -v -X POST http://localhost:8089/api/files/upload \
  -F "file=@tnguyen/test/test-image.png" \
  -F "userId=nguyen" \
  -F "channelId=1"
```

Expected:
```json
{
  "id": 1,
  "originalName": "test-image.png",
  "contentType": "image/png",
  "fileSize": 12345,
  "url": "/api/files/1",
  "thumbnailUrl": "/api/files/1/thumbnail",
  "uploader": "nguyen",
  "channelId": 1,
  "createdAt": "2026-05-08T14:10:00"
}
```

### 4.4. Upload file text (không có thumbnail)

```bash
echo "Nội dung file text thử nghiệm" > tnguyen/test/test-file.txt

curl -v -X POST http://localhost:8089/api/files/upload \
  -F "file=@tnguyen/test/test-file.txt;type=text/plain" \
  -F "userId=nguyen" \
  -F "channelId=1"
```

Expected: `thumbnailUrl` = `null` (vì không phải ảnh)

### 4.5. Upload file quá lớn (test validation — max 10MB)

```bash
# Tạo file 11MB
dd if=/dev/zero of=tnguyen/test/test-big.bin bs=1M count=11 2>/dev/null

curl -v -X POST http://localhost:8089/api/files/upload \
  -F "file=@tnguyen/test/test-big.bin" \
  -F "userId=nguyen" \
  -F "channelId=1"

# Xong xóa file test
rm tnguyen/test/test-big.bin
```

Expected: `400` hoặc `413` — file quá lớn

### 4.6. Xem thông tin file

```http
### F2. Metadata file id=1
GET http://localhost:8089/api/files/1/info
Accept: application/json
```

### 4.7. Download file gốc

```bash
# Lưu ra file và mở kiểm tra
curl -o tnguyen/test/downloaded-image.png http://localhost:8089/api/files/1
```

Hoặc mở trình duyệt: http://localhost:8089/api/files/1

### 4.8. Download thumbnail

```bash
curl -o tnguyen/test/downloaded-thumb.jpg http://localhost:8089/api/files/1/thumbnail
```

Mở `tnguyen/test/downloaded-thumb.jpg` → phải là ảnh nhỏ 200×200

### 4.9. File không tồn tại

```http
### F3. File không tồn tại → 404
GET http://localhost:8089/api/files/9999
Accept: application/json
```

Expected: `404 Not Found`

### 4.10. Xóa file (owner)

```http
### F4. Xóa file (chỉ owner được xóa)
DELETE http://localhost:8089/api/files/1?userId=nguyen
```

Expected: `200 OK`

### 4.11. Xóa file (không phải owner)

```http
### F5. Xóa file của người khác → 403
DELETE http://localhost:8089/api/files/2?userId=hacker
```

Expected: `403 Forbidden`

### 4.12. Verify trên MinIO Console

Mở http://localhost:9001 → **Object Browser**:
- Bucket `chat-files` → phải thấy file vừa upload (tên UUID)
- Bucket `chat-thumbnails` → phải thấy thumbnail tương ứng (prefix `thumb_`)

---

## 5. Test Tích Hợp End-to-End

> Test flow hoàn chỉnh: đăng nhập → chat qua WebSocket → notification service nhận event → tạo notification

### Bước 1: Chạy đủ 6 service

```
auth-service (8081) → presence-service (8083) → messaging-service (8082)
→ log-service (8084) → notification-service (8088) → file-service (8089)
```

### Bước 2: Đăng ký + lấy token

```http
### E2E: Đăng ký 2 user
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{"username": "alice", "password": "pass123"}

###

POST http://localhost:8081/api/auth/register
Content-Type: application/json

{"username": "bob", "password": "pass123"}

###

### Lấy token alice
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{"username": "alice", "password": "pass123"}
```

### Bước 3: Kết nối WebSocket

```bash
# Terminal 1 — alice
wscat -c "ws://localhost:8082/ws/chat?token=PASTE_TOKEN_ALICE"

# Terminal 2 — bob
wscat -c "ws://localhost:8082/ws/chat?token=PASTE_TOKEN_BOB"
```

### Bước 4: Gửi tin nhắn có @mention

Trong terminal alice, gõ:
```json
{"type":"CHAT","content":"Hey @bob kiểm tra cái này!"}
```

### Bước 5: Verify

```http
### Kiểm tra notification-service đã nhận event
GET http://localhost:8088/api/notifications?userId=bob&unreadOnly=true
Accept: application/json

###

### Kiểm tra log-service cũng nhận
GET http://localhost:8084/api/logs/history?page=0&size=10
Accept: application/json
```

---

## 6. Chạy Unit Test

### 6.1. Trong IntelliJ

- Chuột phải folder `src/test/java` của module → **Run 'All Tests'**
- Hoặc chuột phải 1 file test → **Run 'XxxTest'**
- Hoặc click icon ▶ bên cạnh tên method test

### 6.2. Từ Terminal

```bash
# Test 1 module
./mvnw -pl notification-service test

# Test 1 class
./mvnw -pl notification-service test -Dtest=NotificationServiceTest

# Test 1 method
./mvnw -pl notification-service test -Dtest=NotificationServiceTest#testMentionParsing

# Test cả 2 module của bạn
./mvnw -pl notification-service,file-service test

# Test toàn bộ project
./mvnw clean verify
```

---

## 7. Bảng Tổng Hợp Test Cases

### Notification Service (port 8088)

| # | Test case | Input | Expected |
|---|-----------|-------|----------|
| N-01 | Health check | `GET /actuator/health` | `{"status":"UP"}` |
| N-02 | Notifications rỗng | `GET /api/notifications?userId=nguyen` | `[]` |
| N-03 | Unread count rỗng | `GET /api/notifications/unread-count?userId=nguyen` | `{}` |
| N-04 | Ack channel | `POST /api/channels/1/ack` | `200 OK` |
| N-05 | RabbitMQ — tin nhắn @mention | Publish `LogEntry` vào `chat.exchange` | Tạo notification cho user |
| N-06 | RabbitMQ — @everyone | Publish `LogEntry` có `@everyone` | Tạo notification cho tất cả |
| N-07 | RabbitMQ — DM | Publish `LogEntry` eventType=PRIVATE | Tạo notification cho receiver |
| N-08 | Mark as read | `PUT /api/notifications/1/read` | `200 OK` |
| N-09 | Unread count > 0 | `GET /api/notifications/unread-count` | Count > 0 |

### File Service (port 8089)

| # | Test case | Input | Expected |
|---|-----------|-------|----------|
| F-01 | Health check | `GET /actuator/health` | `{"status":"UP"}` |
| F-02 | Upload ảnh PNG | `POST /api/files/upload` + PNG file | `FileMetadataDTO` + `thumbnailUrl` |
| F-03 | Upload text file | `POST /api/files/upload` + TXT file | `FileMetadataDTO`, `thumbnailUrl=null` |
| F-04 | Upload > 10MB | `POST /api/files/upload` + 11MB file | `400` hoặc `413` |
| F-05 | Xem info file | `GET /api/files/1/info` | `FileMetadataDTO` |
| F-06 | Download file | `GET /api/files/1` | Binary stream |
| F-07 | Download thumbnail | `GET /api/files/1/thumbnail` | JPEG 200×200 |
| F-08 | File không tồn tại | `GET /api/files/9999` | `404` |
| F-09 | Xóa file (owner) | `DELETE /api/files/1?userId=nguyen` | `200 OK` |
| F-10 | Xóa file (người khác) | `DELETE /api/files/2?userId=hacker` | `403` |
| F-11 | Verify MinIO | Mở MinIO Console | Thấy file + thumbnail |

---

## 8. Lưu Ý Kỹ Thuật Quan Trọng

### MessageDTO hiện tại KHÔNG có channelId

```java
// common-lib/src/.../dto/MessageDTO.java — 5 fields
private MessageType type;
private String sender;
private String receiver;
private String content;
private LocalDateTime timestamp;
// ⚠ KHÔNG CÓ channelId, serverId
```

**Messaging-service publish `LogEntry` (không phải `MessageDTO`)** khi gửi vào RabbitMQ:

```java
// messaging-service/src/.../service/MessageService.java dòng 58-60
public void publishLogEvent(MessageDTO msg) {
    LogEntry log = new LogEntry(msg.getTimestamp(), msg.getType().name(),
                                msg.getSender(), msg.getReceiver(), msg.getContent());
    rabbitTemplate.convertAndSend("chat.exchange", "log." + msg.getType().name().toLowerCase(), log);
}
```

→ **Notification service sẽ nhận `LogEntry`**, routing key `log.chat`, `log.private`, v.v.

### RabbitMQ routing hiện tại

| Producer | Exchange | Routing key | Payload class |
|----------|----------|-------------|---------------|
| messaging-service | `chat.exchange` | `log.chat`, `log.private`, `log.join`, `log.leave` | `LogEntry` |

| Consumer | Queue | Binding pattern | Nhận |
|----------|-------|-----------------|------|
| log-service | `chat.log.queue` | `log.#` | Mọi event `log.*` |
| **notification-service** (bạn tạo) | `chat.notification.queue` | `log.#` hoặc `notify.#` | Tùy thiết kế |

---

## 9. Dọn Dẹp Sau Test

```bash
# Dừng và xóa container test
docker stop test-rabbitmq test-minio
docker rm test-rabbitmq test-minio

# Xóa file test tạm (nếu có)
rm -f tnguyen/test/test-image.png tnguyen/test/test-file.txt tnguyen/test/downloaded-*.*
```

# 🔔📁 Implementation Plan: Notification Service + File Service

> **Người thực hiện:** Bạn (Làm mới 100%)
> **Ngày:** 2026-05-07
> **Tham chiếu:** `doc/01_yeu_cau_phan_mem.md` § G (N1–N5) + § H (F1–F4)

---

## Tổng Quan

| Module | Port | Chức năng | DB | Ghi chú |
|--------|------|-----------|----|---------|
| `notification-service` | **8088** | Unread badge, @mention, push notification | MySQL | Consumer RabbitMQ |
| `file-service` | **8089** | Upload/download file, thumbnail ảnh | MySQL + MinIO | S3-compatible storage |

**Tổng:** 9 chức năng (N1–N5 + F1–F4), ~30 files Java, 2 Dockerfiles.

---

# PHẦN 1: NOTIFICATION SERVICE (port 8088)

## 1.1. Chức Năng Yêu Cầu

| ID | Chức năng | Loại | Mô tả |
|----|-----------|------|-------|
| N1 | Mention @user | RabbitMQ consumer | Parse tin nhắn → phát hiện `@username` → tạo notification |
| N2 | Mention @everyone | RabbitMQ consumer | Parse `@everyone` → tạo notification cho tất cả members |
| N3 | Unread count | Logic + REST API | Đếm tin nhắn chưa đọc theo channel/DM cho mỗi user |
| N4 | Đánh dấu đã đọc | REST API | `POST /api/channels/{channelId}/ack` — cập nhật last_read |
| N5 | Push notification | WebSocket push | Khi có tin nhắn mới ở channel không active → đẩy qua WS |

## 1.2. Kiến Trúc

```
messaging-service ──publish──► RabbitMQ ──consume──► notification-service
                                                          │
                                                    ┌─────▼──────┐
                                                    │   MySQL    │
                                                    │ - notifications   │
                                                    │ - read_status     │
                                                    └────────────┘
                                                          │
                                              WebSocket push / REST API
                                                          │
                                                       Client
```

**RabbitMQ binding:**
- Exchange: `chat.exchange` (Topic, durable) — dùng chung với log-service
- Queue: `chat.notification.queue` (durable)
- Routing key pattern: `notify.#`
- Routing keys: `notify.message`, `notify.mention`, `notify.dm`

> **Tham khảo:** Copy pattern từ `log-service/src/.../config/RabbitMQConfig.java` — chỉ đổi queue name + routing key.

## 1.3. Database Schema

```sql
-- notification-service dùng DB: chatserver_notifications

-- Bảng thông báo
CREATE TABLE notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      VARCHAR(100) NOT NULL,       -- người nhận notification
    channel_id   BIGINT,                      -- channel nguồn (nullable cho DM)
    server_id    BIGINT,                      -- server nguồn (nullable cho DM)
    sender       VARCHAR(100),                -- người gửi tin nhắn gốc
    type         ENUM('MENTION','MENTION_ALL','MESSAGE','DM') NOT NULL,
    content      VARCHAR(500),                -- preview nội dung tin nhắn
    is_read      BOOLEAN DEFAULT FALSE,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_unread (user_id, is_read),
    INDEX idx_channel (channel_id)
);

-- Bảng theo dõi đọc tin (cho unread count)
CREATE TABLE read_status (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         VARCHAR(100) NOT NULL,
    channel_id      BIGINT NOT NULL,
    last_read_msg_id BIGINT DEFAULT 0,        -- ID tin nhắn cuối cùng đã đọc
    last_read_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel (user_id, channel_id)
);
```

## 1.4. REST API

| # | Method | Endpoint | Request | Response | Mô tả |
|---|--------|----------|---------|----------|-------|
| 1 | `GET` | `/api/notifications` | `?userId={uid}&unreadOnly=true` | `List<NotificationDTO>` | Lấy danh sách notification |
| 2 | `POST` | `/api/channels/{channelId}/ack` | `{ "userId": "...", "lastMessageId": 123 }` | `200 OK` | Đánh dấu đã đọc |
| 3 | `GET` | `/api/notifications/unread-count` | `?userId={uid}` | `Map<channelId, count>` | Số tin chưa đọc mỗi channel |
| 4 | `PUT` | `/api/notifications/{id}/read` | — | `200 OK` | Đánh dấu 1 notification đã đọc |
| 5 | `GET` | `/actuator/health` | — | `{"status":"UP"}` | Health check |

## 1.5. Cấu Trúc File

```
notification-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/chatsever/notification/
    │   │   ├── NotificationApplication.java
    │   │   ├── config/
    │   │   │   └── RabbitMQConfig.java          # Queue + binding setup
    │   │   ├── controller/
    │   │   │   └── NotificationController.java  # 4 REST endpoints
    │   │   ├── dto/
    │   │   │   ├── NotificationDTO.java
    │   │   │   ├── AckRequest.java
    │   │   │   └── UnreadCountResponse.java
    │   │   ├── listener/
    │   │   │   └── MessageEventListener.java    # @RabbitListener — core logic
    │   │   ├── model/
    │   │   │   ├── Notification.java             # JPA Entity
    │   │   │   ├── ReadStatus.java               # JPA Entity
    │   │   │   └── NotificationType.java         # Enum
    │   │   ├── repository/
    │   │   │   ├── NotificationRepository.java
    │   │   │   └── ReadStatusRepository.java
    │   │   └── service/
    │   │       └── NotificationService.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-docker.yml
    └── test/
        └── java/com/chatsever/notification/
            ├── service/NotificationServiceTest.java
            └── listener/MessageEventListenerTest.java
```

**Tổng: ~15 files**

## 1.6. Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Shared DTOs -->
    <dependency>
        <groupId>com.chatsever</groupId>
        <artifactId>common-lib</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Web + REST -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- RabbitMQ consumer -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- MySQL + JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- H2 cho dev local -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Health check -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Jackson datetime -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 1.7. Logic Quan Trọng

### MessageEventListener — Core consumer

```java
@Component
public class MessageEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onMessage(MessageDTO message) {
        // 1. Parse nội dung → tìm @username hoặc @everyone
        List<String> mentions = parseMentions(message.getContent());

        // 2. Nếu có @everyone → tạo notification cho tất cả members trong channel
        if (mentions.contains("everyone")) {
            notifyAllMembers(message);
        }

        // 3. Nếu có @user cụ thể → tạo notification cho từng user
        for (String user : mentions) {
            if (!"everyone".equals(user)) {
                createNotification(user, NotificationType.MENTION, message);
            }
        }

        // 4. Cập nhật unread count cho tất cả user trong channel
        //    (trừ người gửi)
        incrementUnreadForChannel(message.getChannelId(), message.getSender());
    }

    private List<String> parseMentions(String content) {
        // Regex: @(\w+) → extract username
        Pattern pattern = Pattern.compile("@(\\w+)");
        // ...
    }
}
```

### Unread Count Logic

```java
// Khi user mở channel → gọi POST /api/channels/{id}/ack
// → cập nhật last_read_msg_id trong read_status
// → reset unread count cho channel đó

// Khi tính unread count:
// unread = totalMessages(channelId) WHERE id > last_read_msg_id
// Hoặc đơn giản hơn: dùng counter trong read_status table
```

---

# PHẦN 2: FILE SERVICE (port 8089)

## 2.1. Chức Năng Yêu Cầu

| ID | Chức năng | Loại | Mô tả |
|----|-----------|------|-------|
| F1 | Upload file | REST API | `POST /api/files/upload` — max 10MB |
| F2 | Download file | REST API | `GET /api/files/{fileId}` |
| F3 | Preview ảnh | Tự động | Gen thumbnail khi upload ảnh (JPEG, PNG, GIF) |
| F4 | Multi-format | Logic | Hỗ trợ: ảnh, video nhỏ, PDF, document, code snippet |

## 2.2. Kiến Trúc

```
Client ──upload──► file-service ──store──► MinIO (S3-compatible)
                       │
                 ┌─────▼──────┐
                 │   MySQL    │    (metadata: filename, size, type, URL)
                 └────────────┘
                       │
                  gen thumbnail
                       │
                 ┌─────▼──────┐
                 │   MinIO    │    (thumbnail bucket riêng hoặc prefix)
                 └────────────┘
```

**MinIO buckets:**
- `chat-files` — file đính kèm tin nhắn
- `chat-avatars` — ảnh đại diện (dùng chung cho user-profile-service)
- `chat-thumbnails` — thumbnail tự động generate

## 2.3. Database Schema

```sql
-- file-service dùng DB: chatserver_files

CREATE TABLE file_metadata (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,        -- tên file gốc
    stored_name   VARCHAR(255) NOT NULL UNIQUE,  -- UUID-based name trên MinIO
    content_type  VARCHAR(100) NOT NULL,         -- MIME type: image/jpeg, application/pdf
    file_size     BIGINT NOT NULL,               -- bytes
    bucket        VARCHAR(100) NOT NULL DEFAULT 'chat-files',
    thumbnail_key VARCHAR(255),                  -- key thumbnail trên MinIO (null nếu không phải ảnh)
    uploader      VARCHAR(100) NOT NULL,          -- username người upload
    channel_id    BIGINT,                         -- channel liên quan (nullable)
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader (uploader),
    INDEX idx_channel (channel_id)
);
```

## 2.4. REST API

| # | Method | Endpoint | Request | Response | Mô tả |
|---|--------|----------|---------|----------|-------|
| 1 | `POST` | `/api/files/upload` | `multipart/form-data` (file + userId + channelId) | `FileMetadataDTO` (id, url, thumbnailUrl) | Upload file (max 10MB) |
| 2 | `GET` | `/api/files/{fileId}` | — | Binary stream | Download file gốc |
| 3 | `GET` | `/api/files/{fileId}/thumbnail` | — | Binary stream (image) | Download thumbnail |
| 4 | `GET` | `/api/files/{fileId}/info` | — | `FileMetadataDTO` | Thông tin file (size, type, name) |
| 5 | `DELETE` | `/api/files/{fileId}` | `?userId={uid}` | `200 OK` | Xóa file (chỉ owner) |
| 6 | `GET` | `/actuator/health` | — | `{"status":"UP"}` | Health check |

## 2.5. Cấu Trúc File

```
file-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/chatsever/file/
    │   │   ├── FileApplication.java
    │   │   ├── config/
    │   │   │   └── MinioConfig.java              # MinIO client bean
    │   │   ├── controller/
    │   │   │   └── FileController.java           # 5 REST endpoints
    │   │   ├── dto/
    │   │   │   └── FileMetadataDTO.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── FileNotFoundException.java
    │   │   │   └── FileTooLargeException.java
    │   │   ├── model/
    │   │   │   └── FileMetadata.java              # JPA Entity
    │   │   ├── repository/
    │   │   │   └── FileMetadataRepository.java
    │   │   └── service/
    │   │       ├── FileStorageService.java        # MinIO upload/download
    │   │       └── ThumbnailService.java          # Thumbnail generation
    │   └── resources/
    │       ├── application.yml
    │       └── application-docker.yml
    └── test/
        └── java/com/chatsever/file/
            ├── service/FileStorageServiceTest.java
            └── service/ThumbnailServiceTest.java
```

**Tổng: ~15 files**

## 2.6. Dependencies (pom.xml) — Khác biệt so với notification

```xml
<dependencies>
    <!-- (giống notification: common-lib, web, actuator, jpa, mysql, h2, jackson, test) -->

    <!-- MinIO S3 SDK -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>8.5.7</version>
    </dependency>

    <!-- Thumbnail generation (Java built-in java.awt.image đủ dùng,
         hoặc dùng Thumbnailator cho tiện hơn) -->
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.20</version>
    </dependency>
</dependencies>
```

## 2.7. Logic Quan Trọng

### MinioConfig — S3 client

```java
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### FileStorageService — Upload flow

```java
public FileMetadataDTO upload(MultipartFile file, String userId, Long channelId) {
    // 1. Validate: size <= 10MB, content type hợp lệ
    validateFile(file);

    // 2. Generate unique stored name: UUID + extension
    String storedName = UUID.randomUUID() + getExtension(file.getOriginalFilename());

    // 3. Upload lên MinIO bucket "chat-files"
    minioClient.putObject(PutObjectArgs.builder()
            .bucket("chat-files")
            .object(storedName)
            .stream(file.getInputStream(), file.getSize(), -1)
            .contentType(file.getContentType())
            .build());

    // 4. Nếu là ảnh → generate thumbnail
    String thumbnailKey = null;
    if (isImage(file.getContentType())) {
        thumbnailKey = thumbnailService.generateAndUpload(file, storedName);
    }

    // 5. Lưu metadata vào MySQL
    FileMetadata metadata = new FileMetadata(...);
    repository.save(metadata);

    return toDTO(metadata);
}
```

### ThumbnailService — Auto gen

```java
@Service
public class ThumbnailService {

    private static final int THUMB_WIDTH = 200;
    private static final int THUMB_HEIGHT = 200;

    public String generateAndUpload(MultipartFile original, String storedName) {
        // 1. Dùng Thumbnailator resize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(original.getInputStream())
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .outputFormat("jpg")
                .toOutputStream(baos);

        // 2. Upload thumbnail lên MinIO bucket "chat-thumbnails"
        String thumbKey = "thumb_" + storedName.replace(getExt(storedName), ".jpg");
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("chat-thumbnails")
                .object(thumbKey)
                .stream(new ByteArrayInputStream(baos.toByteArray()), baos.size(), -1)
                .contentType("image/jpeg")
                .build());

        return thumbKey;
    }
}
```

### Allowed file types

```java
private static final Set<String> ALLOWED_TYPES = Set.of(
    // Ảnh
    "image/jpeg", "image/png", "image/gif", "image/webp",
    // Video nhỏ
    "video/mp4", "video/webm",
    // Documents
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    // Code / Text
    "text/plain", "application/json", "text/html", "text/css",
    "application/javascript", "text/x-java-source"
);

private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
```

---

# PHẦN 3: CHECKLIST TRIỂN KHAI

## Phase 1 — Scaffolding (Ước: 1–2 giờ)

- [ ] **1.1** Tạo `notification-service/pom.xml` (copy từ log-service, thêm JPA + MySQL)
- [ ] **1.2** Tạo `notification-service/src/.../NotificationApplication.java`
- [ ] **1.3** Tạo `notification-service/src/main/resources/application.yml`
- [ ] **1.4** Tạo `notification-service/Dockerfile` (copy template từ log-service)
- [ ] **1.5** Tạo `file-service/pom.xml` (thêm MinIO + Thumbnailator)
- [ ] **1.6** Tạo `file-service/src/.../FileApplication.java`
- [ ] **1.7** Tạo `file-service/src/main/resources/application.yml`
- [ ] **1.8** Tạo `file-service/Dockerfile`
- [ ] **1.9** Thêm 2 modules vào root `pom.xml` → `<modules>`
- [ ] **1.10** `./mvnw clean install -DskipTests` — verify build sạch

## Phase 2 — Notification Service Core (Ước: 3–4 giờ)

- [ ] **2.1** Tạo `RabbitMQConfig.java` — queue `chat.notification.queue`, routing `notify.#`
- [ ] **2.2** Tạo JPA entities: `Notification.java`, `ReadStatus.java`
- [ ] **2.3** Tạo repositories: `NotificationRepository`, `ReadStatusRepository`
- [ ] **2.4** Tạo `NotificationService.java` — logic tạo notification, đếm unread, ack
- [ ] **2.5** Tạo `MessageEventListener.java` — `@RabbitListener`, parse @mention
- [ ] **2.6** Tạo DTOs: `NotificationDTO`, `AckRequest`, `UnreadCountResponse`
- [ ] **2.7** Tạo `NotificationController.java` — 4 REST endpoints
- [ ] **2.8** Viết unit test cho `NotificationService`
- [ ] **2.9** Viết unit test cho `MessageEventListener`
- [ ] **2.10** Test local: `./mvnw -pl notification-service spring-boot:run`

## Phase 3 — File Service Core (Ước: 4–5 giờ)

- [ ] **3.1** Tạo `MinioConfig.java` — MinioClient bean
- [ ] **3.2** Tạo JPA entity: `FileMetadata.java`
- [ ] **3.3** Tạo `FileMetadataRepository.java`
- [ ] **3.4** Tạo `FileStorageService.java` — upload/download MinIO
- [ ] **3.5** Tạo `ThumbnailService.java` — gen thumbnail ảnh
- [ ] **3.6** Tạo `FileMetadataDTO.java`
- [ ] **3.7** Tạo exceptions: `FileNotFoundException`, `FileTooLargeException`, `GlobalExceptionHandler`
- [ ] **3.8** Tạo `FileController.java` — 5 REST endpoints
- [ ] **3.9** Viết unit test cho `FileStorageService`
- [ ] **3.10** Viết unit test cho `ThumbnailService`
- [ ] **3.11** Test local với MinIO: `docker run -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"`

## Phase 4 — Docker Integration (Ước: 1–2 giờ)

- [ ] **4.1** Cập nhật `docker-compose.yml` — thêm 2 service containers
- [ ] **4.2** Cập nhật `infra/mysql/init.sql` — thêm DB `chatserver_notifications`, `chatserver_files`
- [ ] **4.3** `docker compose up -d --build` — verify cả 2 service khởi động
- [ ] **4.4** Smoke test: `curl localhost:8088/actuator/health` + `curl localhost:8089/actuator/health`
- [ ] **4.5** Test upload file thật qua curl:
  ```bash
  curl -X POST http://localhost:8089/api/files/upload \
    -F "file=@test.png" \
    -F "userId=nguyen" \
    -F "channelId=1"
  ```
- [ ] **4.6** Test notification qua RabbitMQ Management UI (publish message vào `chat.exchange` với routing key `notify.message`)

## Phase 5 — Gateway + Polish (Ước: 30 phút)

- [ ] **5.1** Thêm 2 routes vào `gateway-service/application.yml`:
  ```yaml
  - id: notification-service
    uri: http://notification-service:8088
    predicates:
      - Path=/api/notifications/**,/api/channels/*/ack
  - id: file-service
    uri: http://file-service:8089
    predicates:
      - Path=/api/files/**
  ```
- [ ] **5.2** Test qua gateway: `curl localhost:8080/api/files/upload`

---

## Tổng Effort Ước Tính

| Phase | Thời gian | Mô tả |
|-------|-----------|-------|
| Phase 1 | 1–2 giờ | Scaffolding 2 modules |
| Phase 2 | 3–4 giờ | Notification service CRUD + RabbitMQ |
| Phase 3 | 4–5 giờ | File service + MinIO + Thumbnail |
| Phase 4 | 1–2 giờ | Docker integration |
| Phase 5 | 30 phút | Gateway routes |
| **Tổng** | **~10–14 giờ** | |

---

## application.yml mẫu

### notification-service

```yaml
server:
  port: 8088

spring:
  application:
    name: notification-service
  # Dev profile: H2
  datasource:
    url: jdbc:h2:mem:notificationdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: 5672
    username: guest
    password: guest
  jackson:
    serialization:
      write-dates-as-timestamps: false

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### file-service

```yaml
server:
  port: 8089

spring:
  application:
    name: file-service
  datasource:
    url: jdbc:h2:mem:filedb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
  jackson:
    serialization:
      write-dates-as-timestamps: false

# MinIO config
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  buckets:
    files: chat-files
    thumbnails: chat-thumbnails

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### application-docker.yml (cả 2 service)

```yaml
# Override cho môi trường Docker
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:mysql}:3306/${MYSQL_DATABASE}
    username: ${MYSQL_USER:chatapp}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  h2:
    console:
      enabled: false
```

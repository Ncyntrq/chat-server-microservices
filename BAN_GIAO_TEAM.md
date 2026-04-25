# Bàn Giao Người A → Team B / C / D / E

> **Người A đã hoàn thành:** `common-lib` + `log-service`.
> **Team có thể bắt đầu Phase 2 song song.**
> Cập nhật: 2026-04-25 · Repo: https://github.com/Ncyntrq/chat-server-microservices

---

## 1. Tổng quan đã hoàn thành

### 1.1. Module hoàn thành

| Module | Trạng thái | Test pass | Vị trí |
|--------|------------|-----------|--------|
| `common-lib` | ✅ Build + `mvn install` | 6 / 6 | `common-lib/` — JAR ở `~/.m2/repository/com/chatsever/common-lib/1.0.0-SNAPSHOT/` |
| `log-service` | ✅ Build + chạy thật được | 6 / 6 | `log-service/` — port `8084` |

Tổng: **12/12 unit test pass**, build sạch, cả 2 JAR đã `install` vào local Maven repo.

### 1.2. Hạ tầng chung đã setup sẵn

- **Maven Wrapper** (`./mvnw`) ở root — không ai cần cài Maven hệ thống. Spring Boot 3.5.14, Java 17.
- **Parent POM** (root `pom.xml`) khai báo Spring Boot parent, Java 17, encoding UTF-8.
- **`.gitignore`** đã loại trừ `target/`, `logs/`, IDE files — clone về build sẽ không kéo theo rác.

---

## 2. Quy trình clone + build (cho team)

```bash
# 1. Clone
git clone https://github.com/Ncyntrq/chat-server-microservices.git
cd chat-server-microservices

# 2. Build toàn bộ (lần đầu wrapper sẽ tự download Apache Maven 3.9.14 ~10 MB)
./mvnw clean install

# 3. Verify common-lib JAR đã có trong local repo
ls ~/.m2/repository/com/chatsever/common-lib/1.0.0-SNAPSHOT/
# → common-lib-1.0.0-SNAPSHOT.jar
```

> **Trên Windows** dùng `mvnw.cmd` thay cho `./mvnw`.

Sau bước 3, mỗi người có thể bắt đầu code module của mình.

---

## 3. API `common-lib` — dùng như thế nào

### 3.1. Khai báo dependency trong POM của module bạn

```xml
<dependency>
    <groupId>com.chatsever</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3.2. Các class công khai

#### `com.chatsever.common.enums.MessageType`
Enum 9 giá trị: `CHAT`, `PRIVATE`, `SYSTEM`, `ERROR`, `LIST`, `JOIN`, `LEAVE`, `PING`, `PONG`.

#### `com.chatsever.common.dto.MessageDTO`
POJO 5 field: `type` (`MessageType`), `sender`, `receiver`, `content`, `timestamp` (`LocalDateTime`).

- Có `@JsonInclude(NON_NULL)` → field `null` (vd `receiver` cho broadcast) tự bị bỏ qua khi serialize.
- Timestamp format: `yyyy-MM-dd'T'HH:mm:ss`.
- **KHÔNG có field `token`** — đừng thêm. JWT chỉ truyền qua query param khi WS handshake.

```java
MessageDTO m = new MessageDTO(
    MessageType.CHAT, "nguyen", null, "Hello", LocalDateTime.now());
String json = objectMapper.writeValueAsString(m);
```

#### `com.chatsever.common.dto.LogEntry`
POJO 5 field cho RabbitMQ event: `timestamp`, `eventType`, `sender`, `receiver`, `content`.

- `eventType` là `String`: `"BROADCAST"`, `"PRIVATE"`, `"USER_LOGIN"`, `"USER_LOGOUT"`, `"USER_REGISTER"`.
- **Người C** publish class này; **Người A** đã consume class này → bắt buộc dùng chung 1 class trong `common-lib`, không tự copy ở 2 service.

```java
// Người C publish:
LogEntry e = new LogEntry(LocalDateTime.now(), "BROADCAST", "nguyen", null, "Hello");
rabbitTemplate.convertAndSend("chat.exchange", "log.chat", e);
```

#### `com.chatsever.common.util.SecurityUtil`
Static methods bọc `BCryptPasswordEncoder`:

- `SecurityUtil.hashPassword(String raw)` → BCrypt hash (chuỗi bắt đầu bằng `$2`).
- `SecurityUtil.checkPassword(String raw, String hashed)` → `boolean`.

```java
// Người B trong UserService:
String hash = SecurityUtil.hashPassword(req.getPassword());
user.setPasswordHash(hash);

if (!SecurityUtil.checkPassword(req.getPassword(), user.getPasswordHash())) {
    throw new UnauthorizedException("Sai username hoặc password");
}
```

---

## 4. Hợp đồng RabbitMQ (cho Người C — `messaging-service`)

`log-service` đã setup consumer sẵn. Người C **chỉ cần publish**:

| Hằng số | Giá trị |
|---------|---------|
| Exchange | `chat.exchange` (Topic, durable) |
| Queue (log-service) | `chat.log.queue` (durable) — auto-declare khi log-service start |
| Binding pattern | `log.#` |
| Routing key gợi ý | `log.chat`, `log.private`, `log.login`, `log.logout`, `log.register` |

Producer cũng phải dùng `Jackson2JsonMessageConverter` + `JavaTimeModule` để `LocalDateTime` serialize đúng — xem
`log-service/src/main/java/com/chatsever/log/config/RabbitMQConfig.java` làm tham khảo.

---

## 5. REST endpoint đã sẵn (cho Người D — `gateway-service`)

`log-service` expose:

```
GET http://localhost:8084/api/logs/history?page=0&size=50&eventType=BROADCAST
GET http://localhost:8084/actuator/health
```

Response của `/api/logs/history`:

```json
{
  "content": [
    { "timestamp": "...", "eventType": "...", "sender": "...", "receiver": null, "content": "..." }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 0,
  "totalPages": 0
}
```

Người D thêm route trong `gateway-service/application.yml`:

```yaml
- id: log-service
  uri: http://localhost:8084
  predicates:
    - Path=/api/logs/**
```

---

## 6. Test thử log-service đang chạy thật

```bash
# B1: Khởi động RabbitMQ (BẮT BUỘC trước khi start log-service)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# B2: Chạy log-service
./mvnw -pl log-service spring-boot:run

# B3 (terminal khác): Test endpoint
curl http://localhost:8084/api/logs/history
# → {"content":[],"page":0,"size":50,"totalElements":0,"totalPages":0}

curl http://localhost:8084/actuator/health
# → {"status":"UP"}

# B4: Test consumer thật bằng Management UI
#   http://localhost:15672  (guest/guest) → Exchanges → chat.exchange → Publish message
#   Routing key: log.chat
#   Content type: application/json
#   Payload:
#     {"timestamp":"2026-04-25T12:00:00","eventType":"BROADCAST","sender":"test","receiver":null,"content":"Hello"}
#
# Kiểm tra: cat log-service/logs/chat_log.txt → phải có 1 dòng JSON
```

---

## 7. Chạy bằng Docker (khuyến nghị cho team)

Người A đã setup sẵn `Dockerfile` cho log-service và `docker-compose.yml` (RabbitMQ + log-service). Team **không cần cài Java/Maven hệ thống**, chỉ cần Docker.

### 7.1. Yêu cầu

- Docker Engine ≥ 20.10 và Docker Compose v2 (`docker compose`, không phải `docker-compose` rời).
- Cổng `5672`, `15672`, `8084` không bị chiếm.

### 7.2. Lệnh chính

```bash
# Build image + start RabbitMQ và log-service
docker compose up -d --build

# Theo dõi log realtime
docker compose logs -f log-service

# Kiểm tra trạng thái
docker compose ps
#   chat-rabbitmq      ... healthy
#   chat-log-service   ... up

# Smoke test (sau khi up xong)
curl http://localhost:8084/actuator/health
# → {"status":"UP"}

curl http://localhost:8084/api/logs/history
# → {"content":[],"page":0,"size":50,"totalElements":0,"totalPages":0}

# Dừng (giữ data RabbitMQ)
docker compose stop

# Dừng + xoá container (data RabbitMQ vẫn còn vì dùng anonymous volume mặc định)
docker compose down

# Dừng + xoá sạch cả volume RabbitMQ
docker compose down -v
```

### 7.3. Cách hoạt động

- **`log-service/Dockerfile`** dùng multi-stage build: stage 1 build bằng `./mvnw -pl log-service -am package` (kéo theo `common-lib`), stage 2 chỉ lấy executable jar trên `eclipse-temurin:17-jre-jammy`. Build context phải là project root vì log-service phụ thuộc common-lib.
- **`docker-compose.yml`** override 4 biến môi trường để log-service kết nối RabbitMQ bằng hostname `rabbitmq` (Spring Boot tự map `SPRING_RABBITMQ_HOST` → `spring.rabbitmq.host`):

  ```yaml
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_RABBITMQ_PORT: 5672
  SPRING_RABBITMQ_USERNAME: guest
  SPRING_RABBITMQ_PASSWORD: guest
  ```

- File log thực tế được mount ra host: `./log-service/logs/chat_log.txt`. Đã thêm vào `.gitignore`, không lo commit nhầm.
- `depends_on: condition: service_healthy` đảm bảo log-service chỉ start sau khi RabbitMQ pass healthcheck → tránh lỗi connection refused khi cold start.

### 7.4. Test consumer thật trong môi trường Docker

```bash
# Mở Management UI: http://localhost:15672  (guest / guest)
# → Exchanges → chat.exchange → Publish message
#     Routing key: log.chat
#     Properties → content_type: application/json
#     Payload:
#       {"timestamp":"2026-04-25T12:00:00","eventType":"BROADCAST",
#        "sender":"test","receiver":null,"content":"Hello via docker"}

# Verify đã ghi log
cat ./log-service/logs/chat_log.txt
# → JSON line vừa publish

# Hoặc gọi API
curl 'http://localhost:8084/api/logs/history?eventType=BROADCAST'
```

### 7.5. Khi team thêm service mới (B/C/D/E)

Mỗi người tạo `Dockerfile` riêng cho module mình (copy template từ `log-service/Dockerfile`, đổi 2 chỗ: `-pl <module-name>` và artifactId trong `COPY --from=build`), sau đó thêm vào `docker-compose.yml` cùng cấp với `log-service`. Ví dụ skeleton cho auth-service:

```yaml
auth-service:
  build:
    context: .
    dockerfile: auth-service/Dockerfile
  container_name: chat-auth-service
  ports:
    - "8081:8081"
  networks:
    - chat-net
```

Service phụ thuộc nhau (vd messaging → auth/presence) thêm `depends_on:` tương tự log-service.

### 7.6. Cần override config riêng cho máy mình?

Tạo file `docker-compose.override.yml` ở root (đã được `.gitignore` ignore). Docker Compose tự merge khi `docker compose up`. Ví dụ override port nếu 8084 bị chiếm:

```yaml
services:
  log-service:
    ports:
      - "9084:8084"
```

---

## 8. Quy ước cho team

### 7.1. Cấu trúc Maven khi thêm module mới

Mọi module mới đặt cùng cấp với `common-lib` và `log-service`:

```
chat-server-microservices/
├── pom.xml                  ← Parent — thêm tên module vào <modules>
├── common-lib/              ← ✅ Người A
├── log-service/             ← ✅ Người A
├── auth-service/            ← ⬜ Người B
├── messaging-service/       ← ⬜ Người C
├── presence-service/        ← ⬜ Người D
├── gateway-service/         ← ⬜ Người D
└── client-app/              ← ⬜ Người E
```

POM mỗi module phải:

1. Có `<parent>` trỏ về `com.chatsever:chat-server:1.0.0-SNAPSHOT`.
2. Khai báo dependency `common-lib` (xem § 3.1).
3. Backend service: thêm `spring-boot-maven-plugin` để build executable JAR.

**Sau khi tạo POM module mới, NHỚ thêm tên module vào `<modules>` của parent `pom.xml`** — không thì reactor build sẽ bỏ qua.

### 7.2. Quy ước port (đã chốt — không đổi)

| Service | Port |
|---------|------|
| gateway-service | 8080 |
| auth-service | 8081 |
| messaging-service | 8082 |
| presence-service | 8083 |
| log-service | 8084 ← đang chạy |
| RabbitMQ | 5672 / 15672 |

Tham khảo `doc/02_kien_truc_he_thong.md` § 2.6.

### 7.3. Ràng buộc kỹ thuật (đừng vi phạm)

1. **Raw WebSocket — KHÔNG STOMP.** Người C dùng `@EnableWebSocket` + `TextWebSocketHandler`, không dùng `@EnableWebSocketMessageBroker`.
2. **JWT chỉ qua query param khi handshake** — extract từ URL, không đọc từ payload. `MessageDTO` cố ý không có field `token`.
3. **BCrypt — KHÔNG SHA-256.** Người B phải dùng `SecurityUtil` từ `common-lib`.
4. **`SecurityConfig` BẮT BUỘC trong auth-service** — phải `permitAll()` cho `/api/auth/**`, không Spring Security mặc định block hết.
5. **WS close codes:** `4001` = JWT invalid/expired khi handshake; `4002` = duplicate session.
6. **`LogEntry` chỉ tồn tại 1 nơi duy nhất là `common-lib`** — không copy class này sang messaging-service hay log-service, sẽ vỡ deserialization Jackson.

### 7.4. Test convention

- Test đặt cùng package với class production, ở `src/test/java/`.
- File test tên `<ClassName>Test.java`.
- Dùng `@MockitoBean` (Spring Boot 3.5+), KHÔNG dùng `@MockBean` (đã deprecated).
- Chạy 1 test class: `./mvnw -pl <module> test -Dtest=<TestClass>`.
- Chạy 1 method: `./mvnw -pl <module> test -Dtest=<TestClass>#<method>`.

---

## 9. File quan trọng cần biết

| File | Vai trò |
|------|---------|
| `pom.xml` (root) | Parent POM — thêm module mới vào đây |
| `mvnw` / `mvnw.cmd` | Maven Wrapper — chạy thay cho `mvn` |
| `.mvn/wrapper/maven-wrapper.properties` | Pin Maven 3.9.14 |
| `CLAUDE.md` | Hướng dẫn cho Claude Code khi làm việc trong repo này |
| `doc/03_thiet_ke_chi_tiet.md` | **Spec từng class/method — đọc TRƯỚC khi code** |
| `doc/04_giao_thuc_truyen_thong.md` | **Contract REST/WS/AMQP — không tự đổi format** |
| `doc/05_ke_hoach_trien_khai.md` § 5.4 | 20 test case nghiệm thu (T1–T20) |
| `doc/phan_cong_project.md` | Ai phụ trách module nào |

---

## 10. Liên hệ Người A khi cần

Người A nhận hỗ trợ những việc liên quan đến phần đã bàn giao:

- Format `MessageDTO` / `LogEntry` không hợp.
- BCrypt / `SecurityUtil` không match.
- Consumer RabbitMQ trong log-service không nhận event.
- Pagination `/api/logs/history` lỗi.

Mọi việc khác liên quan đến module của bạn → bạn tự xử lý hoặc trao đổi với người phụ trách module đó.

---

## 11. Trạng thái Integration / Docker

Phân công ban đầu (`doc/phan_cong_project.md`) chỉ định **Người D** là Integration Lead. Người A đã hoàn thành sớm và **đã làm trước phần Docker template** để team không bị block:

- ✅ `log-service/Dockerfile` — multi-stage build, executable jar, có thể dùng làm template copy cho 4 service backend còn lại (xem § 7.5).
- ✅ `docker-compose.yml` (root) — RabbitMQ + log-service đã chạy được. Người D / cả team chỉ cần thêm service block khi module mình sẵn sàng.

Người D vẫn giữ vai trò Integration Lead cho phần phức tạp hơn (cross-service debug, network giữa các container, healthcheck đầy đủ stack, troubleshoot khi tích hợp xong).

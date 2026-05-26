# DEVOPS_SUMMARY.md — Hướng Dẫn Triển Khai Toàn Diện
# Dự án: chat-server-microservices
# Phiên bản: 1.0.0 | Môi trường: Oracle Cloud ARM64 (24GB RAM, 4 Cores)

---

## 1. Tổng Quan Kiến Trúc (Architecture Overview)

### 1.1 Luồng CI/CD Tổng Thể

```
Developer Push Code
        │
        ▼
  GitHub / GitLab
        │  (Webhook HTTP POST)
        ▼
    Jenkins (Port 9090)
        │
        ├─► [Stage: Checkout]  ─── Lấy source code từ SCM
        │
        ├─► [Stage: Build]  ──────── mvn clean package -DskipTests
        │
        ├─► [Stage: Static Analysis] ── SonarQube (Port 9002)
        │
        ├─► [Stage: Quality Gate] ─── Chờ kết quả SonarQube
        │
        ├─► [Stage: Dockerization] ─── Build Docker images (ARM64)
        │
        ├─► [Stage: API Testing] ───── Newman chạy Postman collections
        │
        ├─► [Stage: Push Images] ───── Đẩy lên Docker Registry
        │
        └─► [Stage: Deploy] ─────────── docker compose up -d
                                                │
                              ┌─────────────────┼──────────────────┐
                              ▼                 ▼                  ▼
                      Microservices      Prometheus (9091)    Grafana (3001)
                      (chat-net)         scrape /actuator      Dashboard
```

### 1.2 Các Thành Phần Hạ Tầng

| Thành phần | Image Docker | Port Host | Mục đích |
|---|---|---|---|
| **Jenkins** | `jenkins/jenkins:lts-jdk17` | **9090** | CI/CD Orchestrator |
| **SonarQube** | `sonarqube:lts-community` | **9002** | Phân tích chất lượng code |
| **PostgreSQL** | `postgres:15-alpine` | Nội bộ | Database cho SonarQube |
| **Prometheus** | `prom/prometheus:latest` | **9091** | Thu thập metrics |
| **Grafana** | `grafana/grafana:latest` | **3001** | Hiển thị dashboard |

### 1.3 Mạng Docker

- **`devops-net`**: Mạng riêng cho các công cụ DevOps (Jenkins, SonarQube, Prometheus, Grafana).
- **`chat-net`**: Mạng cho các microservices ứng dụng.
- Prometheus và Jenkins được kết nối vào **cả hai** mạng để có thể scrape metrics từ microservices và deploy.

---

## 2. Kiến Thức Tiên Quyết (Prerequisite Knowledge)

### 2.1 Jenkins Plugins Bắt Buộc

Cài đặt tất cả các plugin sau trong Jenkins UI (`Manage Jenkins → Plugins → Available plugins`):

| Plugin | Mục đích |
|---|---|
| **Docker Pipeline** | Cho phép dùng lệnh `docker` trong Jenkinsfile |
| **Docker Commons** | Thư viện dùng chung cho Docker trong Jenkins |
| **SonarQube Scanner** | Tích hợp phân tích SonarQube vào pipeline |
| **NodeJS** | Cài đặt NodeJS để chạy `newman` (Postman CLI) |
| **Pipeline** | Hỗ trợ Declarative Pipeline (thường đã có sẵn) |
| **Git** | Tích hợp SCM với Jenkins |
| **Credentials Binding** | Inject secrets an toàn từ Jenkins Credentials Store |
| **HTML Publisher** | Publish báo cáo Newman dạng HTML |
| **Workspace Cleanup** | Dọn dẹp workspace sau mỗi build |
| **Timestamper** | Thêm timestamp vào console output |
| **Build Discarder** | Tự động xóa các build cũ |

### 2.2 Cách Prometheus Hoạt Động (Scraping)

- Prometheus sử dụng mô hình **pull**: nó tự chủ động gọi endpoint của từng service để lấy metrics, không phải service đẩy dữ liệu lên.
- Mỗi Spring Boot service phải expose endpoint `/actuator/prometheus` thông qua dependency `micrometer-registry-prometheus`.
- File `devops/prometheus/prometheus.yml` định nghĩa tần suất và địa chỉ các endpoint cần scrape.
- Prometheus và microservices phải cùng nằm trên một Docker network (`chat-net`) để Prometheus có thể resolve tên service.

### 2.3 Spring Boot Actuator Bắt Buộc

Mỗi microservice phải có trong `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

Và trong `application.yml` / `application.properties`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2.4 Multi-stage Dockerfile và ARM64

- **Stage 1 (build)**: Dùng `maven:3.9-eclipse-temurin-17-alpine` để compile và đóng gói JAR. Alpine giúp giảm kích thước image.
- **Stage 2 (runtime)**: Chỉ copy JAR vào `eclipse-temurin:17-jre-alpine`. Image runtime không chứa Maven, source code, giảm attack surface.
- **ARM64 Compatibility**: Tất cả images được chọn đều có bản `linux/arm64`. Khi build trên VPS ARM64, Docker tự chọn đúng platform.
- **JVM Flags quan trọng**:
  - `-XX:+UseContainerSupport`: JVM đọc memory limit từ cgroup của container (không dùng RAM toàn máy).
  - `-XX:MaxRAMPercentage=75.0`: Giới hạn JVM heap ở 75% RAM của container.
  - `-XX:+ExitOnOutOfMemoryError`: Tự tắt container nếu OOM để Docker restart.

---

## 3. Hướng Dẫn Cài Đặt Từng Bước (Step-by-Step Setup Guide)

### 3.1 Chuẩn Bị Môi Trường VPS (Oracle Cloud ARM64)

**Bước 1: Kết nối SSH vào VPS**

```bash
ssh -i "your-private-key.pem" ubuntu@<VPS_PUBLIC_IP>
```

**Bước 2: Cài đặt Docker và Docker Compose**

```bash
sudo apt-get update && sudo apt-get upgrade -y
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker

sudo apt-get install -y docker-compose-plugin
docker compose version
```

**Bước 3: Cấu hình kernel parameter cho SonarQube (Elasticsearch)**

SonarQube sử dụng Elasticsearch bên trong, yêu cầu `vm.max_map_count` đủ lớn:

```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

**Bước 4: Clone dự án lên VPS**

```bash
git clone https://github.com/<your-username>/chat-server-microservices.git
cd chat-server-microservices
```

**Bước 5: Tạo file `.env` từ template**

```bash
cp .env.example .env
nano .env
```

Điền các giá trị thực vào. **Không được dùng giá trị mặc định trong môi trường production.**

**Bước 6: Tạo Docker network `chat-net` trước**

Network `chat-net` được khai báo là `external: true` trong `docker-compose.devops.yml`, nghĩa là nó phải tồn tại trước khi chạy stack DevOps:

```bash
docker network create chat-net
```

### 3.2 Khởi Động Stack DevOps

```bash
docker compose -f docker-compose.devops.yml --env-file .env up -d
```

Kiểm tra trạng thái:

```bash
docker compose -f docker-compose.devops.yml ps
docker compose -f docker-compose.devops.yml logs -f jenkins
```

Chờ khoảng **2-3 phút** để Jenkins và SonarQube khởi động hoàn toàn.

**Lấy mật khẩu admin Jenkins lần đầu:**

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 3.3 Cấu Hình Jenkins (Giao Diện Web UI)

Truy cập: `http://<VPS_PUBLIC_IP>:9090`

#### Bước A: Cài Đặt Plugins

1. Vào `Manage Jenkins → Plugins → Available plugins`.
2. Tìm và tick chọn tất cả các plugin trong bảng mục 2.1.
3. Nhấn **"Download now and install after restart"**.
4. Tick ô **"Restart Jenkins when installation is complete"**.

#### Bước B: Cấu Hình Tool — Maven

1. Vào `Manage Jenkins → Tools`.
2. Cuộn đến **"Maven installations"**, nhấn **"Add Maven"**.
3. **Name**: `Maven-3.9` (phải khớp chính xác với trong Jenkinsfile).
4. Tick **"Install automatically"**, chọn version **3.9.x**.
5. Nhấn **"Save"**.

#### Bước C: Cấu Hình Tool — NodeJS

1. Trong trang **Tools**, cuộn đến **"NodeJS installations"**, nhấn **"Add NodeJS"**.
2. **Name**: `NodeJS-20`.
3. Tick **"Install automatically"**, chọn **NodeJS 20.x.x**.
4. Nhấn **"Save"**.

#### Bước D: Cấu Hình SonarQube Server trong Jenkins

1. Vào `Manage Jenkins → System`.
2. Cuộn xuống **"SonarQube servers"**, nhấn **"Add SonarQube"**.
3. Điền:
   - **Name**: `SonarQube` (phải khớp chính xác với `withSonarQubeEnv('SonarQube')` trong Jenkinsfile).
   - **Server URL**: `http://sonarqube:9000` (dùng tên container vì Jenkins và SonarQube cùng network `devops-net`).
   - **Server authentication token**: Chọn token đã tạo ở bước 3.5.
4. Nhấn **"Save"**.

#### Bước E: Tạo Pipeline Job

1. Từ Dashboard Jenkins, nhấn **"New Item"**.
2. Nhập tên: `chat-server-microservices-pipeline`.
3. Chọn loại **"Pipeline"**, nhấn **"OK"**.
4. Trong tab **"General"**:
   - Tick **"GitHub project"** (nếu dùng GitHub), nhập URL repo.
5. Trong tab **"Build Triggers"**:
   - Tick **"GitHub hook trigger for GITScm polling"** (để Webhook tự động trigger).
6. Trong tab **"Pipeline"**:
   - **Definition**: Chọn **"Pipeline script from SCM"** (⚠️ **QUAN TRỌNG** — đây là cách để Jenkins đọc Jenkinsfile từ repo).
   - **SCM**: Chọn **"Git"**.
   - **Repository URL**: `https://github.com/<your-username>/chat-server-microservices.git`
   - **Credentials**: Nhấn **"Add"** nếu repo là private, chọn credentials đã lưu.
   - **Branches to build**: `*/main` (hoặc `*/master` tùy branch chính của bạn).
   - **Script Path**: `Jenkinsfile` (Jenkins sẽ tìm file này ở root của repo).
7. Nhấn **"Save"**.

### 3.4 Cấu Hình Credentials trong Jenkins

Vào `Manage Jenkins → Credentials → System → Global credentials → Add Credentials`.

**Credential 1: Docker Hub**
- **Kind**: Username with password
- **Username**: Tên tài khoản Docker Hub
- **Password**: Docker Hub Access Token (tạo tại hub.docker.com/settings/security)
- **ID**: `dockerhub-credentials`

**Credential 2: File `.env`**
- **Kind**: Secret file
- **File**: Upload file `.env` thực (chứa giá trị thật)
- **ID**: `dotenv-file`

**Credential 3: SonarQube Token (xem bước 3.5)**
- **Kind**: Secret text
- **Secret**: Token từ SonarQube
- **ID**: `sonarqube-token`

### 3.5 Tạo Token SonarQube và Kết Nối Jenkins

Truy cập SonarQube: `http://<VPS_PUBLIC_IP>:9002`
- Username mặc định: `admin`
- Password mặc định: `admin` (hệ thống yêu cầu đổi ngay)

**Tạo Project trong SonarQube:**

1. Nhấn **"Create Project → Manually"**.
2. **Project key**: `chat-server-microservices` (phải khớp với `SONAR_PROJECT_KEY` trong Jenkinsfile).
3. **Display name**: `Chat Server Microservices`.
4. Nhấn **"Set Up"**.

**Tạo Token:**

1. Vào `My Account → Security → Generate Tokens`.
2. **Name**: `jenkins-token`.
3. **Type**: `Global Analysis Token`.
4. Nhấn **"Generate"**, copy token hiện ra (chỉ hiển thị 1 lần).
5. Quay lại Jenkins, vào `Manage Jenkins → Credentials`, tạo credential kiểu **Secret text** với ID `sonarqube-token` và paste token vào.
6. Trong `Manage Jenkins → System → SonarQube servers`, chọn credential `sonarqube-token` vừa tạo.

### 3.6 Cấu Hình Webhook (GitHub/GitLab → Jenkins)

**Mục đích:** Mỗi khi có `git push`, GitHub/GitLab tự động gửi HTTP POST đến Jenkins để kích hoạt pipeline.

**Trên GitHub:**

1. Vào repo → **Settings → Webhooks → Add webhook**.
2. **Payload URL**: `http://<VPS_PUBLIC_IP>:9090/github-webhook/`
   > ⚠️ URL phải kết thúc bằng dấu `/`
3. **Content type**: `application/json`.
4. **Which events**: Chọn **"Just the push event"**.
5. Nhấn **"Add webhook"**.

**Trên GitLab:**

1. Vào repo → **Settings → Webhooks**.
2. **URL**: `http://<VPS_PUBLIC_IP>:9090/gitlab/build_now`
3. Tick **"Push events"**.
4. Nhấn **"Add webhook"**.

> **Lưu ý bảo mật:** Nếu VPS có firewall (Oracle Cloud Security Lists), mở port 9090 cho IP của GitHub/GitLab Webhook server, không mở cho tất cả (`0.0.0.0/0`).

### 3.7 Khởi Động Microservices

Sau khi DevOps stack đã chạy, khởi động các microservices:

```bash
docker compose -f docker-compose.yml --env-file .env up -d
```

Kiểm tra health:

```bash
docker compose -f docker-compose.yml ps
curl http://localhost:8080/actuator/health
```

### 3.8 Import Dashboard Grafana

Truy cập Grafana: `http://<VPS_PUBLIC_IP>:3001`
- Username: giá trị `GRAFANA_ADMIN_USER` trong `.env`
- Password: giá trị `GRAFANA_ADMIN_PASSWORD` trong `.env`

**Datasource** đã được auto-provision, trỏ đến Prometheus tại `http://prometheus:9090`.

**Import Dashboard Spring Boot JVM:**

1. Vào **Dashboards → Import**.
2. Nhập ID dashboard: `**4701**` (Spring Boot 2.1 Statistics — hỗ trợ Micrometer).
   - Hoặc dùng ID `**11378**` (JVM Micrometer — chi tiết hơn).
3. Nhấn **"Load"**.
4. Trong dropdown **"Prometheus"**, chọn datasource **"Prometheus"** (đã auto-provision).
5. Nhấn **"Import"**.

---

## 4. Khắc Phục Sự Cố (Troubleshooting)

### 4.1 Lỗi Permission: Jenkins Không Thể Dùng Docker Socket

**Triệu chứng:**

```
Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock
```

**Nguyên nhân:** Jenkins container chạy với user `root` nhưng socket `/var/run/docker.sock` thuộc group `docker` trên host.

**Giải pháp:**

```bash
# Trên VPS host, lấy GID của group docker
getent group docker
# Ví dụ output: docker:x:998:ubuntu

# Vào container Jenkins và thêm user jenkins vào group có cùng GID
docker exec -u root jenkins groupadd -g 998 docker-host
docker exec -u root jenkins usermod -aG docker-host jenkins
docker restart jenkins
```

Cách thay thế đơn giản hơn (đã áp dụng trong `docker-compose.devops.yml` với `user: root`):
- Jenkins container đã chạy với quyền root, không cần bước trên. Tuy nhiên đây không phải best practice cho production.

### 4.2 Lỗi Out-of-Memory (OOM) trên ARM VPS 24GB

**Triệu chứng:** Container bị kill với mã `137` (OOM Killer), hoặc VPS bị chậm/treo.

**Nguyên nhân:** Chạy đồng thời quá nhiều JVM process (11 microservices + Jenkins + SonarQube mỗi cái cần 512MB+).

**Giải pháp A — Giới hạn RAM từng service trong `docker-compose.yml`:**

```yaml
services:
  auth-service:
    deploy:
      resources:
        limits:
          memory: 512m
        reservations:
          memory: 256m
    environment:
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

**Giải pháp B — Giới hạn RAM của SonarQube (đã có trong `docker-compose.devops.yml`):**

Các biến `SONAR_WEB_JAVAOPTS`, `SONAR_CE_JAVAOPTS`, `SONAR_SEARCH_JAVAOPTS` đã được thiết lập để giới hạn heap.

**Giải pháp C — Bật SWAP (khuyến nghị cho ARM VPS):**

```bash
sudo fallocate -l 8G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

**Giải pháp D — Không build tất cả service song song trong Jenkins:**

Trong Jenkinsfile, stage `Dockerization` build tuần tự (`.each`), không dùng `parallel`. Tránh OOM trong quá trình build.

### 4.3 Lỗi SonarQube Không Khởi Động Được

**Triệu chứng:** Container `sonarqube` liên tục restart, log hiện `max virtual memory areas vm.max_map_count [65530] is too low`.

**Giải pháp:**

```bash
sudo sysctl -w vm.max_map_count=262144
# Đảm bảo đã thêm vào /etc/sysctl.conf để bền vững sau reboot
grep vm.max_map_count /etc/sysctl.conf
```

### 4.4 Lỗi Webhook Không Trigger Jenkins

**Triệu chứng:** Push code lên GitHub nhưng Jenkins không tự chạy pipeline.

**Checklist:**

1. **Kiểm tra Jenkins URL có truy cập được từ internet không:**
   - Trên Oracle Cloud, vào **VPC → Security Lists → Ingress Rules**, thêm rule cho TCP port 9090 từ `0.0.0.0/0`.
   - Kiểm tra: `curl http://<VPS_PUBLIC_IP>:9090/github-webhook/` — phải trả về response (dù là 403/200).
2. **Kiểm tra cấu hình Webhook trên GitHub:**
   - Vào **Settings → Webhooks → Recent Deliveries**, xem response code.
   - `200` = OK, `301` = sai URL (thiếu `/`), `5xx` = Jenkins lỗi.
3. **Kiểm tra cài đặt trong Jenkins Pipeline Job:**
   - Tab **Build Triggers** phải tick **"GitHub hook trigger for GITScm polling"**.

### 4.5 Lỗi Prometheus Không Scrape Được Metrics

**Triệu chứng:** Trong Prometheus UI (`http://<VPS_IP>:9091/targets`), các target hiện màu đỏ `DOWN`.

**Checklist:**

1. **Microservices đã chạy chưa?**
   ```bash
   docker compose ps
   curl http://localhost:8081/actuator/health
   ```
2. **Microservices có expose `/actuator/prometheus` không?**
   - Kiểm tra dependency Micrometer và cấu hình `management.endpoints.web.exposure.include` trong `application.yml`.
3. **Prometheus có cùng network với microservices không?**
   - Kiểm tra `docker-compose.devops.yml`: Prometheus phải có `chat-net` trong danh sách networks.
4. **Reload cấu hình Prometheus không cần restart:**
   ```bash
   curl -X POST http://localhost:9091/-/reload
   ```

### 4.6 Lỗi Maven Build Thất Bại Trong Jenkins

**Triệu chứng:** Stage `Build` fail với lỗi không tải được dependencies.

**Giải pháp — Cấu hình Maven settings với mirror:**

1. Vào `Manage Jenkins → Managed files → Add a new Config → Maven settings.xml`.
2. Thêm mirror của Maven Central để tăng tốc và tránh timeout:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <mirrorOf>central</mirrorOf>
      <url>https://repo1.maven.org/maven2</url>
    </mirror>
  </mirrors>
</settings>
```

### 4.7 Lỗi `docker-compose.devops.yml` Không Tìm Thấy `chat-net`

**Triệu chứng:** Lỗi `network chat-net declared as external, but could not be found`.

**Giải pháp:**

```bash
docker network create chat-net
docker compose -f docker-compose.devops.yml up -d
```

---

## 5. Cấu Trúc Thư Mục DevOps

```
chat-server-microservices/
├── docker-compose.yml              # Stack microservices (hiện tại)
├── docker-compose.devops.yml       # Stack DevOps (Jenkins, SonarQube, Prometheus, Grafana)
├── Dockerfile.template             # Template Dockerfile tối ưu cho ARM64
├── Jenkinsfile                     # Declarative Pipeline CI/CD
├── .env.example                    # Template biến môi trường (commit lên git)
├── .env                            # Biến môi trường thực (KHÔNG commit lên git!)
├── .gitignore                      # Đảm bảo .env được ignore
└── devops/
    ├── prometheus/
    │   └── prometheus.yml          # Cấu hình scrape metrics
    └── grafana/
        └── provisioning/
            └── datasources/
                └── prometheus.yml  # Auto-provision Prometheus datasource
```

---

## 6. Checklist Bảo Mật (Security Checklist)

- [ ] File `.env` đã được thêm vào `.gitignore`
- [ ] Không có secret/password nào được hardcode trong Dockerfile hoặc `docker-compose.yml`
- [ ] Đã đổi mật khẩu mặc định của SonarQube (`admin/admin`)
- [ ] Đã đổi mật khẩu mặc định của Grafana
- [ ] Docker Hub Access Token dùng thay vì mật khẩu tài khoản
- [ ] Oracle Cloud Security Lists chỉ mở các port cần thiết (không mở wildcard)
- [ ] SonarQube token là `Global Analysis Token`, không phải User Token (phạm vi hẹp hơn)
- [ ] Jenkins chạy với `user: root` chỉ để đọc Docker socket — xem xét giải pháp bảo mật hơn trong production

---

## 7. Tham Khảo Nhanh — Các Lệnh Thường Dùng

```bash
# Khởi động stack DevOps
docker compose -f docker-compose.devops.yml --env-file .env up -d

# Tắt stack DevOps
docker compose -f docker-compose.devops.yml down

# Xem log Jenkins
docker logs -f jenkins

# Xem log SonarQube
docker logs -f sonarqube

# Restart một service cụ thể
docker compose -f docker-compose.devops.yml restart prometheus

# Kiểm tra health tất cả container
docker compose -f docker-compose.devops.yml ps

# Khởi động microservices
docker compose -f docker-compose.yml --env-file .env up -d

# Xem tất cả container đang chạy
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Dọn dẹp images không dùng (giải phóng disk)
docker image prune -af

# Reload Prometheus config không cần restart
curl -X POST http://localhost:9091/-/reload
```

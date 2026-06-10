# 13. Plan — Tìm kiếm tin nhắn & Tìm kiếm bạn bè

> **Ngày tạo:** 2026-06-09 · **Nhánh:** tnguyen-8/6
> **Phạm vi:** Bổ sung 2 chức năng tìm kiếm cho hệ thống chat microservices:
> 1. **Tìm kiếm tin nhắn** (message search) — chưa có backend.
> 2. **Tìm kiếm bạn bè / người dùng** (friend search) — backend đã có sẵn, chủ yếu thiếu UI client + làm giàu trạng thái bạn bè.
>
> Tuân thủ YAGNI / KISS / DRY. Không đổi schema khi không cần. Tái dùng tối đa code hiện có.

---

## A. Hiện trạng (đối chiếu code thực tế)

### Tìm kiếm tin nhắn
| Thành phần | Trạng thái |
|---|---|
| `ChatMessage` entity (`content` TEXT, `sender`, `receiver`, `channelId`, `serverId`, `timestamp`, `type`) | ✅ Có sẵn, đủ trường để search |
| `MessageRepository` | ⚠️ Chỉ có truy vấn theo channel / DM, **chưa có query search** |
| `MessageController` (`/api/channels/{id}/messages`), `PrivateMessageController` (`/api/messages/private`) | ✅ Có, nhưng **không có endpoint search** |
| Gateway route `/api/messages/**` → messaging-service (8082) | ✅ Có (route số 5) |
| Client UI tìm tin nhắn | ❌ Chưa có |

→ **Cần làm mới:** repository query + service method + controller endpoint + client UI. Không cần đổi schema; chỉ thêm index DB.

### Tìm kiếm bạn bè / người dùng
| Thành phần | Trạng thái |
|---|---|
| `UserProfileController` → `GET /api/users/search?q=` (UP5) | ✅ **Đã có sẵn** |
| `UserProfileRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(...)` | ✅ Có |
| Gateway route `/api/users/**` → user-profile-service (8090) | ✅ Có (route số 2) |
| `FriendController` (`/api/friends`, `/pending`, `/request`, `/accept`, `/reject`) | ✅ Có |
| Client UI tìm bạn / gửi lời mời từ kết quả tìm kiếm | ❌ Chưa có (chỉ nhập tay username để gửi request) |
| Làm giàu kết quả search bằng trạng thái quan hệ (đã là bạn / đang chờ / lạ) | ❌ Chưa có |

→ **Cần làm mới:** chủ yếu là client UI. Backend chỉ cần bổ sung nhỏ (loại trừ chính mình, gắn trạng thái quan hệ).

---

## B. Mục tiêu & Phạm vi

**Trong phạm vi:**
- Tìm tin nhắn theo từ khóa trong: (a) kênh đang mở, (b) cuộc trò chuyện DM đang mở, (c) tùy chọn toàn cục theo user.
- Phân trang kết quả, highlight từ khóa, click kết quả → nhảy tới ngữ cảnh tin nhắn.
- Tìm người dùng theo `username` / `displayName`, hiển thị avatar + trạng thái quan hệ, gửi lời mời kết bạn ngay từ kết quả.

**Ngoài phạm vi (YAGNI):**
- Full-text search engine (Elasticsearch / Postgres `tsvector`) — chỉ cân nhắc nếu LIKE không đủ ở quy mô lớn (xem mục G).
- Tìm theo file đính kèm / nội dung file.
- Fuzzy / typo-tolerant search.

---

## C. Thiết kế Backend

### C1. Message Search — messaging-service

**1. Repository** (`MessageRepository.java`) — thêm query phân trang dùng `Pageable`:

```java
// Tìm trong 1 channel
@Query("SELECT m FROM ChatMessage m WHERE m.channelId = :channelId " +
       "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
List<ChatMessage> searchInChannel(@Param("channelId") Long channelId,
                                  @Param("q") String q, Pageable pageable);

// Tìm trong DM giữa 2 user
@Query("SELECT m FROM ChatMessage m WHERE " +
       "((m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)) " +
       "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
List<ChatMessage> searchInPrivate(@Param("u1") String u1, @Param("u2") String u2,
                                  @Param("q") String q, Pageable pageable);

// Tìm toàn cục theo user (mọi tin nhắn user gửi/nhận)
@Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user OR m.receiver = :user) " +
       "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
List<ChatMessage> searchAllForUser(@Param("user") String user,
                                   @Param("q") String q, Pageable pageable);
```

**2. Controller** — endpoint mới `MessageSearchController.java` (file riêng, giữ <200 dòng, đặt dưới `/api/messages/search` để khớp gateway route `/api/messages/**`):

```
GET /api/messages/search?q={kw}&scope={channel|private|all}&channelId=&targetUser=&limit=50
Header: X-User-Id (gateway đã inject sau JwtAuthFilter)
```
- `scope=channel` → cần `channelId`.
- `scope=private` → cần `targetUser` (kết hợp `X-User-Id`).
- `scope=all` → chỉ dùng `X-User-Id`.
- Validate: `q` không rỗng, độ dài ≥ 2 ký tự (tránh quét toàn bảng vô nghĩa), `limit` ≤ 100.
- Trả `List<ChatMessage>` (đồng nhất format với các controller hiện có) hoặc DTO gọn nếu cần.

**3. Service** — đưa logic chọn scope + validation vào `MessageService` (hoặc method riêng) để controller mỏng.

**4. Index DB** — thêm migration/DDL index để LIKE không quét toàn bảng:
- `idx_chat_messages_channel_id (channel_id)`
- `idx_chat_messages_sender (sender)`, `idx_chat_messages_receiver (receiver)`
- (Tùy chọn về sau) Postgres GIN trigram trên `content` cho `LIKE '%..%'` — xem mục G.

**5. Bảo mật/uỷ quyền:** xác minh `X-User-Id` có quyền xem channel/DM trước khi trả kết quả (tái dùng cơ chế kiểm tra quyền sẵn có trong `MessageService` / `RoleClient` nếu áp dụng cho channel).

### C2. Friend / User Search — user-profile-service & friend-service

Endpoint `GET /api/users/search?q=` **đã có**. Bổ sung nhỏ:

**1. user-profile-service:**
- Loại trừ chính người gọi khỏi kết quả (nhận `X-User-Id`, filter ra).
- Giới hạn `limit` (vd 20) + validate `q` ≥ 2 ký tự.
- (Tùy chọn) trả kèm `avatarUrl`, `displayName`, `customStatus` (model đã có sẵn các trường này → không cần đổi gì).

**2. friend-service (làm giàu trạng thái quan hệ):** để UI hiển thị nút đúng ("Kết bạn" / "Đang chờ" / "Bạn bè"):
- **Phương án ưu tiên (KISS):** client gọi song song `/api/users/search` + `/api/friends` + `/api/friends/pending`, rồi tự gộp trạng thái phía client. **Không cần đổi backend.**
- Phương án thay thế (nếu muốn gộp ở server): thêm `GET /api/friends/status?targetUser=` trả enum quan hệ. Chỉ làm nếu client gộp tỏ ra cồng kềnh → YAGNI, mặc định chọn phương án ưu tiên.

---

## D. Thiết kế Client (Swing — client-app)

### D1. Network layer
- **Mới:** `network/MessageSearchApiClient.java` — gọi `GET /api/messages/search` (kèm JWT từ `SessionManager`). Tái dùng pattern của `PrivateMessageApiClient` / `ChannelApiClient`.
- **Tái dùng:** thêm method `searchUsers(String q)` vào client API user-profile hiện có (hoặc tạo `UserSearchApiClient` nếu chưa có client cho `/api/users`). `FriendApiClient` đã có sẵn cho list/pending/request.

### D2. UI — Tìm tin nhắn
- Thêm **icon kính lúp 🔍** vào header phòng chat (Vùng 3) — cùng chỗ với icon Ghim 📌 đã có (tham chiếu `PinController` / header trong `ChatClientGUI`). Tái dùng `IconButton`.
- Click → mở **`MessageSearchPanel`** (overlay/popup bên phải hoặc dialog): ô nhập từ khóa + toggle phạm vi (Kênh này / Cuộc trò chuyện này / Tất cả).
- Kết quả: danh sách item (avatar, tên người gửi, thời gian, đoạn nội dung **highlight** từ khóa). Tái dùng style của `ChatMessageItem` ở dạng rút gọn (component mới `SearchResultItem`).
- Click 1 kết quả → đóng panel, `ChatHistoryView` cuộn/nhảy tới tin nhắn tương ứng (dùng `message.id`; nếu chưa load thì load quanh `before=id+1`).
- Debounce input (~300ms) tránh spam request; hiển thị trạng thái rỗng / đang tải.

### D3. UI — Tìm bạn bè
- Thêm **ô tìm kiếm** vào header của `FriendSidebar` (hiện có `titleLabel "Bạn bè"`), hoặc nút "Thêm bạn" mở `AddFriendDialog`.
- **`AddFriendDialog` / `FriendSearchPanel`** (component mới): ô nhập → gọi `searchUsers` (debounce) → list kết quả (avatar + username + displayName + trạng thái quan hệ).
- Mỗi item có nút theo trạng thái:
  - Lạ → **"Kết bạn"** → `POST /api/friends/request`.
  - Đang chờ (mình gửi) → nhãn **"Đã gửi lời mời"** (disabled).
  - Đang chờ (họ gửi) → **"Chấp nhận"** → `POST /api/friends/accept`.
  - Đã là bạn → **"Nhắn tin"** (mở DM) / nhãn "Bạn bè".
- Sau khi gửi request thành công → `Toast` xác nhận (tái dùng `gui/components/feedback/Toast.java`).

### D4. Theme & chuẩn UI
- Dùng `AppColors`, `AppFonts`, `UiScale` sẵn có. Không tự đặt màu/size cứng.
- Mỗi file UI mới giữ <200 dòng (tách component nếu phình to).

---

## E. Các file liên quan

**Tạo mới (backend):**
- `messaging-service/.../controller/MessageSearchController.java`
- (DDL) index cho `chat_messages` — thêm vào script init DB hiện có / migration.

**Sửa (backend):**
- `messaging-service/.../repository/MessageRepository.java` — +3 query search.
- `messaging-service/.../service/MessageService.java` — logic scope + validate + check quyền.
- `user-profile-service/.../controller/UserProfileController.java` + `service/UserProfileService.java` — loại trừ self, limit, validate.

**Tạo mới (client):**
- `client-app/.../network/MessageSearchApiClient.java`
- `client-app/.../gui/components/chat/MessageSearchPanel.java` (+ `SearchResultItem.java`)
- `client-app/.../gui/components/friends/FriendSearchPanel.java` (hoặc `AddFriendDialog.java`)
- (nếu chưa có) `client-app/.../network/UserSearchApiClient.java`

**Sửa (client):**
- `client-app/.../gui/ChatClientGUI.java` — gắn icon search vào header phòng chat + wiring nhảy tới tin nhắn.
- `client-app/.../gui/chat/ChatHistoryView.java` — hỗ trợ scroll/jump tới `messageId`.
- `client-app/.../gui/components/friends/FriendSidebar.java` — gắn ô tìm/nút thêm bạn.

**Gateway:** không đổi — `/api/messages/**` và `/api/users/**` đã route đúng.

---

## F. Các bước triển khai (Phases)

### Phase 1 — Backend Message Search
1. Thêm 3 query vào `MessageRepository`.
2. Thêm scope/validate/auth vào `MessageService`.
3. Tạo `MessageSearchController` (`GET /api/messages/search`).
4. Thêm index DB cho `chat_messages`.
5. Build qua Docker/Jenkins (máy dev không có JDK/Maven local — xem ghi chú `new_features.md`).
6. Test endpoint bằng file `.http` trong `tnguyen/test/`.

### Phase 2 — Backend Friend/User Search (chỉnh nhỏ)
1. `user-profile-service`: loại trừ self + limit + validate `q`.
2. Xác nhận giữ phương án gộp trạng thái ở client (không thêm endpoint mới).
3. Test `GET /api/users/search?q=`.

### Phase 3 — Client Message Search UI
1. `MessageSearchApiClient`.
2. `MessageSearchPanel` + `SearchResultItem` (highlight, debounce).
3. Icon 🔍 trong header + wiring jump-to-message trong `ChatHistoryView`.

### Phase 4 — Client Friend Search UI
1. `searchUsers` ở network layer.
2. `FriendSearchPanel` / `AddFriendDialog` + gộp trạng thái quan hệ.
3. Gắn vào `FriendSidebar` + Toast xác nhận.

### Phase 5 — Test & Review
1. `tester` agent: build + smoke test 2 luồng search.
2. `code-reviewer` agent.
3. Cập nhật `docs/project-changelog.md` + `tnguyen/docs/changelog_summary.md`.

---

## G. Rủi ro & Cân nhắc

| Rủi ro | Giảm thiểu |
|---|---|
| `LIKE '%kw%'` quét toàn bảng khi dữ liệu lớn | Bắt buộc index theo `channel_id`/`sender`/`receiver` để thu hẹp trước; `q` ≥ 2 ký tự; `limit` ≤ 100. Nâng cấp Postgres `pg_trgm` GIN index nếu cần — **chỉ làm khi đo được chậm** (YAGNI). |
| Rò rỉ tin nhắn không có quyền xem | Kiểm tra quyền channel/DM trong service trước khi trả. |
| Jump-to-message khi tin nhắn chưa load trong `ChatHistoryView` | Load lân cận theo `message.id` rồi cuộn; fallback mở đúng channel/DM trước. |
| Trùng lặp logic gộp trạng thái bạn bè ở client | Gói trong 1 helper dùng chung (DRY). |
| Spam request khi gõ | Debounce ~300ms + huỷ request cũ. |

---

## H. Tiêu chí hoàn thành (Definition of Done)
- [ ] `GET /api/messages/search` trả đúng kết quả cho cả 3 scope, có phân trang & check quyền.
- [ ] Index DB đã thêm; truy vấn không quét toàn bảng.
- [ ] `GET /api/users/search` loại trừ self, có limit/validate.
- [ ] Client: tìm tin nhắn → highlight → click nhảy tới đúng tin nhắn.
- [ ] Client: tìm người dùng → hiển thị trạng thái quan hệ → gửi/chấp nhận lời mời ngay từ kết quả.
- [ ] Build pass qua Docker/Jenkins; `code-reviewer` không còn lỗi nghiêm trọng.
- [ ] Changelog đã cập nhật.

---

## I. Câu hỏi chưa chốt — ĐÃ CHỐT (mặc định khi triển khai)
1. ✅ Chứa-từ-khóa đơn giản (LIKE), highlight từ khóa trong kết quả.
2. ✅ Scope mặc định theo ngữ cảnh đang mở (kênh/DM); có lựa chọn "Tất cả tin nhắn".
3. ✅ Lấy 50 kết quả đầu (chưa làm "tải thêm" — YAGNI, bổ sung sau nếu cần).

## J. Trạng thái triển khai — ✅ HOÀN THÀNH (2026-06-09)
- Backend: `MessageSearchController`, 3 query repo, index `chat_messages`; refine `/api/users/search`.
- Client: `MessageSearchApiClient`, `MessageSearchPanel`, `AddFriendDialog`, icon 🔍 + jump-to-message, `FriendSidebar` mở dialog tìm bạn.
- Build: cả 15 module Maven `compile` SUCCESS (JDK 17 + mvnw).
- Test thủ công: `tnguyen/test/test-search.http`.
- Code review: đã xử lý H2/M1/M2/M3/L1/L3.
- ✅ Nợ kỹ thuật membership-check cho `scope=channel`: `MessageService.canSearchChannel` (read-only, không side-effect), không phải member → trả mảng rỗng. serverId suy ra từ tin nhắn của channel (không thêm cross-service call).

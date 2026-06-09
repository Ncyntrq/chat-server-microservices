# 🛠 Báo Cáo Thay Đổi & Nâng Cấp Hệ Thống (Changelog)

Tài liệu này tổng hợp toàn bộ các chỉnh sửa, nâng cấp, và các tính năng mới đã được triển khai trên cả **Client (UI)** và **Backend (Microservices)** để đáp ứng yêu cầu của hệ thống Chat thời gian thực.

---

## 1. Dịch Vụ Quản Lý Vai Trò (Role Service)
Đây là phần nhận được nhiều cập nhật nhất để giải quyết triệt để lỗi "null" và hoàn thiện quy trình phân quyền.

### 🟢 Thêm mới
- **API `PUT /api/servers/roles/{roleId}`**: Bổ sung Endpoint để cho phép chủ Server cập nhật lại Tên và Mã màu của Role.
- **API `DELETE /api/servers/roles/{roleId}`**: Bổ sung Endpoint để xóa một Role khỏi Server.
- **`RoleApiClient` (Client)**: Khai báo 2 phương thức `updateRole` và `deleteRole` để kết nối giao diện với Backend.
- **Bảng Màu Trực Quan (JColorChooser)**: Tích hợp bảng chọn màu hiển thị ngay trên giao diện tạo/sửa Role, loại bỏ việc nhập mã Hex thủ công.

### 🟡 Thay đổi (Cập nhật)
- **Giao diện `RoleManagementDialog`**: Đập đi xây lại hoàn toàn theo kiến trúc Bảng chia đôi (Split-Pane). Cột trái là danh sách, cột phải là Form chỉnh sửa luôn hiển thị.
- **Sửa lỗi hiển thị chữ `null`**: Đổi ánh xạ đọc JSON từ `r.get("name")` thành đúng chuẩn API trả về là `r.get("roleName")`. Giờ đây tên Role mới tạo được hiển thị chính xác ngay lập tức.
- **Độ rộng Textbox**: Canh lại kích thước ô nhập Tên Role, giới hạn chiều dài và đưa vào `FlowLayout` để không bị kéo giãn mất thẩm mỹ.
- **Chế độ Mặc định**: Vừa mở cửa sổ Quản lý Role là kích hoạt ngay chế độ "Tạo Role Mới" (luôn mở Form bên phải) để có thể gõ ngay không cần click thêm nút nào.

### 🔴 Xóa bỏ
- **Bỏ Logic tự sinh 4 Role mặc định**: Trong `ServerServiceImpl` của hệ thống Backend, xóa bỏ lời gọi API `initDefaultRoles`. Giờ đây khi tạo Server mới, danh sách Role sẽ hoàn toàn trống, không tự đẻ ra 4 Role rác nữa.
- **Dọn dẹp Database**: Xóa sạch toàn bộ các bản ghi bị lỗi `null` (thực chất là 4 Role cũ) trong Database `chat_role_db` thông qua lệnh SQL của Docker.

---

## 2. Hệ Thống Nhắn Tin (Messaging Service)
Xử lý các sự cố ngăn cản việc tải tin nhắn cũ.

### 🟡 Thay đổi (Cập nhật)
- **Cấu hình API Gateway**: Fix lỗi định tuyến khiến tính năng Chat Riêng trả về mã lỗi `500 (No static resource api/messages/private)`.
- **Khắc phục Crash ở `MessageDTO`**: Cập nhật lại logic Deserialize (giải mã) chuỗi JSON của `java.time.LocalDateTime`, giúp ứng dụng Client không bị văng lỗi khi cố gắng hiển thị lịch sử trò chuyện.

### 🔴 Xóa bỏ
- **Bỏ Log tải trò chuyện**: Xóa bỏ hoàn toàn các thông báo rác dạng "Đang tải trò chuyện...", "WebSocket mở kết nối..." trên luồng hiển thị tin nhắn ở giao diện Client, giúp trải nghiệm chat liền mạch và tinh gọn hơn.

---

## 3. Hệ Thống Server & Channel (Server / Channel Service)
Tinh chỉnh luồng trải nghiệm khi tạo và tham gia Server.

### 🟡 Thay đổi (Cập nhật)
- **Logic Tạo Kênh Mặc Định**:
  - Tại `ServerServiceImpl`, khi một Server mới sinh ra, hệ thống tự gọi ngầm sang `channel-service` để tạo kênh `# General`.
  - **Sửa Lỗi Phân Quyền (400 Bad Request)**: Thêm Header `X-User-Id` mang ID của chủ Server vào API nội bộ để `channel-service` xác thực quyền tạo kênh.
- **Giao diện Thanh Channel**:
  - Cập nhật hiển thị tên Kênh: Thêm tiền tố `#` (như Discord), đổi tên hiển thị từ "Kênh" thành "Kênh chat".
  - Chuyển biểu tượng dấu cộng `+` xuống dưới và sửa lỗi hiển thị ô vuông. Bấm vào nguyên dòng để tạo kênh mới.
- **Mã Mời (Invite Code)**: Cải tiến UI và Logic gia nhập Server, giờ chỉ yêu cầu bạn nhập Mã Mời (không cần ID server).

---

## 4. Dịch Vụ Hồ Sơ & File (Profile & File Upload)
Hỗ trợ thay đổi diện mạo cá nhân theo thời gian thực.

### 🟢 Thêm mới
- **API Tải File & Cập Nhật Hồ Sơ**: Gắn kết tính năng Upload ảnh (PNG, JPG) cho việc đổi Avatar của cá nhân và Avatar của Server.
- Cập nhật giao diện tự động Reload ảnh Avatar mới trên thanh điều hướng sau khi Upload thành công.

### 🟡 Thay đổi (Cập nhật)
- **UI Hồ Sơ**: Chuyển biểu tượng mở "Cài đặt hồ sơ" từ dấu ba chấm sang biểu tượng **Bánh Răng ⚙️**.

---

## 5. Trạng Thái và Bạn Bè (Presence / Friend Service)
Xử lý các yêu cầu cập nhật Real-time khắt khe.

### 🟡 Thay đổi (Cập nhật)
- **Màn hình Home Real-time**: Lời mời kết bạn giờ đây sẽ pop-up và hiển thị ngay lập tức nhờ bắt sự kiện qua mạng thay vì phải F5 hoặc chuyển màn hình.
- Trạng thái Online/Offline cũng được làm mới liên tục khi có user đăng nhập/đăng xuất.
- **Cấu hình Docker `friend-service`**: Sửa lỗi pull access denied cho `friend-service` bằng cách thiết lập tính năng tự động Build local (Build image trực tiếp trên máy) `docker compose up -d --build friend-service`.

### 🔴 Xóa bỏ
- **Thanh thành viên ở Home**: Bỏ hoàn toàn thanh Danh sách thành viên bên phải giao diện Home, vì bên trái đã có Danh sách Bạn bè, tránh gây trùng lặp và rối rắm giao diện.
- **Ẩn Khung Nhập Text**: Khi không ở trong 1 đoạn chat nào, vùng nhập tin nhắn sẽ tự động biến mất.

---

## 6. Tìm Kiếm Tin Nhắn & Tìm Kiếm Bạn Bè (Search)
Triển khai theo plan `13_plan_tim_kiem_tin_nhan_va_ban_be.md`.

### 🟢 Thêm mới
- **API `GET /api/messages/search`** (messaging-service): tìm tin nhắn theo từ khóa với 3 phạm vi `scope=channel|private|all`, có phân trang (`limit` ≤ 100) và validate từ khóa ≥ 2 ký tự. Phạm vi `private`/`all` tự giới hạn theo `X-User-Id`.
- **3 query tìm kiếm** trong `MessageRepository`: `searchInChannel`, `searchInPrivate`, `searchAllForUser` (LIKE không phân biệt hoa thường).
- **Index DB** cho `chat_messages` (`channelId`, `sender`, `receiver`) qua `@Index` để thu hẹp truy vấn.
- **`MessageSearchApiClient`** (Client): gọi endpoint tìm kiếm, tái dùng `RawChatMessage`.
- **`MessageSearchPanel`** (Client): dialog tìm tin nhắn — ô nhập debounce ~300ms, chọn phạm vi, highlight từ khóa, click kết quả để nhảy tới tin nhắn.
- **Icon 🔍** trên header phòng chat + cơ chế `scrollToMessage`/flash-highlight trong `ChatHistoryView`.
- **`AddFriendDialog`** (Client): dialog tìm bạn — tìm theo username/tên hiển thị (debounce), hiển thị trạng thái quan hệ (Kết bạn / Chấp nhận / Bạn bè) và gửi/chấp nhận lời mời ngay từ kết quả.

### 🟡 Thay đổi (Cập nhật)
- **`GET /api/users/search`** (user-profile-service): loại trừ chính người gọi (`X-User-Id`), validate `q` ≥ 2 ký tự, giới hạn kết quả tại DB (`findTop21...`) thay vì lọc toàn bộ trong bộ nhớ.
- **`FriendSidebar`**: nút "➕ Thêm bạn" mở `AddFriendDialog` (tìm kiếm) thay cho ô nhập username thủ công.
- **`UserProfileApiClient.searchUsers`**: URL-encode từ khóa (an toàn với khoảng trắng / ký tự đặc biệt).

### 🔒 Bảo mật
- **Kiểm tra membership cho `scope=channel`**: `MessageService.canSearchChannel` xác minh user là member của server sở hữu channel (read-only, KHÔNG tự thêm member như `hasPermission`). serverId suy ra từ chính tin nhắn của channel — không cần gọi thêm service. Không phải member → trả mảng rỗng (không lộ dữ liệu, không báo lỗi). Channel không có tin / global → không ràng buộc.

### ⚠️ Ghi chú kỹ thuật
- Tìm theo `content` dùng `LIKE '%kw%'` (full scan trong phạm vi đã lọc) — đủ cho quy mô hiện tại; nâng cấp full-text khi cần.
- `GET /api/channels/{id}/messages` (đọc lịch sử) giữ nguyên hành vi lenient để không phá luồng auto-join hiện có; chỉ message-search siết membership.

---

## 7. Giao diện Sáng/Tối (Light/Dark Theme)
Triển khai Phần 1 của plan `14_plan_theme_sang_toi_va_hoa_tiet_nen.md`.

### 🟢 Thêm mới
- **`Theme` (enum)** + **`ThemeManager`** (Preferences như `UiScale`): nhớ theme đã chọn giữa các phiên.
- **Nút chuyển chế độ ☀/☾** trên `UserFooterPanel` (góc dưới sidebar) — click để đổi Sáng/Tối tức thì.
- **Tab "Giao diện"** trong `UserSettingsDialog` (`AppearancePanel`): radio chọn Sáng/Tối.
- **Bảng màu LIGHT** đầy đủ trong `AppColors` (nền/chữ đảo theo theme; brand/semantic/status giữ nhất quán).

### 🟡 Thay đổi (Cập nhật)
- **`AppColors`**: chuyển field từ `final` → mutable, nạp qua `apply(Theme)` (giống cơ chế rescale của `AppFonts`) ⇒ **không phải sửa ~330 call-site**.
- **`ClientApplication`**: `applyLookAndFeel()` chọn `FlatLightLaf`/`FlatDarkLaf` theo theme lúc khởi động; `restartChatWindow()` dựng lại cửa sổ khi đổi theme (cách chắc chắn vì màu set imperative lúc dựng component).
- **`ChatClientGUI`**: thêm `getSessionUsername()` + `shutdown()` (đóng WS + tắt callback trước khi huỷ frame).

### ⚠️ Ghi chú kỹ thuật
- Đổi theme = **dựng lại cửa sổ chat** (đóng WS cũ TRƯỚC khi tạo cửa sổ mới → tránh 2 kết nối WS trùng cho cùng user; dispose + tạo mới trong cùng 1 EDT task → không bị AWT auto-shutdown).
- `TEXT_WHITE` ở light mode map sang màu đậm (#1A1C1E) vì được dùng như "chữ nổi bật trên surface"; chữ trắng trên nút brand dùng `Color.WHITE` hard-code nên không bị ảnh hưởng.

---

## 8. Hoạ tiết nền chat (Chat Wallpaper)
Triển khai Phần 2 của plan `14_plan_theme_sang_toi_va_hoa_tiet_nen.md` (phương án A — nhẹ).

### 🟢 Thêm mới
- **Asset** `client-app/src/main/resources/wallpapers/` (~864KB): 2 gradient nền (dark/light) + 3 pattern (Mèo / Star Wars / Kẹo ngọt) × 2 biến thể theo theme. Lấy từ `tnguyen/design`, đổi tên kebab.
- **`WallpaperManager`** (Preferences): nhớ lựa chọn `Ngẫu nhiên / Không nền / <pattern>`; chế độ Ngẫu nhiên bốc 1 pattern ổn định theo phiên.
- **`WallpaperRenderer`**: nạp+cache ảnh, ghép gradient + pattern phủ kín (scale cover), cache ảnh đã ghép theo (size,theme,pattern) → cuộn không bị re-scale.
- **Selector trong tab "Giao diện"** (`AppearancePanel`): combo chọn hoạ tiết, đổi → repaint vùng chat ngay (không rebuild cửa sổ).

### 🟡 Thay đổi (Cập nhật)
- **`ChatHistoryView`**: vẽ nền wallpaper ở `paintComponent` (cố định, không cuộn theo tin); `viewport` + `chatHistoryPanel` chuyển **non-opaque** để lộ nền.
- **`ChatClientGUI`**: thêm `refreshChatBackground()`.

### ⚠️ Ghi chú kỹ thuật
- Pattern vẽ ở **alpha 0.4** (đủ trang trí, chữ vẫn đọc rõ vì glyph vẽ đè lên, không cần bong bóng tin nhắn).
- Pattern đã có sẵn 2 biến thể (đường trắng cho theme tối / đường đậm cho theme sáng) → **không cần tint runtime**, chỉ chọn đúng biến thể theo theme.
- "Không nền" → nền phẳng `BG_PRIMARY` (giữ giao diện cũ).

### 🔧 Tinh chỉnh (2026-06-09)
- **Motif nhỏ hơn**: hoạ tiết chuyển từ scale-cover (phóng to) sang **tile bản thu nhỏ 50%** (`TexturePaint`) → motif nhỏ, lặp dày; alpha giảm `0.4 → 0.30`.
- **Box mờ sau tin nhắn** (`MSG_BUBBLE`, theme-aware): dark = navy ~76%, light = trắng ~80% — chỉ hiện khi bật wallpaper, giúp chữ nổi rõ không bị hoạ tiết gây rối (`ChatMessageItem.paintComponent`).
- **Tăng tương phản chữ**: `TEXT_NORMAL` dark `#D7DBE2 → #E6E9EF`, light `#2E3338 → #1F2329`; `TEXT_MUTED` cũng chỉnh đậm/sáng tương ứng.

### 🔧 Tinh chỉnh box tin nhắn (2026-06-09, lần 2)
- **Box frosted (blur) thay cho opacity đặc**: nền sau tin nhắn nay là bản wallpaper **đã làm mờ** (`WallpaperRenderer.blurredFor`, blur nhanh bằng downscale/upscale, cache) + 1 lớp tint nhẹ (giảm `MSG_BUBBLE`: dark ~55%, light ~65%).
- **Box ôm sát nội dung**: rộng theo bề rộng text/đính kèm/header thực tế (`measuredContentWidth`, cap `maxBubbleWidth`), cao theo nội dung — không còn trải full-width.
- **Căn giữa dọc**: nội dung tin nhắn bọc trong `GridBagLayout` (anchor WEST, fill ngang) ⇒ căn giữa theo chiều dọc trong ô.

### 🔧 Tinh chỉnh box + chuyển theme (2026-06-09, lần 3)
- **Box ôm sát thật sự**: `JTextPane` của tin nhắn nay báo bề rộng ÔM SÁT (dòng dài nhất, cap `maxBubbleWidth`) → box & toolbar bám sát nội dung; bỏ ước lượng `measuredContentWidth`.
- **Box to/cân đối hơn**: padding `12×8`, chiều cao tối thiểu `MIN_BUBBLE_H=36` (tương xứng toolbar Sửa/Ghim/Xoá), căn giữa quanh tâm nội dung.
- **Toolbar nằm CẠNH tin nhắn**: chuyển toolbar từ EAST (mép phải) vào `centerWrap` (gridx1, ngay sau bubble); khi ẩn GridBag bỏ qua nên content không bị xô.
- **Toolbar cũng frosted**: dùng chung `WallpaperRenderer.paintFrosted(...)` (blur + tint) khi bật wallpaper; tắt wallpaper thì nền đặc `BG_FLOATING` như cũ.
- **Đổi theme TẠI CHỖ (không reload)**: `ClientApplication.applyThemeLive` — đổi FlatLaf L&F + `FlatLaf.updateUI()` + remap màu nền/chữ trung tính đã cache (`AppColors.buildRemap`); component tự vẽ (tin nhắn/wallpaper/toolbar) đọc màu mỗi lần repaint nên tự cập nhật. Bỏ hẳn rebuild cửa sổ / reconnect WS / reload dữ liệu.
  - `TEXT_WHITE` (dark) đổi thành `#FEFEFE` để khác `Color.WHITE` → remap không đụng chữ trắng hard-code trên nút brand.
  - Hạn chế nhỏ: vài đường viền (matte/line border) tạo từ AppColors không remap → có thể còn màu cũ tới lần mở lại; chấp nhận được, đa số border nằm ở dialog (dựng mới mỗi lần mở).

### 🔧 Tinh chỉnh box (2026-06-09, lần 4)
- **Box to ngay từ đầu (hết "to khi hover")**: nguyên nhân là hàng thấp hơn box nên box bị cắt, hover hiện toolbar mới đẩy hàng cao ra. Khắc phục: `ChatMessageItem.getPreferredSize` ép **chiều cao hàng tối thiểu** (≥ `MIN_BUBBLE_H+4`) ⇒ box hiển thị đầy đủ ngay, toolbar hover nằm gọn trong hàng (không nhảy). Box cao bám theo hàng (đồng đều), rộng ôm sát nội dung.
- **Giảm margin giữa các box**: strut giữa tin `10 → 3`; padding dọc item `8/2 → 4/1`.
- **Border-radius + viền box**: bo góc `16` + viền mảnh `SEPARATOR` cho box tin nhắn rõ nét.

### 🔧 Menu hành động tin nhắn (2026-06-09, lần 5)
- Hover hiện **một nút "⋯"** (thay vì lộ thẳng Sửa/Ghim/Xóa). Bấm "⋯" → mở `JPopupMenu` gồm Sửa / Ghim / Xóa theo quyền. Menu tự theo theme (FlatLaf). Giữ toolbar hiển thị trong lúc menu mở (cờ `menuOpen`) để popup không đóng theo nút.

---

## 9. Nhóm tin nhắn theo thời gian + giờ hover
Triển khai plan `15_plan_hien_thi_gio_va_nhom_tin_nhan.md`.

### 🟡 Thay đổi (Cập nhật)
- **`ChatHistoryView`**: gộp nhóm khi **cùng người gửi VÀ cách tin trước ≤ 5 phút** (`GROUP_GAP_MINUTES`); chỉ tin đầu nhóm hiện avatar/tên/giờ. Theo dõi thêm `lastTimestamp`, reset khi đổi người / tin hệ thống / `clear()`. Quá 5 phút hoặc đổi người → tách nhóm mới.
- **`ChatMessageItem`**: `buildToolbar` → `buildHoverBar` — panel hover (frosted) chứa **[giờ nhỏ nếu tin compact] + [nút "⋯" nếu có quyền]**. Tin bị ẩn giờ → hover hiện **giờ nhỏ `HH:mm`** cạnh "⋯"; tin của người khác (không có "⋯") vẫn hiện giờ hover.

### 🔧 Khoảng cách nhóm theo giờ (2026-06-09)
- Tin cách tin trước **≥ 1 giờ** → chèn **khoảng trống rộng hơn** (18px) phía trên để phân tách trực quan. Hằng `BIG_GAP_MINUTES=60`, `BIG_GAP_PX=18` trong `ChatHistoryView`.

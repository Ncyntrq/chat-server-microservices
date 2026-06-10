# 15. Plan — Hiển thị giờ & nhóm tin nhắn theo thời gian

> **Ngày tạo:** 2026-06-09 · **Nhánh:** tnguyen-8/6 · **Phạm vi:** client-app (Swing), thuần UI
> Tuân thủ YAGNI / KISS / DRY. Không đổi backend, không đổi schema.

---

> ✅ **ĐÃ TRIỂN KHAI (2026-06-09)** theo phương án đề xuất (ngưỡng 5 phút, không tách theo ngày, giờ `HH:mm`).
> Files: `ChatHistoryView` (lastTimestamp + 5 phút), `ChatMessageItem` (buildHoverBar: giờ + ⋯). Build `client-app` SUCCESS. Chờ user test GUI.

## A. Mục tiêu (theo yêu cầu)
1. Tin nhắn **LIÊN TIẾP cùng 1 người** trong vòng **5 phút** → chỉ hiển thị **giờ ở tin ĐẦU nhóm** (các tin sau ẩn giờ + ẩn avatar/tên như hiện tại).
2. Các tin bị ẩn giờ (vd cách nhau 1 phút) → **hover** vào mới hiện **giờ nhỏ**, đặt **bên cạnh nút "⋯"** (nút mở menu Ghim/Sửa/Xóa).
3. Khi **người khác** nhắn → bắt đầu **nhóm mới** (hiện lại avatar/tên/giờ), rồi áp dụng quy tắc tương tự.

---

## B. Hiện trạng (đối chiếu code)
- **`ChatHistoryView.appendMessage`** (client): nhóm tin **chỉ theo người gửi liên tiếp** (`lastSender`), **CHƯA xét thời gian**. Tin liên tiếp cùng người → `isConsecutive=true` → layout "compact" (ẩn header gồm tên + giờ).
- **`ChatMessageItem`**:
  - Tin đầu nhóm (`!compact`): header có **tên + giờ** (`HH:mm`) — đã hiển thị giờ.
  - Tin compact (`isConsecutive`): **ẩn hẳn header → không có giờ**.
  - Toolbar hover hiện **nút "⋯"** (gridx1 trong `centerWrap`), bấm mở menu Sửa/Ghim/Xóa. Toolbar chỉ được tạo khi user **có hành động** (`canEdit||canPin||canDelete`) → tin của người khác mà mình không có quyền sẽ **không có toolbar**.
- **`MessageDTO.getTimestamp()`**: `LocalDateTime` — đủ dữ liệu để so khoảng cách thời gian.

→ Cần: (1) thêm điều kiện **5 phút** vào logic nhóm; (2) thêm **nhãn giờ nhỏ hiện khi hover** cho tin compact, **độc lập** với việc có toolbar hành động hay không.

---

## C. Thiết kế

### C1. Nhóm theo thời gian — `ChatHistoryView`
- Thêm field `private LocalDateTime lastTimestamp;` (cạnh `lastSender`).
- Hằng `private static final long GROUP_GAP_MINUTES = 5;`
- Trong `appendMessage`, tính `isConsecutive`:
  - `true` khi: **cùng người gửi** (`sender.equals(lastSender)`) **VÀ** khoảng cách thời gian với tin trước `≤ 5 phút`
    (`lastTimestamp != null && Duration.between(lastTimestamp, ts).abs().toMinutes() < GROUP_GAP_MINUTES`).
  - Ngược lại (người khác / quá 5 phút / thiếu timestamp) → `false` ⇒ **tin đầu nhóm mới** (hiện avatar+tên+giờ).
- Cập nhật `lastSender = sender; lastTimestamp = ts;` cho tin thường; với tin SYSTEM/JOIN/LEAVE/ERROR → reset `lastSender=null; lastTimestamp=null` (như hiện tại + reset time).
- `clear()` reset `lastTimestamp = null` (cùng chỗ reset `lastSender`).
- **Lưu ý mép nhóm khi tải lịch sử**: history nạp tuần tự qua `appendMessage` nên logic trên áp dụng đúng. (Không xử lý gộp xuyên ngày riêng — 5 phút đã đủ; có thể bổ sung "khác ngày → tách nhóm" sau nếu cần — YAGNI.)

### C2. Giờ hover cho tin compact — `ChatMessageItem`
Mục tiêu: tin compact hiện **giờ nhỏ** khi hover, **bên trái nút "⋯"**, kể cả khi không có toolbar hành động.

**Tách bạch 2 thứ trong vùng hover (gridx1 của `centerWrap`):**
- **Nhãn giờ** (`JLabel`, font `AppFonts.TINY`, màu `TEXT_MUTED`): chỉ tạo khi `isConsecutive` (tin compact). Nội dung = `message.getTimestamp().format("HH:mm")`.
- **Nút "⋯"**: chỉ khi có hành động (giữ logic hiện tại).

**Gộp vào 1 panel hover (frosted)** đặt ở gridx1:
- Tạo panel này khi **`isConsecutive` HOẶC có hành động** (để tin người khác — không toolbar — vẫn có chỗ hiện giờ hover).
- Bố cục: `[giờ nhỏ]  [⋯]` (FlowLayout). Nếu chỉ có giờ (không hành động) → panel chỉ chứa giờ.
- `setVisible(false)` mặc định; hover hiện (tái dùng cơ chế `setHover` + cờ `menuOpen` hiện có).
- Nền **frosted** dùng lại `WallpaperRenderer.paintFrosted` (đang áp cho toolbar) → đồng nhất.

**Đổi tên/khái quát:** biến `toolbar` hiện tại → vai trò "panel hover" chứa giờ + ⋯. Đổi điều kiện tạo từ `buildToolbar()` (chỉ khi có action) sang luôn tạo khi `isConsecutive || hasActions`. Giữ `showActionMenu`/menu như cũ.

**Tin đầu nhóm (`!compact`)**: giờ vẫn nằm ở header (không lặp lại trong panel hover). Panel hover chỉ chứa "⋯" (nếu có action) như hiện tại.

### C3. Vị trí hiển thị giờ hover
- "Nhỏ nhỏ bên cạnh nút ⋯" → nhãn giờ đặt **trước** (bên trái) nút ⋯ trong cùng panel frosted. Cả cụm hiện khi hover.

---

## D. Các file liên quan
**Sửa:**
- `client-app/src/main/java/gui/chat/ChatHistoryView.java` — thêm `lastTimestamp` + điều kiện 5 phút + reset trong `clear()`.
- `client-app/src/main/java/gui/components/chat/ChatMessageItem.java` — panel hover chứa `[giờ (nếu compact)] + [⋯ (nếu có action)]`; tạo khi `isConsecutive || hasActions`; giờ format `HH:mm` (đã có `TIME_FMT`).

**Không đổi:** backend, `MessageDTO`, các service.

---

## E. Các bước triển khai
1. `ChatHistoryView`: thêm field + hằng + sửa tính `isConsecutive` (sender + ≤5p) + reset.
2. `ChatMessageItem`: tổng quát hoá `buildToolbar` → `buildHoverBar` (giờ + ⋯); thêm nhãn giờ cho tin compact; giữ frosted + menu + `menuOpen`.
3. Build `client-app` (mvnw) kiểm tra compile.
4. Test thủ công: gửi nhiều tin liên tiếp <5p (ẩn giờ, hover hiện), >5p (tách nhóm, hiện giờ), xen kẽ người khác (tách nhóm), tin của người khác không có quyền (vẫn hiện giờ hover, không có ⋯).

---

## F. Tiêu chí hoàn thành
- [ ] Tin liên tiếp cùng người ≤5p: chỉ tin đầu hiện giờ; tin sau ẩn giờ.
- [ ] Tin sau hover → hiện giờ nhỏ cạnh "⋯" (frosted, theo theme).
- [ ] >5p hoặc đổi người → tách nhóm (hiện lại avatar/tên/giờ).
- [ ] Tin của người khác (không có nút ⋯) vẫn hiện giờ khi hover.
- [ ] Build `client-app` SUCCESS; không phá hover-menu/edit/pin hiện có.

---

## G. Rủi ro & lưu ý
- **Timestamp null**: tin gửi mới có thể chưa có timestamp ngay (hiện `createTextBody`/header fallback "Bây giờ"). Khi `ts==null` → coi như **không nhóm** (an toàn) + nhãn hover bỏ qua.
- **Cờ `menuOpen`/hover hiện có**: panel hover đổi nội dung nhưng giữ cơ chế ẩn/hiện ⇒ không phá popup menu.
- **Chiều cao hàng**: panel hover thêm nhãn giờ vẫn nằm trong chiều cao tối thiểu hiện tại (`MIN_BUBBLE_H+4`) ⇒ không gây "nhảy" khi hover.

## H. Câu hỏi chưa chốt
1. Ngưỡng nhóm đúng **5 phút** chứ? (mặc định 5; dễ chỉnh qua hằng `GROUP_GAP_MINUTES`.)
2. Có cần tách nhóm khi **khác ngày** dù <5p (hiếm) không? (Đề xuất: không — YAGNI.)
3. Giờ hover định dạng `HH:mm` hay kèm ngày khi tin cũ? (Đề xuất: `HH:mm`, tooltip đầy đủ nếu cần sau.)

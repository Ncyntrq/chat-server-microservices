# 14. Plan — Theme sáng/tối + Hoạ tiết nền chat (wallpaper)

> **Ngày tạo:** 2026-06-09 · **Nhánh:** tnguyen-8/6 · **Phạm vi:** client-app (Swing)
> Hai tính năng liên quan nhưng tách bạch:
> 1. **Light/Dark mode** — chuyển theme sáng/tối, nhớ lựa chọn giữa các phiên.
> 2. **Hoạ tiết nền chat (wallpaper)** — nền doodle/gradient kiểu Telegram, có thể random.
>
> Tuân thủ YAGNI / KISS / DRY.

---

## ❓ TRẢ LỜI CÂU HỎI: có cần tách 32 ảnh & up vào dự án không?

**Tóm tắt: KHÔNG up cả tấm sprite-sheet, và KHÔNG cần đủ 32. Tách ra một SUBSET đã chọn lọc (≈6–10) thành từng file riêng, để dưới classpath resources.**

Lý do:
- File hiện tại `tnguyen/design/32 Telegram SVG Wallpapers (Community).png` nặng **9MB, 9053×3858** → nếu nạp nguyên tấm vào `BufferedImage` sẽ tốn **~140MB RAM** (9053×3858×4 byte). Không thể ship như vậy.
- Cấu trúc tấm ảnh: **hàng trên** = các nền gradient; **các hàng dưới** = hoạ tiết doodle ở 2 biến thể (tối bên trái / trắng bên phải). Đây là kiểu Telegram: **gradient + lớp hoạ tiết đơn sắc phủ lên, được tô màu (tint) theo theme**.

**Khuyến nghị (theo thứ tự ưu tiên):**

| Phương án | Mô tả | Đánh giá |
|---|---|---|
| **A. (Đề xuất) Subset PNG tiles, tint runtime** | Chọn ~6–10 hoạ tiết đẹp, export mỗi cái 1 file PNG **đơn sắc + alpha** (hoặc trắng nền trong suốt), để `resources/wallpapers/`. Runtime **tô màu** hoạ tiết theo theme (trắng mờ trên nền tối / xám mờ trên nền sáng) → **1 bộ dùng cho cả 2 theme** (DRY). | ✅ KISS, nhẹ, hợp cả light/dark |
| B. Subset PNG cả 2 biến thể (tối + trắng) | Tách riêng bản tối & bản trắng cho mỗi hoạ tiết → nhiều file gấp đôi, không tint. | ⚠️ Tốn file, kém DRY |
| C. Lấy SVG gốc của bộ Telegram | Bộ "Telegram Patterns" là community/open. SVG **tile liền mạch + tint vô cấp**. | ✅ Chất lượng nhất nhưng tốn công source SVG |
| D. Up cả sprite-sheet 9MB | Crop tại runtime. | ❌ Nặng RAM, biên crop không liền mạch khi tile |

> **Lưu ý kỹ thuật khi tách bằng crop tự động:** tấm sheet bố cục KHÔNG đều (hàng gradient rộng khác hàng hoạ tiết, có khoảng trống) → **không** crop lưới cố định sạch được. Nên **export thủ công** từng hoạ tiết đã chọn (Photoshop/Figma/Preview) hoặc dùng ImageMagick crop theo toạ độ từng ô.
>
> **Cảnh báo tile:** các mảnh raster cắt từ sheet **không tile liền mạch** (biên không khớp). Nếu muốn nền lặp đẹp → dùng phương án C (SVG) hoặc chọn hoạ tiết đủ lớn để **scale phủ kín** thay vì tile.

**Kết luận đề xuất:** Phương án **A** — tách ~6–10 hoạ tiết đơn sắc (alpha) thành file riêng dưới `client-app/src/main/resources/wallpapers/`, tint theo theme. Gradient nền có thể vẽ bằng code (`GradientPaint`) thay vì ảnh (KISS, không cần ship ảnh gradient).

---

## PHẦN 1 — LIGHT/DARK MODE  ✅ ĐÃ TRIỂN KHAI (2026-06-09)

> Files: `Theme`, `ThemeManager`, `AppColors` (final→mutable + apply), `ClientApplication`
> (applyLookAndFeel + restartChatWindow), `ChatClientGUI` (getSessionUsername/shutdown),
> `UserFooterPanel` (nút ☀/☾), `AppearancePanel` + `UserSettingsDialog` (tab Giao diện).
> Build `client-app` SUCCESS. Đã qua code-review (xử lý C2/H1/H2/L3). Chờ user test GUI.
> **Phần 2 (wallpaper) chưa làm.**


### 1.1 Hiện trạng & thách thức
- L&F: **FlatLaf `FlatDarkLaf`** (có sẵn `FlatLightLaf` trong cùng thư viện).
- `AppColors` dùng **`public static final Color`** → không reassign được lúc runtime.
- **~330 lượt tham chiếu trong 44 file**, đa số gọi imperative trong constructor (`setBackground(AppColors.X)`) → màu bị **cache lúc dựng component**.

→ Hệ quả: đổi theme runtime KHÔNG tự cập nhật các component đã dựng. Hai lựa chọn:

| Cách | Mô tả | Đánh giá |
|---|---|---|
| **(Đề xuất) Rebuild cửa sổ** | Đổi theme → dispose `ChatClientGUI` hiện tại → tạo lại với cùng session. | ✅ KISS, chắc chắn đúng, không sửa 330 call-site |
| Live re-apply | Walk component tree, set lại từng màu. | ❌ Mong manh, dễ sót, nhiều code |

→ **Chọn: Rebuild cửa sổ** (giống nhiều app "đổi giao diện → áp dụng lại"). Nhanh & đáng tin.

### 1.2 Kiến trúc
1. **`Theme` (enum)**: `DARK`, `LIGHT`. (KHÔNG làm `SYSTEM`/auto — YAGNI.)
2. **`ThemePalette`**: gom toàn bộ giá trị màu cho 1 theme (các field trùng tên `AppColors`).
3. **`AppColors` refactor**: đổi `final` → **biến static mutable**, nạp từ palette đang chọn. **Giữ nguyên tên field** ⇒ **0 thay đổi call-site**. Thêm `AppColors.apply(ThemePalette)`.
   - Màu **nền + chữ**: đảo theo theme (dark navy ↔ light).
   - Màu **semantic** (SUCCESS/DANGER/WARNING/BRAND/status/avatar): giữ gần như nguyên (tinh chỉnh nhẹ độ tương phản cho nền sáng nếu cần).
4. **`ThemeManager`** (mirror `UiScale`): singleton + `Preferences` lưu theme đang chọn; `current()`, `set(Theme)`.
5. **`ClientApplication`** (startup): đọc `ThemeManager.current()` → set `FlatLightLaf`/`FlatDarkLaf` + `AppColors.apply(...)` + `AppFonts.applyGlobalScale()` **TRƯỚC** khi dựng UI.
6. **Toggle UI**: thêm mục **"Giao diện / Appearance"** trong `gui/profile/UserSettingsDialog.java` (radio Sáng/Tối hoặc switch). Khi đổi → `ThemeManager.set()` → **rebuild cửa sổ**.
7. **Rebuild helper**: `ClientApplication.restartChatWindow()` — dispose frame cũ, set L&F + `AppColors.apply` + `FlatLaf.updateUI()`, tạo `new ChatClientGUI(username)` + `startSession()`.

### 1.3 Bảng màu LIGHT (đề xuất khởi điểm)
| Field | DARK (hiện tại) | LIGHT (đề xuất) |
|---|---|---|
| BG_PRIMARY | #1A1E27 | #FFFFFF |
| BG_SECONDARY | #151921 | #F2F3F5 |
| BG_TERTIARY | #0E1116 | #E3E5E8 |
| BG_HOVER | #232A38 | #E9EAED |
| BG_ACTIVE | #2B3344 | #DCDFE4 |
| BG_FLOATING | #1D222D | #FFFFFF |
| SEPARATOR | #272E3B | #D7D9DD |
| TEXT_NORMAL | #D7DBE2 | #2E3338 |
| TEXT_MUTED | #8B92A0 | #6A7178 |
| TEXT_HEADER | #F1F3F7 | #1A1C1E |
| TEXT_WHITE | #FFFFFF | #1A1C1E (đổi vai trò → "text nổi bật") |
| BRAND_PRIMARY | #5B6CFF | #5B6CFF (giữ) |

> ⚠️ `TEXT_WHITE` đang bị dùng như "chữ nổi bật trên nền tối" (36 chỗ). Ở light mode cần đổi thành màu đậm. Vì giữ tên field, chỉ cần palette LIGHT map `TEXT_WHITE → màu đậm`. Kiểm tra các chỗ dùng `TEXT_WHITE` trên nền sáng (vd nút brand vẫn cần chữ trắng) — tinh chỉnh từng case nếu lệch.

### 1.4 Các file (Phần 1)
**Tạo mới:** `gui/theme/Theme.java`, `gui/theme/ThemePalette.java`, `gui/theme/ThemeManager.java`.
**Sửa:** `gui/theme/AppColors.java` (final→mutable + apply), `gui/ClientApplication.java` (init theme + restart helper), `gui/profile/UserSettingsDialog.java` (mục Appearance + toggle).

---

## PHẦN 2 — HOẠ TIẾT NỀN CHAT (WALLPAPER)  ✅ ĐÃ TRIỂN KHAI (2026-06-09)

> Asset thực tế người dùng cung cấp: 2 gradient (dark/light) + 3 pattern (cats/starwars/sweets) × 2 biến thể.
> → Không cần tint runtime (đã có sẵn biến thể trắng/đậm theo theme); gradient dùng ảnh PNG (scale cover)
> thay vì GradientPaint vì iridescent đa điểm dừng. Files: `WallpaperManager`, `WallpaperRenderer`,
> `ChatHistoryView` (paintComponent + non-opaque), `ChatClientGUI.refreshChatBackground`, `AppearancePanel` (combo).
> Build SUCCESS, asset ~864KB đóng gói trong jar. Chờ user test GUI (tinh chỉnh alpha pattern nếu cần).


### 2.1 Mục tiêu
- Vẽ **nền hoạ tiết** phía sau luồng tin nhắn (vùng `ChatHistoryView`), kiểu Telegram.
- Hỗ trợ **chọn** wallpaper + chế độ **RANDOM** (ngẫu nhiên mỗi phiên / mỗi lần đổi).
- Tô màu hoạ tiết **theo theme** (mờ nhạt, không chói chữ).

### 2.2 Kiến trúc
1. **Assets**: `client-app/src/main/resources/wallpapers/` chứa ~6–10 PNG hoạ tiết đơn sắc + alpha (phương án A ở trên). Đặt tên ngữ nghĩa: `pattern-bubbles.png`, `pattern-stars.png`…
2. **`WallpaperManager`** (mirror `UiScale`): `Preferences` lưu id wallpaper đang chọn hoặc `RANDOM`/`NONE`; cung cấp `currentImage()` (nếu RANDOM → chọn 1 lúc khởi tạo phiên, vary theo index, **không** dùng `Math.random` nếu cần tái lập — dùng `System.nanoTime()%n` chấp nhận được cho UI).
3. **`ChatBackgroundPanel`** (JPanel custom, override `paintComponent`):
   - Vẽ **gradient nền** (bằng `GradientPaint` theo theme) → KHÔNG cần ảnh gradient.
   - Vẽ **hoạ tiết** đã tint (alpha thấp, vd 6–12%) **tile** hoặc **scale phủ kín**.
   - Đặt panel này làm nền của vùng chat; `chatHistoryPanel` (`VerticalScrollablePanel`) set **non-opaque**; viewport của `ChatHistoryView` cũng non-opaque để lộ nền.
4. **Tint runtime**: nạp PNG đơn sắc → tô màu theo theme bằng `AlphaComposite`/filter (DRY: 1 ảnh, 2 theme).
5. **Chọn wallpaper UI**: mục trong `UserSettingsDialog` (cùng khu Appearance): lưới thumbnail + lựa chọn "Ngẫu nhiên" / "Không nền". Đổi → `WallpaperManager.set()` → repaint vùng chat (không cần rebuild cả cửa sổ).

### 2.3 ⚠️ Rủi ro tích hợp (QUAN TRỌNG)
- Hiện `chatHistoryPanel` và viewport đều `setBackground(BG_PRIMARY)` **opaque** → che mất nền. Cần set **non-opaque** chuỗi: viewport → `VerticalScrollablePanel` → các strut. **Bong bóng tin nhắn** (`ChatMessageItem`) phải có nền riêng (bubble) để chữ vẫn đọc được trên hoạ tiết.
- Phải đảm bảo **hiệu năng**: tile bằng `TexturePaint` (rẻ) thay vì vẽ lặp thủ công; cache ảnh đã tint.
- Hoạ tiết raster cắt từ sheet **không tile liền mạch** → ưu tiên **scale phủ kín** hoặc dùng SVG.

### 2.4 Các file (Phần 2)
**Tạo mới:** `gui/theme/WallpaperManager.java`, `gui/components/chat/ChatBackgroundPanel.java`, thư mục `resources/wallpapers/`.
**Sửa:** `gui/chat/ChatHistoryView.java` (đặt nền + non-opaque), `gui/ChatClientGUI.java` (gắn `ChatBackgroundPanel` sau `chatHistoryView`), `gui/profile/UserSettingsDialog.java` (mục chọn wallpaper), kiểm tra `ChatMessageItem` (bong bóng có nền riêng).

---

## PHẦN 3 — CÁC BƯỚC TRIỂN KHAI (PHASES)

### Phase 1 — Refactor AppColors thành theme-able
1. Tạo `Theme`, `ThemePalette`, palette DARK (giá trị hiện tại) + LIGHT.
2. Đổi `AppColors` final→mutable + `apply(palette)`; mặc định nạp DARK (không đổi hành vi).
3. `mvnw -pl client-app compile` → đảm bảo build pass, app vẫn y hệt.

### Phase 2 — ThemeManager + chuyển theme
1. `ThemeManager` (Preferences).
2. `ClientApplication`: init theme lúc startup + `restartChatWindow()`.
3. Toggle trong `UserSettingsDialog` → đổi + rebuild.
4. Test thủ công: đổi Sáng/Tối, đóng/mở app nhớ lựa chọn, kiểm tra các màn (chat, sidebar, dialog).

### Phase 3 — Chuẩn bị asset wallpaper
1. Chọn ~6–10 hoạ tiết từ sheet, export PNG đơn sắc+alpha vào `resources/wallpapers/`.
2. (Tuỳ chọn) tối ưu kích thước/nén.

### Phase 4 — ChatBackgroundPanel + tint + tile
1. `WallpaperManager` + `ChatBackgroundPanel` (gradient + pattern tint).
2. Gắn nền vào vùng chat, set non-opaque chuỗi component, đảm bảo bong bóng có nền.
3. Test: bật/tắt nền, đổi theme → hoạ tiết tint đúng, chữ vẫn đọc rõ.

### Phase 5 — Chọn wallpaper UI + Random
1. Lưới chọn wallpaper + "Ngẫu nhiên"/"Không nền" trong `UserSettingsDialog`.
2. Đổi → repaint (không rebuild).

### Phase 6 — Test & review & docs
1. `tester`/manual: light/dark + wallpaper trên các màn chính.
2. `code-reviewer`.
3. Cập nhật `changelog_summary.md`.

---

## PHẦN 4 — TIÊU CHÍ HOÀN THÀNH
- [ ] Đổi Sáng/Tối tức thì (rebuild), nhớ giữa các phiên; mọi màn hợp lệ (không còn mảng tối/sáng lạc).
- [ ] `AppColors` theme-able mà KHÔNG phải sửa call-site đại trà.
- [ ] Nền chat hiển thị hoạ tiết, tint theo theme, chữ đọc rõ, hiệu năng mượt.
- [ ] Chọn wallpaper + chế độ Ngẫu nhiên + Không nền; nhớ lựa chọn.
- [ ] Build `client-app` pass; review không lỗi nghiêm trọng.

---

## PHẦN 5 — CÂU HỎI / QUYẾT ĐỊNH CẦN CHỐT
1. **Asset wallpaper:** chốt phương án **A** (subset ~8 PNG đơn sắc, tint runtime) chứ? Hay muốn lấy **SVG gốc** (C) để tile liền mạch? *(Đề xuất: A trước, nâng cấp C nếu cần.)*
2. **Số lượng & danh sách hoạ tiết** muốn dùng (tôi đề xuất tự chọn ~8 cái đa dạng).
3. **Gradient nền:** vẽ bằng code (`GradientPaint`, KISS) hay dùng ảnh gradient từ sheet? *(Đề xuất: code.)*
4. **Random scope:** ngẫu nhiên **mỗi phiên** hay **mỗi cuộc trò chuyện**? *(Đề xuất: mỗi phiên — KISS.)*
5. **SYSTEM/auto theme** (theo OS) — có cần không? *(Đề xuất: KHÔNG, YAGNI.)*

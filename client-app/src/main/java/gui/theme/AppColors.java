package gui.theme;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Bảng màu trung tâm. Các field KHÔNG còn là hằng {@code final}: chúng được nạp lại
 * từ theme đang chọn mỗi khi gọi {@link #apply(Theme)} (giống cơ chế của {@link AppFonts}).
 * Code vẫn dùng {@code AppColors.BG_PRIMARY} như cũ — chỉ cần rebuild UI sau khi đổi theme
 * để các component đọc lại màu mới.
 */
public class AppColors {
    // Backgrounds
    public static Color BG_PRIMARY;     // Main Chat Area
    public static Color BG_SECONDARY;   // Channel/Friend sidebar + panel phải
    public static Color BG_TERTIARY;    // Server rail / Input boxes (sâu nhất)
    public static Color BG_HOVER;       // Sidebar hover
    public static Color BG_ACTIVE;      // Active/selected item
    public static Color BG_FLOATING;    // Floating cards / tooltips / toast
    public static Color BG_MESSAGE_HOVER; // Hover sáng/tối nhẹ trên nền
    public static Color MSG_BUBBLE;       // Box mờ sau tin nhắn (tăng độ đọc khi có wallpaper)

    // Separator
    public static Color SEPARATOR;

    // Text Colors
    public static Color TEXT_NORMAL;
    public static Color TEXT_MUTED;
    public static Color TEXT_WHITE;   // "Chữ nổi bật" trên surface (đảo màu theo theme)
    public static Color TEXT_LINK;
    public static Color TEXT_HEADER;

    // Brand / Semantic / Status — giữ nhất quán giữa 2 theme
    public static Color BRAND_PRIMARY;
    public static Color BRAND_HOVER;
    public static Color BRAND_LINK;
    public static Color SUCCESS;
    public static Color DANGER;
    public static Color WARNING;
    public static Color MSG_HIGHLIGHT_BG;
    public static Color MSG_HIGHLIGHT_BORDER;
    public static Color STATUS_ONLINE;
    public static Color STATUS_IDLE;
    public static Color STATUS_OFFLINE;
    public static Color STATUS_DND;

    // Avatar palette — độc lập theme
    private static final Color[] AVATAR_COLORS = {
            Color.decode("#5865F2"), // Blurple
            Color.decode("#EB459E"), // Fuchsia
            Color.decode("#57F287"), // Green
            Color.decode("#FEE75C"), // Yellow
            Color.decode("#ED4245"), // Red
            Color.decode("#3BA55C"), // Emerald
            Color.decode("#E67E22"), // Orange
    };

    static {
        apply(ThemeManager.get().current());
    }

    private AppColors() {}

    /** Nạp toàn bộ màu theo theme. Gọi lúc khởi động & khi đổi theme. */
    public static void apply(Theme theme) {
        applySharedColors();
        if (theme == Theme.LIGHT) applyLight();
        else applyDark();
    }

    /** Màu brand/semantic/status — chung cho cả 2 theme. */
    private static void applySharedColors() {
        BRAND_PRIMARY = Color.decode("#5B6CFF");
        BRAND_HOVER = Color.decode("#4856E6");
        BRAND_LINK = Color.decode("#4DA6FF");
        SUCCESS = Color.decode("#2FB36B");
        DANGER = Color.decode("#F0484F");
        WARNING = Color.decode("#F0B233");
        TEXT_LINK = Color.decode("#4DA6FF");
        MSG_HIGHLIGHT_BG = new Color(0x5B, 0x6C, 0xFF, 0x1F);
        MSG_HIGHLIGHT_BORDER = Color.decode("#5B6CFF");
        STATUS_ONLINE = Color.decode("#2FB36B");
        STATUS_IDLE = Color.decode("#F0B233");
        STATUS_OFFLINE = Color.decode("#6B7280");
        STATUS_DND = Color.decode("#F0484F");
    }

    /** Theme tối — navy/charcoal (mặc định). */
    private static void applyDark() {
        BG_PRIMARY = Color.decode("#1A1E27");
        BG_SECONDARY = Color.decode("#151921");
        BG_TERTIARY = Color.decode("#0E1116");
        BG_HOVER = Color.decode("#232A38");
        BG_ACTIVE = Color.decode("#2B3344");
        BG_FLOATING = Color.decode("#1D222D");
        BG_MESSAGE_HOVER = new Color(0xFF, 0xFF, 0xFF, 0x0D);
        MSG_BUBBLE = new Color(0x12, 0x16, 0x1F, 0x8C); // navy ~55% phủ lên nền đã blur
        SEPARATOR = Color.decode("#272E3B");
        TEXT_NORMAL = Color.decode("#E6E9EF");          // sáng hơn cho dễ đọc
        TEXT_MUTED = Color.decode("#9AA1AF");
        TEXT_WHITE = Color.decode("#FEFEFE"); // ~trắng nhưng KHÁC Color.WHITE → remap không đụng chữ trắng hard-code
        TEXT_HEADER = Color.decode("#F1F3F7");
    }

    /** Theme sáng. */
    private static void applyLight() {
        BG_PRIMARY = Color.decode("#FFFFFF");
        BG_SECONDARY = Color.decode("#F2F3F5");
        BG_TERTIARY = Color.decode("#E3E5E8");
        BG_HOVER = Color.decode("#E9EAED");
        BG_ACTIVE = Color.decode("#DCDFE4");
        BG_FLOATING = Color.decode("#FFFFFF");
        BG_MESSAGE_HOVER = new Color(0x00, 0x00, 0x00, 0x0D);
        MSG_BUBBLE = new Color(0xFF, 0xFF, 0xFF, 0xA6); // trắng ~65% phủ lên nền đã blur
        SEPARATOR = Color.decode("#D7D9DD");
        TEXT_NORMAL = Color.decode("#1F2329");          // đậm hơn cho dễ đọc
        TEXT_MUTED = Color.decode("#5C636B");
        TEXT_WHITE = Color.decode("#1A1C1E"); // "chữ nổi bật" → đậm trên nền sáng
        TEXT_HEADER = Color.decode("#1A1C1E");
    }

    /**
     * Bản đồ đổi màu {@code from → to} cho các màu nền/chữ trung tính (theme-distinct).
     * Dùng để remap live các component đã set màu lúc dựng, khi đổi theme mà KHÔNG rebuild cửa sổ.
     * Chỉ gồm màu khác biệt giữa 2 theme (brand/semantic/translucent được loại trừ).
     */
    public static Map<Color, Color> buildRemap(Theme from, Theme to) {
        apply(from);
        Color[] a = neutralSnapshot();
        apply(to);
        Color[] b = neutralSnapshot();
        Map<Color, Color> m = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            if (a[i] != null && !a[i].equals(b[i])) m.put(a[i], b[i]);
        }
        return m;
    }

    private static Color[] neutralSnapshot() {
        return new Color[]{
                BG_PRIMARY, BG_SECONDARY, BG_TERTIARY, BG_HOVER, BG_ACTIVE, BG_FLOATING, SEPARATOR,
                TEXT_NORMAL, TEXT_MUTED, TEXT_HEADER, TEXT_WHITE
        };
    }

    /** Màu avatar nhất quán theo username. */
    public static Color avatarColorFor(String name) {
        if (name == null || name.isEmpty()) return BRAND_PRIMARY;
        int idx = Math.abs(name.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[idx];
    }
}

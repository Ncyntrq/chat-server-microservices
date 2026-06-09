package gui.theme;

import java.util.Random;
import java.util.prefs.Preferences;

/**
 * Quản lý hoạ tiết nền chat do người dùng chọn. Lưu/đọc qua {@link Preferences}.
 * Giá trị lưu: "NONE", "RANDOM", hoặc id pattern ("cats" / "starwars" / "sweets").
 * Mirror cấu trúc {@link UiScale} / {@link ThemeManager}.
 */
public final class WallpaperManager {

    public static final String NONE = "NONE";
    public static final String RANDOM = "RANDOM";
    /** Các pattern có sẵn (khớp tên file resources/wallpapers/pattern-<id>-<theme>.png). */
    public static final String[] PATTERNS = { "cats", "starwars", "sweets" };

    private static final WallpaperManager INSTANCE = new WallpaperManager();
    private static final String PREF_KEY = "ui.wallpaper";

    private final Preferences prefs = Preferences.userNodeForPackage(WallpaperManager.class);
    private String selection;
    /** Pattern đã bốc ngẫu nhiên cho phiên hiện tại (cache để không nhảy mỗi lần repaint). */
    private String randomPick;

    private WallpaperManager() {
        this.selection = prefs.get(PREF_KEY, RANDOM);
    }

    public static WallpaperManager get() { return INSTANCE; }

    public String selection() { return selection; }

    public void set(String value) {
        this.selection = value;
        prefs.put(PREF_KEY, value);
        if (RANDOM.equals(value)) randomPick = null; // bốc lại lần sau
    }

    /** Id pattern đang hiệu lực, hoặc null nếu tắt nền. */
    public String activePatternId() {
        if (NONE.equals(selection)) return null;
        if (RANDOM.equals(selection)) {
            if (randomPick == null) {
                randomPick = PATTERNS[new Random().nextInt(PATTERNS.length)];
            }
            return randomPick;
        }
        return selection;
    }

    public boolean isEnabled() {
        return !NONE.equals(selection);
    }
}

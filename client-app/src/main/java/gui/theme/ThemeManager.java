package gui.theme;

import java.util.prefs.Preferences;

/**
 * Quản lý theme sáng/tối do người dùng chọn. Lưu/đọc qua {@link Preferences}
 * để nhớ giữa các phiên. Mirror cấu trúc của {@link UiScale}.
 */
public final class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();
    private static final String PREF_KEY = "ui.theme";

    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private Theme current;

    private ThemeManager() {
        Theme def = Theme.DARK;
        try {
            this.current = Theme.valueOf(prefs.get(PREF_KEY, def.name()));
        } catch (IllegalArgumentException e) {
            this.current = def;
        }
    }

    public static ThemeManager get() { return INSTANCE; }

    public Theme current() { return current; }

    public boolean isDark() { return current == Theme.DARK; }

    /** Đặt theme mới và lưu lại. */
    public void set(Theme theme) {
        this.current = theme;
        prefs.put(PREF_KEY, theme.name());
    }

    /** Đảo theme hiện tại và lưu lại; trả về theme mới. */
    public Theme toggle() {
        set(current.toggled());
        return current;
    }
}

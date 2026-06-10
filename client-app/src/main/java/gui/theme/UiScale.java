package gui.theme;

import java.util.prefs.Preferences;

/**
 * Hệ số zoom do NGƯỜI DÙNG điều chỉnh (chồng thêm lên DPI mà FlatLaf đã tự xử lý).
 * Lưu/đọc qua Preferences để nhớ giữa các phiên.
 */
public final class UiScale {

    private static final UiScale INSTANCE = new UiScale();

    public static final double MIN = 0.8;
    public static final double MAX = 1.6;
    public static final double STEP = 0.1;

    private static final String PREF_KEY = "ui.zoom";

    private final Preferences prefs = Preferences.userNodeForPackage(UiScale.class);
    private double factor;

    private UiScale() {
        this.factor = clamp(prefs.getDouble(PREF_KEY, 1.0));
    }

    public static UiScale get() { return INSTANCE; }

    public double factor() { return factor; }

    /** Đặt hệ số mới (đã clamp) và lưu lại. Trả về hệ số thực sự áp dụng. */
    public double set(double value) {
        factor = clamp(value);
        prefs.putDouble(PREF_KEY, factor);
        return factor;
    }

    public double increase() { return set(factor + STEP); }

    public double decrease() { return set(factor - STEP); }

    public double reset() { return set(1.0); }

    /** Scale 1 giá trị px theo hệ số hiện hành. */
    public int scaled(int px) { return (int) Math.round(px * factor); }

    private static double clamp(double v) {
        // Làm tròn về bội của STEP để tránh trôi số (vd 0.99999)
        double rounded = Math.round(v / STEP) * STEP;
        return Math.max(MIN, Math.min(MAX, rounded));
    }
}

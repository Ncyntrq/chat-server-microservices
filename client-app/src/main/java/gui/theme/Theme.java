package gui.theme;

/** Chế độ giao diện sáng/tối. */
public enum Theme {
    DARK, LIGHT;

    public Theme toggled() {
        return this == DARK ? LIGHT : DARK;
    }
}

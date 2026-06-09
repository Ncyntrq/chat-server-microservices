package gui.profile;

import gui.ChatClientGUI;
import gui.ClientApplication;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import gui.theme.WallpaperManager;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tab "Giao diện": chọn theme Sáng/Tối. Đổi → lưu + dựng lại cửa sổ chat.
 */
public class AppearancePanel extends JPanel {

    public AppearancePanel(JDialog dialog) {
        setLayout(new GridBagLayout());
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel("Chế độ hiển thị");
        title.setFont(AppFonts.HEADING_SM);
        title.setForeground(AppColors.TEXT_HEADER);
        add(title, gc);

        ButtonGroup group = new ButtonGroup();
        JRadioButton darkBtn = themeRadio("🌙  Tối", Theme.DARK, group, dialog);
        JRadioButton lightBtn = themeRadio("☀  Sáng", Theme.LIGHT, group, dialog);

        Theme current = ThemeManager.get().current();
        darkBtn.setSelected(current == Theme.DARK);
        lightBtn.setSelected(current == Theme.LIGHT);

        gc.gridy = 1; gc.insets = new Insets(0, 0, 8, 0);
        add(darkBtn, gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 24, 0);
        add(lightBtn, gc);

        // --- Hoạ tiết nền chat ---
        JLabel wpTitle = new JLabel("Hoạ tiết nền chat");
        wpTitle.setFont(AppFonts.HEADING_SM);
        wpTitle.setForeground(AppColors.TEXT_HEADER);
        gc.gridy = 3; gc.insets = new Insets(0, 0, 12, 0);
        add(wpTitle, gc);

        gc.gridy = 4; gc.insets = new Insets(0, 0, 0, 0);
        add(buildWallpaperSelector(dialog), gc);
    }

    /** Combo chọn hoạ tiết nền: Ngẫu nhiên / Không nền / từng pattern. */
    private JComboBox<String> buildWallpaperSelector(JDialog dialog) {
        Map<String, String> options = new LinkedHashMap<>(); // label -> value
        options.put("Ngẫu nhiên", WallpaperManager.RANDOM);
        options.put("Không nền", WallpaperManager.NONE);
        options.put("Mèo", "cats");
        options.put("Star Wars", "starwars");
        options.put("Kẹo ngọt", "sweets");

        JComboBox<String> combo = new JComboBox<>(options.keySet().toArray(new String[0]));
        combo.setFont(AppFonts.BODY);
        // Chọn sẵn theo cấu hình hiện tại
        String current = WallpaperManager.get().selection();
        options.entrySet().stream()
                .filter(e -> e.getValue().equals(current))
                .findFirst()
                .ifPresent(e -> combo.setSelectedItem(e.getKey()));

        combo.addActionListener(e -> {
            String label = (String) combo.getSelectedItem();
            String value = options.get(label);
            if (value == null) return;
            WallpaperManager.get().set(value);
            Window owner = dialog.getOwner();
            if (owner instanceof ChatClientGUI chat) chat.refreshChatBackground();
        });
        return combo;
    }

    private JRadioButton themeRadio(String text, Theme theme, ButtonGroup group, JDialog dialog) {
        JRadioButton btn = new JRadioButton(text);
        btn.setFont(AppFonts.BODY);
        btn.setForeground(AppColors.TEXT_NORMAL);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        group.add(btn);
        btn.addActionListener(e -> applyTheme(theme, dialog));
        return btn;
    }

    private void applyTheme(Theme theme, JDialog dialog) {
        if (theme == ThemeManager.get().current()) return;
        ThemeManager.get().set(theme);
        ClientApplication.applyThemeLive(theme); // đổi tại chỗ, dialog tự được re-theme
    }
}

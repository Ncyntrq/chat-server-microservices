package gui.components.friends;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.FriendApiClient;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dialog "Thêm bạn": tìm người dùng theo username/displayName (debounce ~300ms),
 * hiển thị trạng thái quan hệ và nút hành động phù hợp (Kết bạn / Chấp nhận / Bạn bè).
 */
public class AddFriendDialog extends JDialog {

    private final FriendApiClient friendApi = new FriendApiClient();
    private final UserProfileApiClient userApi = new UserProfileApiClient();
    private final String sessionUsername;
    private final Consumer<String> onFriendAction;

    private final JTextField searchField = new JTextField();
    private final JPanel resultsPanel = new JPanel();
    private final Timer debounce;

    private Set<String> friends = Set.of();
    private Set<String> incomingPending = Set.of();
    private final java.util.Set<String> sentRequests = new java.util.HashSet<>();

    public AddFriendDialog(JFrame owner, String sessionUsername, Consumer<String> onFriendAction) {
        super(owner, "Thêm bạn", false);
        this.sessionUsername = sessionUsername;
        this.onFriendAction = onFriendAction;

        this.debounce = new Timer(300, e -> runSearch());
        this.debounce.setRepeats(false);

        setSize(420, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppColors.BG_PRIMARY);

        add(buildHeader(), BorderLayout.NORTH);
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(AppColors.BG_PRIMARY);
        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        gui.theme.ThinScrollBarUI.apply(scroll);
        add(scroll, BorderLayout.CENTER);

        showHint("Nhập username hoặc tên hiển thị để tìm bạn…");
        loadRelationships();
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppColors.BG_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        searchField.setFont(AppFonts.BODY);
        searchField.setBackground(AppColors.BG_TERTIARY);
        searchField.setForeground(AppColors.TEXT_NORMAL);
        searchField.setCaretColor(AppColors.TEXT_WHITE);
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        searchField.getDocument().addDocumentListener(new SimpleDocListener(debounce::restart));
        header.add(searchField, BorderLayout.CENTER);
        return header;
    }

    /** Nạp danh sách bạn + lời mời đến để xác định trạng thái quan hệ. */
    private void loadRelationships() {
        new SwingWorker<Set<String>[], Void>() {
            @Override @SuppressWarnings("unchecked")
            protected Set<String>[] doInBackground() {
                return new Set[]{ Set.copyOf(friendApi.getFriends()), Set.copyOf(friendApi.getPendingRequests()) };
            }
            @Override protected void done() {
                try {
                    Set<String>[] r = get();
                    friends = r[0];
                    incomingPending = r[1];
                    // Render lại để nút phản ánh đúng trạng thái nếu kết quả đã hiển thị trước đó
                    if (searchField.getText().trim().length() >= 2) runSearch();
                } catch (Exception ignore) {
                    // giữ nguyên các set trước đó nếu lỗi
                }
            }
        }.execute();
    }

    private void runSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.length() < 2) { showHint("Nhập ít nhất 2 ký tự để tìm kiếm…"); return; }
        showHint("Đang tìm…");
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override protected List<Map<String, Object>> doInBackground() { return userApi.searchUsers(keyword); }
            @Override protected void done() {
                try { renderResults(get()); }
                catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    showHint("Lỗi tìm kiếm: " + c.getMessage());
                }
            }
        }.execute();
    }

    private void renderResults(List<Map<String, Object>> users) {
        resultsPanel.removeAll();
        if (users.isEmpty()) { showHint("Không tìm thấy người dùng nào."); return; }
        for (Map<String, Object> u : users) {
            String username = String.valueOf(u.get("username"));
            if (username.equalsIgnoreCase(sessionUsername)) continue; // phòng hờ
            resultsPanel.add(buildUserRow(username, str(u.get("displayName"))));
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JComponent buildUserRow(String username, String displayName) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(AppColors.BG_PRIMARY);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        String name = (displayName != null && !displayName.isBlank()) ? displayName : username;
        JLabel label = new JLabel("<html><b>" + esc(name) + "</b><br><span style='color:#8B92A0;'>@"
                + esc(username) + "</span></html>");
        label.setFont(AppFonts.BODY);
        label.setForeground(AppColors.TEXT_NORMAL);
        row.add(label, BorderLayout.CENTER);
        row.add(buildActionButton(username), BorderLayout.EAST);
        return row;
    }

    private JButton buildActionButton(String username) {
        if (friends.contains(username)) return actionButton("Bạn bè", AppColors.STATUS_OFFLINE, null);
        if (incomingPending.contains(username)) {
            return actionButton("Chấp nhận", AppColors.SUCCESS, b -> doAction(username, true, b));
        }
        if (sentRequests.contains(username)) return actionButton("Đã gửi", AppColors.STATUS_OFFLINE, null);
        return actionButton("Kết bạn", AppColors.BRAND_PRIMARY, b -> doAction(username, false, b));
    }

    private void doAction(String username, boolean accept, JButton sourceButton) {
        sourceButton.setEnabled(false);
        sourceButton.setText("...");
        new SwingWorker<Void, Void>() {
            Exception error;
            @Override protected Void doInBackground() {
                try { if (accept) friendApi.acceptRequest(username); else friendApi.sendRequest(username); }
                catch (Exception ex) { error = ex; }
                return null;
            }
            @Override protected void done() {
                if (error != null) {
                    sourceButton.setEnabled(true);
                    sourceButton.setText(accept ? "Chấp nhận" : "Kết bạn");
                    gui.components.feedback.AppDialogs.showError(AddFriendDialog.this, "Lỗi", error.getMessage());
                    return;
                }
                if (onFriendAction != null) onFriendAction.accept(username);
                if (!accept) {
                    sentRequests.add(username);
                    sourceButton.setText("Đã gửi");
                    sourceButton.setBackground(AppColors.STATUS_OFFLINE);
                } else {
                    sourceButton.setText("Bạn bè");
                    sourceButton.setBackground(AppColors.STATUS_OFFLINE);
                }
                loadRelationships();
            }
        }.execute();
    }

    private JButton actionButton(String text, Color bg, Consumer<JButton> onClick) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(AppFonts.CAPTION_BOLD);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        if (onClick != null) {
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> onClick.accept(b));
        } else {
            b.setEnabled(false);
        }
        return b;
    }

    private void showHint(String text) {
        resultsPanel.removeAll();
        JLabel hint = new JLabel(text);
        hint.setFont(AppFonts.BODY);
        hint.setForeground(AppColors.TEXT_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsPanel.add(hint);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record SimpleDocListener(Runnable onChange) implements javax.swing.event.DocumentListener {
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }
}

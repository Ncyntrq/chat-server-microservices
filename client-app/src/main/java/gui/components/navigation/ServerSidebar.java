package gui.components.navigation;

import gui.server.CreateServerDialog;
import gui.server.JoinServerDialog;
import gui.server.ServerSettingsDialog;
import gui.theme.AppColors;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * Thanh server bên trái (72px). Load danh sách server từ API và render động.
 * - Click icon server → callback onServerSelected(serverId)
 * - Right-click icon → mở ServerSettingsDialog
 * - Nút ➕ → popup Tạo / Tham gia server
 */
public class ServerSidebar extends JPanel {

    private final JPanel listPanel;
    private final ServerApiClient serverApi = new ServerApiClient();
    private final Map<Long, ServerIconItem> serverItems = new java.util.HashMap<>();
    private final Map<Long, String> serverNames = new java.util.HashMap<>();
    private final Map<Long, String> serverIconUrls = new java.util.HashMap<>();
    // Cache unread cuối cùng — re-apply sau rebuild để badge không mất khi điều hướng
    private final Map<Long, Integer> lastUnread = new java.util.HashMap<>();

    private BiConsumer<Long, String> onServerSelected;
    private java.util.function.Consumer<Long> onServerChanged;
    private long activeServerId = -1;
    private String sessionUsername;

    public void setSessionUsername(String sessionUsername) {
        this.sessionUsername = sessionUsername;
    }

    public void setOnServerSelected(BiConsumer<Long, String> onServerSelected) {
        this.onServerSelected = onServerSelected;
    }

    public void setOnServerChanged(java.util.function.Consumer<Long> onServerChanged) {
        this.onServerChanged = onServerChanged;
    }

    public ServerSidebar() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_TERTIARY);
        setPreferredSize(new Dimension(72, 0));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_TERTIARY);

        add(listPanel, BorderLayout.CENTER);
        renderServers(List.of()); // khung rỗng ban đầu (HOME + ➕)
    }

    /** Tải danh sách server từ API rồi render lại. */
    public void loadServers() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return serverApi.getMyServers();
            }

            @Override
            protected void done() {
                try {
                    renderServers(get());
                } catch (Exception ex) {
                    renderServers(List.of());
                }
            }
        }.execute();
    }

    /** Render lại toàn bộ: HOME, separator, từng server, nút ➕. */
    private void renderServers(List<Map<String, Object>> servers) {
        listPanel.removeAll();
        serverItems.clear();
        serverNames.clear();
        serverIconUrls.clear();

        listPanel.add(Box.createVerticalStrut(10));
        ServerIconItem homeBtn = new ServerIconItem("💬");
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setActive(activeServerId == -1);
        homeBtn.setOnClick(() -> {
            activeServerId = -1;
            if (onServerSelected != null) onServerSelected.accept(-1L, null);
            refreshActiveStates();
        });
        listPanel.add(homeBtn);

        listPanel.add(Box.createVerticalStrut(5));
        JPanel separator = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(32, 2); }
            @Override public Dimension getMinimumSize()   { return new Dimension(32, 2); }
            @Override public Dimension getMaximumSize()   { return new Dimension(32, 2); }
        };
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);
        separator.setBackground(AppColors.BG_HOVER);
        listPanel.add(separator);
        listPanel.add(Box.createVerticalStrut(5));

        for (Map<String, Object> server : servers) {
            long id = asLong(server.get("id"));
            String name = str(server.get("name"));
            String iconUrl = str(server.get("icon"));
            serverNames.put(id, name);
            serverIconUrls.put(id, iconUrl);
            String symbol = (name == null || name.isBlank()) ? "?" : name.substring(0, 1).toUpperCase();

            ServerIconItem item = new ServerIconItem(symbol);
            serverItems.put(id, item);
            item.setAlignmentX(Component.CENTER_ALIGNMENT);
            item.setActive(id == activeServerId);

            if (iconUrl != null && !iconUrl.isBlank()) {
                if (!iconUrl.startsWith("http")) iconUrl = network.ApiConfig.GATEWAY_HTTP + iconUrl;
                item.loadServerIconFromUrl(iconUrl);
            }

            item.setOnClick(() -> {
                activeServerId = id;
                if (onServerSelected != null) onServerSelected.accept(id, name);
                refreshActiveStates();
            });
            item.setOnContextMenu(() -> showServerContextMenu(item, id, name));
            listPanel.add(item);
        }

        listPanel.add(Box.createVerticalStrut(5));
        ServerIconItem addBtn = new ServerIconItem("➕");
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setOnClick(() -> showAddServerMenu(addBtn));
        listPanel.add(addBtn);

        listPanel.add(Box.createVerticalGlue());
        applyUnread(); // re-apply badge unread đã cache lên item vừa rebuild
        listPanel.revalidate();
        listPanel.repaint();

        // Notify ChatClientGUI of the latest name if a server is currently active
        if (activeServerId != -1 && onServerSelected != null) {
            for (Map<String, Object> server : servers) {
                if (asLong(server.get("id")) == activeServerId) {
                    onServerSelected.accept(activeServerId, str(server.get("name")));
                    break;
                }
            }
        }
    }

    public void updateUnreadCounts(Map<Long, Integer> unreadCounts) {
        SwingUtilities.invokeLater(() -> {
            lastUnread.clear();
            lastUnread.putAll(unreadCounts);
            applyUnread();
        });
    }

    // --- ĐÃ THÊM HÀM NÀY ĐỂ BÊN NGOÀI GỌI ---
    public void selectHome() {
        this.activeServerId = -1;
        refreshActiveStates();
    }

    public String getServerName(long id) {
        return serverNames.get(id);
    }

    public String getServerIconUrl(long id) {
        return serverIconUrls.get(id);
    }

    /** Áp dụng unread đã cache lên các icon server hiện có. */
    private void applyUnread() {
        for (Map.Entry<Long, ServerIconItem> entry : serverItems.entrySet()) {
            Integer count = lastUnread.get(entry.getKey());
            entry.getValue().setUnreadCount(count != null ? count : 0);
        }
    }

    private void refreshActiveStates() {
        loadServers();
    }

    private void showAddServerMenu(Component anchor) {
        gui.components.dropdown.AppDropdown menu = new gui.components.dropdown.AppDropdown();
        Window owner = SwingUtilities.getWindowAncestor(this);
        
        menu.add(new gui.components.dropdown.AppDropdownItem("Create Server", e ->
                new CreateServerDialog(owner, () -> {
                    loadServers();
                    if (onServerChanged != null) onServerChanged.accept(-1L);
                }).setVisible(true)));
                
        menu.add(new gui.components.dropdown.AppDropdownItem("Join Server", e ->
                new JoinServerDialog(owner, (joinedServerId) -> {
                    loadServers();
                    activeServerId = joinedServerId;
                    if (onServerSelected != null) onServerSelected.accept(joinedServerId, getServerName(joinedServerId));
                }).setVisible(true)));

        menu.show(anchor, anchor.getWidth(), 0);
    }

    private void showServerContextMenu(Component anchor, long serverId, String serverName) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    List<Map<String, Object>> muted = new network.NotificationApiClient().getMutedTargets(sessionUsername);
                    for (Map<String, Object> m : muted) {
                        if ("SERVER".equals(m.get("targetType")) && String.valueOf(serverId).equals(m.get("targetId"))) {
                            return true;
                        }
                    }
                } catch (Exception ignore) {}
                return false;
            }
            @Override protected void done() {
                boolean isMuted = false;
                try { isMuted = get(); } catch (Exception ignore) {}
                showServerContextMenuWithMuteState(anchor, serverId, serverName, isMuted);
            }
        }.execute();
    }

    private void showServerContextMenuWithMuteState(Component anchor, long serverId, String serverName, boolean isMuted) {
        gui.components.dropdown.AppDropdown menu = new gui.components.dropdown.AppDropdown();
        
        menu.add(new gui.components.dropdown.AppDropdownItem("Server Settings", e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new gui.server.UnifiedServerSettingsDialog(owner, serverId, () -> {
                loadServers();
                if (onServerChanged != null) onServerChanged.accept(serverId);
            }).setVisible(true);
        }));
        
        menu.add(new gui.components.dropdown.AppDropdownItem("Create Channel", e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new gui.channel.CreateChannelDialog(owner, serverId, () -> {
                if (onServerChanged != null) onServerChanged.accept(serverId);
            }).setVisible(true);
        }));

        menu.add(new gui.components.dropdown.AppDropdownItem("Invite People", e -> {
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    return new network.ServerApiClient().createInviteCode(serverId);
                }
                @Override protected void done() {
                    try {
                        String code = get();
                        Window owner = SwingUtilities.getWindowAncestor(ServerSidebar.this);
                        new gui.server.InviteCodeDialog(owner, code).setVisible(true);
                    } catch (Exception ex) {
                        gui.components.feedback.AppDialogs.showError(ServerSidebar.this, "Lỗi", "Error generating invite: " + ex.getMessage());
                    }
                }
            }.execute();
        }));

        menu.add(new gui.components.dropdown.AppDropdownItem(isMuted ? "Unmute Server" : "Mute Server", e -> {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    new network.NotificationApiClient().toggleMute(sessionUsername, "SERVER", String.valueOf(serverId), !isMuted);
                    return null;
                }
                @Override protected void done() {
                    if (onServerChanged != null) onServerChanged.accept(serverId);
                }
            }.execute();
        }));

        menu.addSeparator();

        menu.add(new gui.components.dropdown.AppDropdownItem("Leave Server", AppColors.DANGER, AppColors.DANGER, e -> {
            boolean confirm = gui.components.feedback.AppDialogs.showConfirm(this,
                    "Confirm", "Are you sure you want to leave this server?");
            if (!confirm) return;
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    new network.ServerApiClient().leaveServer(serverId);
                    return null;
                }
                @Override protected void done() {
                    loadServers();
                    if (activeServerId == serverId) {
                        activeServerId = -1;
                        if (onServerSelected != null) onServerSelected.accept(-1L, null);
                    }
                }
            }.execute();
        }));

        menu.show(anchor, anchor.getWidth(), 0);
    }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o != null) {
            try { return Long.parseLong(o.toString()); } catch (NumberFormatException ignore) {}
        }
        return -1;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
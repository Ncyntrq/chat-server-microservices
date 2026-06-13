package gui.components.channels;

import gui.channel.CreateChannelDialog;
import gui.channel.EditChannelDialog;
import gui.components.chat.IconButton;
import gui.components.chat.SidebarCategoryHeader;
import gui.theme.AppColors;
import network.ApiConfig;
import network.ChannelApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public class ChannelSidebar extends JPanel {

    private final ChannelApiClient channelApi = new ChannelApiClient();
    private final JLabel titleLabel;
    private final JPanel listPanel;
    private final java.util.Map<Long, String> channelNames = new java.util.HashMap<>();
    private final Map<Long, ChannelListItem> channelItems = new java.util.HashMap<>();
    // Cache unread cuối cùng — re-apply sau mỗi lần rebuild để badge không mất khi điều hướng
    private final Map<Long, Integer> lastUnread = new java.util.HashMap<>();
    private UserFooterPanel accountFooter;
    private final String sessionUsername;

    private long activeServerId = -1;
    private LongConsumer onChannelSelected;
    private Runnable onChannelChanged;
    private Runnable onUserChanged;
    
    private boolean isServerMuted = false;
    private final java.util.List<Long> mutedChannels = new java.util.ArrayList<>();

    public void setOnChannelSelected(LongConsumer onChannelSelected) {
        this.onChannelSelected = onChannelSelected;
    }

    public void setOnChannelChanged(Runnable onChannelChanged) {
        this.onChannelChanged = onChannelChanged;
    }

    public void setOnUserChanged(Runnable onUserChanged) {
        this.onUserChanged = onUserChanged;
    }

    public long getActiveServerId() { return activeServerId; }

    public String getChannelName(long channelId) {
        return channelNames.get(channelId);
    }

    public ChannelSidebar(String sessionUsername) {
        this.sessionUsername = sessionUsername;
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);
        setPreferredSize(new Dimension(240, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.BG_SECONDARY);
        headerPanel.setPreferredSize(new Dimension(240, 48));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_PRIMARY));

        titleLabel = new JLabel("Select a server ⏷");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(AppColors.TEXT_WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        
        // Make header clickable for dropdown
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeServerId > 0) {
                    showServerDropdown(headerPanel);
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(AppColors.BG_TERTIARY);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(AppColors.BG_SECONDARY);
            }
        });
        headerPanel.add(titleLabel, BorderLayout.CENTER);



        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel scrollContentWrapper = new JPanel(new BorderLayout());
        scrollContentWrapper.setBackground(AppColors.BG_SECONDARY);
        scrollContentWrapper.add(listPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollContentWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        accountFooter = new UserFooterPanel(sessionUsername, () -> {
            if (onUserChanged != null) onUserChanged.run();
        });

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);
    }

    public void refreshUserFooter() {
        if (accountFooter != null) {
            accountFooter.refreshAvatar();
        }
    }

    public void loadChannels(long serverId, String serverName) {
        this.activeServerId = serverId;
        if (serverName != null) titleLabel.setText(serverName + " ⏷");

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                List<Map<String, Object>> channels = channelApi.getChannelsByServer(serverId);
                try {
                    List<Map<String, Object>> muted = new network.NotificationApiClient().getMutedTargets(sessionUsername);
                    isServerMuted = false;
                    mutedChannels.clear();
                    for (Map<String, Object> m : muted) {
                        String type = (String) m.get("targetType");
                        String idStr = (String) m.get("targetId");
                        if ("SERVER".equals(type) && String.valueOf(serverId).equals(idStr)) {
                            isServerMuted = true;
                        } else if ("CHANNEL".equals(type)) {
                            mutedChannels.add(Long.parseLong(idStr));
                        }
                    }
                } catch (Exception ignore) {}
                return channels;
            }

            @Override
            protected void done() {
                try {
                    renderChannels(get());
                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Failed to load channels");
                    err.setForeground(AppColors.TEXT_MUTED);
                    listPanel.add(err);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            }
        }.execute();
    }

    private void renderChannels(List<Map<String, Object>> channels) {
        listPanel.removeAll();
        channelItems.clear();
        
        String cleanTitle = titleLabel.getText().replace(" 🔕 ⏷", "").replace(" ⏷", "");
        titleLabel.setText(cleanTitle + (isServerMuted ? " 🔕 ⏷" : " ⏷"));

        boolean addedTextHeader = false;
        boolean addedVoiceHeader = false;

        for (Map<String, Object> ch : channels) {
            if (!"VOICE".equalsIgnoreCase(str(ch.get("type")))) {
                if (!addedTextHeader) {
                    listPanel.add(new SidebarCategoryHeader("TEXT CHANNELS", () -> {
                        Window owner = SwingUtilities.getWindowAncestor(this);
                        new CreateChannelDialog(owner, activeServerId,
                                () -> {
                                    loadChannels(activeServerId, titleLabel.getText());
                                    if (onChannelChanged != null) onChannelChanged.run();
                                }).setVisible(true);
                    }));
                    listPanel.add(Box.createVerticalStrut(4));
                    addedTextHeader = true;
                }
                listPanel.add(buildItem(ch, false));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        for (Map<String, Object> ch : channels) {
            if ("VOICE".equalsIgnoreCase(str(ch.get("type")))) {
                if (!addedVoiceHeader) {
                    listPanel.add(Box.createVerticalStrut(12));
                    listPanel.add(new SidebarCategoryHeader("VOICE CHANNELS"));
                    listPanel.add(Box.createVerticalStrut(4));
                    addedVoiceHeader = true;
                }
                listPanel.add(buildItem(ch, true));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        if (!addedTextHeader && !addedVoiceHeader) {
            listPanel.add(new SidebarCategoryHeader("TEXT CHANNELS", () -> {
                Window owner = SwingUtilities.getWindowAncestor(this);
                new CreateChannelDialog(owner, activeServerId,
                        () -> {
                            loadChannels(activeServerId, titleLabel.getText());
                            if (onChannelChanged != null) onChannelChanged.run();
                        }).setVisible(true);
            }));
            listPanel.add(Box.createVerticalStrut(4));
            JLabel empty = new JLabel("No channels yet");
            empty.setForeground(AppColors.TEXT_MUTED);
            listPanel.add(empty);
        }

        applyUnread(); // re-apply badge unread đã cache lên item vừa rebuild
        listPanel.revalidate();
        listPanel.repaint();
    }

    public void updateUnreadCounts(Map<Long, Integer> unreadCounts) {
        SwingUtilities.invokeLater(() -> {
            lastUnread.clear();
            lastUnread.putAll(unreadCounts);
            applyUnread();
        });
    }

    /** Áp dụng unread đã cache lên các item hiện có (giữ badge qua các lần rebuild). */
    private void applyUnread() {
        for (Map.Entry<Long, ChannelListItem> entry : channelItems.entrySet()) {
            Integer count = lastUnread.get(entry.getKey());
            entry.getValue().setUnreadCount(count != null ? count : 0);
        }
    }

    private ChannelListItem buildItem(Map<String, Object> ch, boolean isVoice) {
        long id = asLong(ch.get("id"));
        String name = str(ch.get("name"));
        String topic = str(ch.get("topic"));
        if (name != null) channelNames.put(id, name);
        ChannelListItem item = new ChannelListItem(name != null ? name : "channel", isVoice);
        item.setMuted(mutedChannels.contains(id));
        item.setOnClick(() -> {
            if (onChannelSelected != null) onChannelSelected.accept(id);
        });
        item.setOnContextMenu(() -> showChannelMenu(item, id, name, topic));
        
        channelItems.put(id, item);
        return item;
    }

    private void showServerDropdown(Component anchor) {
        gui.components.dropdown.AppDropdown menu = new gui.components.dropdown.AppDropdown();
        
        menu.add(new gui.components.dropdown.AppDropdownItem("Server Settings", e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new gui.server.UnifiedServerSettingsDialog(owner, activeServerId, () -> {
                if (onChannelChanged != null) onChannelChanged.run();
            }).setVisible(true);
        }));
        
        menu.add(new gui.components.dropdown.AppDropdownItem("Create Channel", e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new CreateChannelDialog(owner, activeServerId, () -> {
                loadChannels(activeServerId, titleLabel.getText().replace(" ⏷", ""));
                if (onChannelChanged != null) onChannelChanged.run();
            }).setVisible(true);
        }));

        menu.add(new gui.components.dropdown.AppDropdownItem("Invite People", e -> {
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    return new network.ServerApiClient().createInviteCode(activeServerId);
                }
                @Override protected void done() {
                    try {
                        String code = get();
                        Window owner = SwingUtilities.getWindowAncestor(ChannelSidebar.this);
                        new gui.server.InviteCodeDialog(owner, code).setVisible(true);
                    } catch (Exception ex) {
                        gui.components.feedback.AppDialogs.showError(ChannelSidebar.this, "Lỗi", "Error generating invite: " + ex.getMessage());
                    }
                }
            }.execute();
        }));

        menu.add(new gui.components.dropdown.AppDropdownItem(isServerMuted ? "Unmute Server" : "Mute Server", e -> {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    new network.NotificationApiClient().toggleMute(sessionUsername, "SERVER", String.valueOf(activeServerId), !isServerMuted);
                    return null;
                }
                @Override protected void done() {
                    loadChannels(activeServerId, titleLabel.getText().replace(" 🔕 ⏷", "").replace(" ⏷", ""));
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
                    new network.ServerApiClient().leaveServer(activeServerId);
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        if (onChannelChanged != null) onChannelChanged.run();
                    } catch (Exception ex) {
                        gui.components.feedback.AppDialogs.showError(ChannelSidebar.this, "Error leaving server: " + ex.getMessage());
                    }
                }
            }.execute();
        }));
        
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void showChannelMenu(Component anchor, long channelId, String name, String topic) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Channel");
        JMenuItem deleteItem = new JMenuItem("Delete Channel");
        Window owner = SwingUtilities.getWindowAncestor(this);

        boolean isChannelMuted = mutedChannels.contains(channelId);
        JMenuItem muteItem = new JMenuItem(isChannelMuted ? "Unmute Channel" : "Mute Channel");
        
        editItem.addActionListener(e ->
                new EditChannelDialog(owner, channelId, name, topic,
                        () -> {
                            loadChannels(activeServerId, titleLabel.getText().replace(" 🔕 ⏷", "").replace(" ⏷", ""));
                            if (onChannelChanged != null) onChannelChanged.run();
                        }).setVisible(true));

        muteItem.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    new network.NotificationApiClient().toggleMute(sessionUsername, "CHANNEL", String.valueOf(channelId), !isChannelMuted);
                    return null;
                }
                @Override protected void done() {
                    loadChannels(activeServerId, titleLabel.getText().replace(" 🔕 ⏷", "").replace(" ⏷", ""));
                }
            }.execute();
        });

        deleteItem.addActionListener(e -> {
            boolean confirm = gui.components.feedback.AppDialogs.showConfirm(this,
                    "Confirm", "Delete channel \"" + name + "\"?");
            if (!confirm) return;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    channelApi.deleteChannel(channelId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadChannels(activeServerId, titleLabel.getText());
                        if (onChannelChanged != null) onChannelChanged.run();
                    } catch (Exception ex) {
                        gui.components.feedback.AppDialogs.showError(ChannelSidebar.this,
                                "Error deleting channel: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        menu.add(editItem);
        menu.add(muteItem);
        menu.add(deleteItem);
        menu.show(anchor, 0, anchor.getHeight());
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

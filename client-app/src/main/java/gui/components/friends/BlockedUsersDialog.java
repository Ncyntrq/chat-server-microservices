package gui.components.friends;

import gui.components.chat.UserListItem;
import gui.theme.AppColors;
import network.FriendApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BlockedUsersDialog extends JDialog {

    private final JPanel listPanel;
    private final FriendApiClient friendApi = new FriendApiClient();
    private final Runnable onUnblock;

    public BlockedUsersDialog(Window owner, List<String> blockedUsers, Runnable onUnblock) {
        super(owner, "Danh sách đã chặn", ModalityType.APPLICATION_MODAL);
        this.onUnblock = onUnblock;
        
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppColors.BG_PRIMARY);
        setSize(350, 400);
        setLocationRelativeTo(owner);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_PRIMARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel listContainer = new JPanel(new BorderLayout());
        listContainer.setBackground(AppColors.BG_PRIMARY);
        listContainer.add(listPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        gui.theme.ThinScrollBarUI.apply(scrollPane);

        add(scrollPane, BorderLayout.CENTER);

        renderList(blockedUsers);
    }

    private void renderList(List<String> blockedUsers) {
        listPanel.removeAll();
        if (blockedUsers.isEmpty()) {
            JLabel empty = new JLabel("Không có người dùng nào bị chặn.");
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalGlue());
            listPanel.add(empty);
            listPanel.add(Box.createVerticalGlue());
        } else {
            for (String f : blockedUsers) {
                UserListItem item = new UserListItem(f, null, AppColors.STATUS_OFFLINE, false);
                item.setBlocked(true);
                
                // Add an Unblock button to the right
                JButton unblockBtn = new JButton("Bỏ chặn");
                unblockBtn.setFont(gui.theme.AppFonts.BODY_SM);
                unblockBtn.setForeground(Color.WHITE);
                unblockBtn.setBackground(AppColors.DANGER);
                unblockBtn.setFocusPainted(false);
                unblockBtn.setBorderPainted(false);
                unblockBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                unblockBtn.addActionListener(e -> {
                    boolean confirm = gui.components.feedback.AppDialogs.showConfirm(BlockedUsersDialog.this, 
                        "Xác nhận", "Bạn có chắc muốn bỏ chặn " + f + "?");
                    if (!confirm) return;
                    new SwingWorker<Void, Void>() {
                        @Override protected Void doInBackground() {
                            friendApi.unblockUser(f);
                            return null;
                        }
                        @Override protected void done() {
                            if (onUnblock != null) onUnblock.run();
                            dispose();
                        }
                    }.execute();
                });
                
                JPanel wrap = new JPanel(new BorderLayout());
                wrap.setOpaque(false);
                wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
                wrap.add(item, BorderLayout.CENTER);
                
                JPanel btnWrap = new JPanel(new GridBagLayout());
                btnWrap.setOpaque(false);
                btnWrap.add(unblockBtn);
                wrap.add(btnWrap, BorderLayout.EAST);
                
                listPanel.add(wrap);
                listPanel.add(Box.createVerticalStrut(8));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }
}

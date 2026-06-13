package gui.components.feedback;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AppDialogs {
    
    public static void showError(Component parent, String message) {
        showError(parent, "Lỗi", message);
    }

    public static void showError(Component parent, String title, String message) {
        showDialog(parent, title, message, AppColors.DANGER, "Đóng", null, null);
    }
    
    public static void showInfo(Component parent, String title, String message) {
        showDialog(parent, title, message, AppColors.BRAND_PRIMARY, "OK", null, null);
    }
    
    public static boolean showConfirm(Component parent, String title, String message) {
        return showDialog(parent, title, message, AppColors.WARNING, "Hủy", "Xác nhận", AppColors.DANGER);
    }
    
    public static boolean showConfirm(Component parent, String title, String message, String confirmText, Color confirmBg) {
        return showDialog(parent, title, message, confirmBg != null ? confirmBg : AppColors.WARNING, "Hủy", confirmText, confirmBg);
    }
    
    /**
     * @return true if confirmed, false if cancelled
     */
    private static boolean showDialog(Component parent, String titleText, String messageText, Color accentColor, 
                                      String cancelText, String confirmText, Color confirmBg) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window == null && parent instanceof Window) window = (Window) parent;
        
        JDialog dialog = new JDialog(window, titleText, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        
        JPanel contentPane = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BG_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(AppColors.SEPARATOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                
                // Accent top border
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), 6, 16, 16));
                g2.fillRect(0, 6, getWidth(), 6); // cover bottom corners of the accent bar
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPane.setOpaque(false);
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 24, 20, 24));
        
        // Title
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_HEADER);
        
        // Message
        JTextArea message = new JTextArea(messageText);
        message.setFont(AppFonts.BODY_SM);
        message.setForeground(AppColors.TEXT_NORMAL);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setEditable(false);
        message.setOpaque(false);
        message.setFocusable(false);
        message.setBorder(BorderFactory.createEmptyBorder(12, 0, 20, 0));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        
        final boolean[] confirmed = {false};
        
        JButton cancelBtn = new JButton(cancelText);
        cancelBtn.setFont(AppFonts.BODY_BOLD);
        cancelBtn.setForeground(AppColors.TEXT_NORMAL);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        cancelBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { cancelBtn.setForeground(AppColors.TEXT_WHITE); }
            public void mouseExited(MouseEvent e) { cancelBtn.setForeground(AppColors.TEXT_NORMAL); }
        });
        cancelBtn.addActionListener(e -> {
            confirmed[0] = false;
            dialog.dispose();
        });
        
        if (confirmText == null) {
            // If it's just an alert (no confirm button), the single button acts as "OK"
            cancelBtn.setText(cancelText);
            cancelBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { cancelBtn.setForeground(AppColors.BRAND_PRIMARY); }
            });
        }
        
        buttonPanel.add(cancelBtn);
        
        if (confirmText != null) {
            Color btnBg = confirmBg != null ? confirmBg : AppColors.BRAND_PRIMARY;
            JButton confirmBtn = new JButton(confirmText) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isPressed()) {
                        g2.setColor(btnBg.darker());
                    } else if (getModel().isRollover()) {
                        g2.setColor(btnBg.brighter());
                    } else {
                        g2.setColor(btnBg);
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            confirmBtn.setFont(AppFonts.BODY_BOLD);
            confirmBtn.setForeground(Color.WHITE);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setBorderPainted(false);
            confirmBtn.setContentAreaFilled(false);
            confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            confirmBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            confirmBtn.addActionListener(e -> {
                confirmed[0] = true;
                dialog.dispose();
            });
            buttonPanel.add(confirmBtn);
        }
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(message, BorderLayout.CENTER);
        
        contentPane.add(topPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        
        // Window drag logic
        final Point[] initialClick = new Point[1];
        topPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick[0] = e.getPoint(); }
        });
        topPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (initialClick[0] == null) return;
                int thisX = dialog.getLocation().x;
                int thisY = dialog.getLocation().y;
                int xMoved = e.getX() - initialClick[0].x;
                int yMoved = e.getY() - initialClick[0].y;
                dialog.setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setSize(new Dimension(Math.max(400, dialog.getWidth()), dialog.getHeight()));
        dialog.setLocationRelativeTo(window);
        dialog.setVisible(true);
        
        return confirmed[0];
    }
}

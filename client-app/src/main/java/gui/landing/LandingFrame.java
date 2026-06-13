package gui.landing;

import gui.auth.AuthDialog;
import gui.components.PrimaryButton;
import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;

/**
 * Trang giới thiệu (landing) hiển thị khi app mở mà chưa đăng nhập.
 * Có 2 nút CTA: Đăng nhập / Đăng ký → mở AuthDialog ở tab tương ứng.
 */
public class LandingFrame extends JFrame {

    public LandingFrame() {
        setTitle("ChatServer");
        setSize(720, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(80, 60, 60, 60));

        try {
            java.net.URL url = getClass().getResource("/logo/logo.png");
            if (url != null) {
                setIconImage(javax.imageio.ImageIO.read(url));
            }
        } catch (Exception e) {}

        // --- Logo / tên app ---
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        try {
            java.net.URL url = getClass().getResource("/logo/logo.png");
            if (url != null) {
                Image img = javax.imageio.ImageIO.read(url).getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                logoPanel.add(new JLabel(new ImageIcon(img)));
            }
        } catch (Exception e) {}

        JLabel logoText = new JLabel("ChatServer");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 40));
        logoText.setForeground(AppColors.TEXT_WHITE);
        logoPanel.add(logoText);
        root.add(logoPanel);
        root.add(Box.createVerticalStrut(16));

        // --- Mô tả ngắn ---
        JLabel desc = new JLabel("A real-time communication platform built for communities");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 16));
        desc.setForeground(AppColors.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(desc);
        root.add(Box.createVerticalStrut(48));

        // --- CTA buttons ---
        PrimaryButton loginBtn = new PrimaryButton("Log In", e -> openAuth(true));
        loginBtn.setMaximumSize(new Dimension(280, 48));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(loginBtn);
        root.add(Box.createVerticalStrut(14));

        PrimaryButton registerBtn = new PrimaryButton("Register", e -> openAuth(false));
        registerBtn.setMaximumSize(new Dimension(280, 48));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(registerBtn);

        root.add(Box.createVerticalGlue());

        // --- Footer ---
        JLabel footer = new JLabel("Chat Server Microservices • v2.0");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setForeground(AppColors.TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(footer);

        setContentPane(root);
    }

    private void openAuth(boolean loginTab) {
        AuthDialog auth = new AuthDialog(this);
        if (loginTab) auth.showLoginTab();
        else auth.showRegisterTab();
        auth.setVisible(true);
    }
}

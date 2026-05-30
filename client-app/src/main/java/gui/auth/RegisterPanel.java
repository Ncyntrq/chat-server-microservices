package gui.auth;

import gui.components.AuthHeader;
import gui.components.FormField;
import gui.components.PrimaryButton;
import network.ApiException;
import network.AuthApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegisterPanel extends JPanel {

    public RegisterPanel(AuthDialog parent) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(Color.decode("#313338"));

        AuthHeader header = new AuthHeader("Create an account", "A solid platform for real-time messaging");
        FormField usernameField = new FormField("USERNAME", "Enter your new username", false);
        FormField passwordField = new FormField("PASSWORD", "Enter your new password", true);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.decode("#F23F42"));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.setBackground(Color.decode("#313338"));
        statusPanel.add(statusLabel);

        AuthApiClient authClient = new AuthApiClient();
        PrimaryButton registerButton = new PrimaryButton("Register", e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setForeground(Color.decode("#F23F42"));
                statusLabel.setText("Vui lòng nhập username và password");
                return;
            }
            statusLabel.setForeground(Color.decode("#B5BAC1"));
            statusLabel.setText("Đang đăng ký...");

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return authClient.register(user, pass);
                }

                @Override
                protected void done() {
                    try {
                        String msg = get();
                        statusLabel.setForeground(Color.decode("#23A55A"));
                        statusLabel.setText("Đăng ký thành công! " + (msg == null ? "" : msg));
                        // Auto chuyển về tab Login sau 1s
                        Timer t = new Timer(800, ev -> {
                            if (parent != null) parent.showLoginTab();
                        });
                        t.setRepeats(false);
                        t.start();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String err = cause instanceof ApiException
                                ? cause.getMessage()
                                : "Đăng ký thất bại: " + cause.getMessage();
                        statusLabel.setForeground(Color.decode("#F23F42"));
                        statusLabel.setText(err);
                    }
                }
            };
            worker.execute();
        });

        JLabel loginLink = new JLabel("<html><font color='#00A8FC'>Already have an account?</font></html>");
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkPanel.setBackground(Color.decode("#313338"));
        linkPanel.add(loginLink);

        JLabel termsText = new JLabel("By registering, you agree to our Terms of Service and Privacy Policy.");
        termsText.setForeground(Color.decode("#80848E"));
        termsText.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        termsPanel.setBackground(Color.decode("#313338"));
        termsPanel.add(termsText);

        add(header);
        add(Box.createVerticalStrut(30));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(usernameField);
        add(Box.createVerticalStrut(15));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(passwordField);
        add(Box.createVerticalStrut(25));
        add(registerButton);
        add(Box.createVerticalStrut(8));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(statusPanel);
        add(Box.createVerticalStrut(15));
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(linkPanel);
        add(Box.createVerticalStrut(20));
        termsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(termsPanel);

        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (parent != null) parent.showLoginTab();
            }
        });
    }
}

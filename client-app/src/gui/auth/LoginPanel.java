package gui.auth;

import gui.components.AuthHeader;
import gui.components.FormField;
import gui.components.PrimaryButton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginPanel extends JPanel {
    public LoginPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        setBackground(Color.decode("#313338"));

        // Header
        AuthHeader header = new AuthHeader("Welcome back!", "We're excited to see you again");

        // Fields
        FormField usernameField = new FormField("Username", "Enter your username or email", false);
        FormField passwordField = new FormField("Password", "Enter your password", true);

        // Forgot Password Link
        JLabel forgotPass = new JLabel("Forgot your password?");
        forgotPass.setForeground(Color.decode("#00A8FC"));
        forgotPass.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkPanel.setBackground(Color.decode("#313338"));
        linkPanel.add(forgotPass);

        // Login Button
        PrimaryButton loginButton = new PrimaryButton("Log In", e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            System.out.println("Attempting login with: " + user + " / " + pass);
            // TODO: Call ApiClient POST /api/auth/login
        });

        // Register Link
        JLabel registerLink = new JLabel("<html><font color='#B5BAC1'>Need an account?</font> <font color='#00A8FC'>Register</font></html>");
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footerPanel.setBackground(Color.decode("#313338"));
        footerPanel.add(registerLink);

        // ---ASSEMBLE---
        add(header);
        add(Box.createVerticalStrut(30));

        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(usernameField);
        add(Box.createVerticalStrut(15));

        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(passwordField);
        add(Box.createVerticalStrut(5));

        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(linkPanel);
        add(Box.createVerticalStrut(20));

        add(loginButton);
        add(Box.createVerticalStrut(10));

        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(footerPanel);

        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Switching to Register Tab...");
                // We will handle tab switching later!
            }
        });
    }
}

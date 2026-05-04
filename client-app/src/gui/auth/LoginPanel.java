package gui.auth;

import gui.components.FormField;
import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private final FormField usernameField;
    private final FormField passwordField;
    private final JButton loginButton;

    public LoginPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        usernameField = new FormField("Username", false);
        passwordField = new FormField("Password", true);
        loginButton = new JButton("Login");

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa

        add(usernameField);
        add(Box.createVerticalStrut(15));
        add(passwordField);
        add(Box.createVerticalStrut(20));
        add(loginButton);

        // MOCK CLICK EVENT (WILL BE REPLACED WITH API CALL)
        loginButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            System.out.println("Attempting to login with: " + user + " / " + pass);
            // TODO: Replace with api call
        });
    }
}

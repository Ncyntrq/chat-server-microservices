package gui.auth;

import javax.swing.*;

public class AuthDialog extends JDialog {
    public AuthDialog() {
        setTitle("Authentication");
        setModal(true);
        setSize(350, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Login", new LoginPanel());
        tabbedPane.addTab("Register", new RegisterPanel());

        add(tabbedPane);
    }
}

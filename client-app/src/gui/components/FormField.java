package gui.components;

import javax.swing.*;
import java.awt.*;

public class FormField extends JPanel {
    private final JTextField textField;

    public FormField(String labelText, boolean isPassword) {
        setLayout(new BorderLayout(0, 5));

        JLabel label = new JLabel(labelText);

        if (isPassword) {
            textField = new JPasswordField(20);
        } else {
            textField = new JTextField(20);
        }

        add(label, BorderLayout.NORTH);
        add(textField, BorderLayout.CENTER);
    }

    public String getText() {
        return textField.getText();
    }
}

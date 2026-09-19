package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.util.RMIConnector;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;









public class RegisterFrame extends JFrame {

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backButton;
    private JLabel statusLabel;

    public RegisterFrame() {
        super("Register - Lost & Found System");
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(440, 400);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Student Registration");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Full Name:"), gbc);
        nameField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(18);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("Confirm Password:"), gbc);
        confirmPasswordField = new JPasswordField(18);
        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);

        registerButton = new JButton("Register");
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        backButton = new JButton("Back to Welcome Screen");
        gbc.gridy = 6;
        panel.add(backButton, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 7;
        panel.add(statusLabel, gbc);

        registerButton.addActionListener(e -> attemptRegister());

        backButton.addActionListener(e -> {
            new WelcomeFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void attemptRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        setFormEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Registering...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected User doInBackground() {
                try {
                    AuthServiceRemote authService = RMIConnector.getAuthService();
                    
                    return authService.register(name, email, password, Role.STUDENT);
                } catch (RemoteException | NotBoundException | ValidationException | AuthenticationException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                if (failure != null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(failure.getMessage());
                    return;
                }
                try {
                    User user = get();
                    JOptionPane.showMessageDialog(RegisterFrame.this,
                            "Registration successful! You can now log in as " + user.getEmail() + ".",
                            "Registered", JOptionPane.INFORMATION_MESSAGE);
                    new LoginFrame().setVisible(true);
                    dispose();
                } catch (Exception e) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        backButton.setEnabled(enabled);
    }
}

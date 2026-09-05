package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Login screen. Submits credentials to AuthServiceRemote over RMI —
 * this is a genuine network call to the server process (LostFoundServer),
 * not a local method call. The call runs on a SwingWorker background
 * thread so the UI never freezes while waiting on the network.
 */
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton backButton;
    private JLabel statusLabel;

    public LoginFrame() {
        super("Login - Lost & Found System");
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Login");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(18);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        loginButton = new JButton("Login");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        backButton = new JButton("Back to Welcome Screen");
        gbc.gridy = 4;
        panel.add(backButton, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        panel.add(statusLabel, gbc);

        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin()); // Enter key submits

        backButton.addActionListener(e -> {
            new WelcomeFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void attemptLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Email and password are required.");
            return;
        }

        setFormEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Connecting to server...");

        // Network call happens off the Event Dispatch Thread so the
        // window stays responsive while RMI does its round trip.
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected User doInBackground() {
                try {
                    AuthServiceRemote authService = RMIConnector.getAuthService();
                    return authService.login(email, password);
                } catch (RemoteException | NotBoundException | AuthenticationException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                if (failure != null) {
                    handleLoginFailure(failure);
                    return;
                }
                try {
                    User user = get();
                    onLoginSuccess(user);
                } catch (Exception e) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleLoginFailure(Exception failure) {
        statusLabel.setForeground(Color.RED);
        if (failure instanceof AuthenticationException) {
            statusLabel.setText(failure.getMessage());
        } else if (failure instanceof NotBoundException) {
            statusLabel.setText("Server is reachable but services aren't bound yet. Is LostFoundServer fully started?");
        } else {
            statusLabel.setText("Could not reach the server. Is LostFoundServer running? (" + failure.getMessage() + ")");
        }
    }

    private void onLoginSuccess(User user) {
        Session.login(user);
        JOptionPane.showMessageDialog(this,
                "Welcome, " + user.getName() + "!",
                "Login Successful", JOptionPane.INFORMATION_MESSAGE);

        if (user.getRole() == Role.ADMIN) {
            new AdminDashboardFrame().setVisible(true);
        } else {
            new StudentDashboardFrame().setVisible(true);
        }
        dispose();
    }

    private void setFormEnabled(boolean enabled) {
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        backButton.setEnabled(enabled);
    }
}

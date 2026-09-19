package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.User;
import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;







public class ManageUsersFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton backButton;

    public ManageUsersFrame() {
        super("Manage Users - Admin - Lost & Found System");
        buildUI();
        loadUsers();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(750, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Manage Users");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"User ID", "Name", "Email", "Role", "Registered"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" ");
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        refreshButton = new JButton("Refresh");
        deleteButton = new JButton("Delete Selected User");
        backButton = new JButton("Back to Dashboard");
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(backButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadUsers());
        deleteButton.addActionListener(e -> attemptDelete());
        backButton.addActionListener(e -> {
            new AdminDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void loadUsers() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected List<User> doInBackground() {
                User admin = Session.getCurrentUser();
                try {
                    AuthServiceRemote authService = RMIConnector.getAuthService();
                    return authService.getAllUsers(admin.getUserId());
                } catch (RemoteException | NotBoundException | DatabaseException | AuthenticationException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Failed to load users: " + failure.getMessage());
                    return;
                }
                try {
                    List<User> users = get();
                    tableModel.setRowCount(0);
                    for (User u : users) {
                        tableModel.addRow(new Object[]{
                                u.getUserId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt()
                        });
                    }
                    statusLabel.setText(users.size() + " user(s).");
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void attemptDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            statusLabel.setText("Select a user first.");
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String email = (String) tableModel.getValueAt(row, 2);

        User currentAdmin = Session.getCurrentUser();
        if (currentAdmin != null && currentAdmin.getUserId() == userId) {
            statusLabel.setText("You cannot delete your own currently logged-in account.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete user \"" + email + "\"? This also removes their reports and claims.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        deleteButton.setEnabled(false);
        statusLabel.setText("Deleting...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                User admin = Session.getCurrentUser();
                try {
                    AuthServiceRemote authService = RMIConnector.getAuthService();
                    authService.deleteUser(admin.getUserId(), userId);
                } catch (RemoteException | NotBoundException | DatabaseException | ValidationException | AuthenticationException e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                deleteButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Delete failed: " + failure.getMessage());
                    return;
                }
                statusLabel.setText("User deleted.");
                loadUsers();
            }
        };
        worker.execute();
    }
}

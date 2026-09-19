package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.User;
import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;






public class AdminDashboardFrame extends JFrame {

    private JLabel totalUsersValue;
    private JLabel totalReportsValue;
    private JLabel lostItemsValue;
    private JLabel foundItemsValue;
    private JLabel pendingClaimsValue;
    private JLabel verifiedItemsValue;
    private JLabel returnedItemsValue;
    private JLabel statusLabel;

    public AdminDashboardFrame() {
        super("Admin Dashboard - Lost & Found System");
        buildUI();
        loadStatistics();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 520);
        setLocationRelativeTo(null);

        User user = Session.getCurrentUser();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel welcome = new JLabel("Welcome, " + (user != null ? user.getName() : "Admin") + "!");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Admin Dashboard");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        
        JPanel statsPanel = new JPanel(new GridLayout(4, 2, 15, 8));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Live Statistics"));
        statsPanel.setMaximumSize(new Dimension(480, 160));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalUsersValue = statValueLabel();
        totalReportsValue = statValueLabel();
        lostItemsValue = statValueLabel();
        foundItemsValue = statValueLabel();
        pendingClaimsValue = statValueLabel();
        verifiedItemsValue = statValueLabel();
        returnedItemsValue = statValueLabel();

        statsPanel.add(statRow("Total Users:", totalUsersValue));
        statsPanel.add(statRow("Total Reports:", totalReportsValue));
        statsPanel.add(statRow("Lost Items:", lostItemsValue));
        statsPanel.add(statRow("Found Items:", foundItemsValue));
        statsPanel.add(statRow("Pending Claims:", pendingClaimsValue));
        statsPanel.add(statRow("Verified Items:", verifiedItemsValue));
        statsPanel.add(statRow("Returned Items:", returnedItemsValue));

        
        JButton manageUsersButton = new JButton("Manage Users");
        JButton manageItemsButton = new JButton("Manage Items");
        JButton manageClaimsButton = new JButton("Manage Claims / Verify Claim");
        JButton refreshStatsButton = new JButton("Refresh Statistics");
        JButton logoutButton = new JButton("Logout");

        for (JButton b : new JButton[]{manageUsersButton, manageItemsButton, manageClaimsButton,
                refreshStatsButton, logoutButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(280, 35));
        }

        manageUsersButton.addActionListener(e -> {
            new ManageUsersFrame().setVisible(true);
            dispose();
        });
        manageItemsButton.addActionListener(e -> {
            new ManageItemsFrame().setVisible(true);
            dispose();
        });
        manageClaimsButton.addActionListener(e -> {
            new ManageClaimsFrame().setVisible(true);
            dispose();
        });
        refreshStatsButton.addActionListener(e -> loadStatistics());
        logoutButton.addActionListener(e -> {
            Session.logout();
            new WelcomeFrame().setVisible(true);
            dispose();
        });

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(welcome);
        root.add(Box.createRigidArea(new Dimension(0, 5)));
        root.add(subtitle);
        root.add(Box.createRigidArea(new Dimension(0, 15)));
        root.add(statsPanel);
        root.add(Box.createRigidArea(new Dimension(0, 5)));
        root.add(statusLabel);
        root.add(Box.createRigidArea(new Dimension(0, 15)));
        root.add(manageUsersButton);
        root.add(Box.createRigidArea(new Dimension(0, 8)));
        root.add(manageItemsButton);
        root.add(Box.createRigidArea(new Dimension(0, 8)));
        root.add(manageClaimsButton);
        root.add(Box.createRigidArea(new Dimension(0, 8)));
        root.add(refreshStatsButton);
        root.add(Box.createRigidArea(new Dimension(0, 15)));
        root.add(logoutButton);

        add(root);
    }

    private JLabel statValueLabel() {
        JLabel label = new JLabel("...");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private JPanel statRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private void loadStatistics() {
        statusLabel.setText("Loading statistics...");

        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected int[] doInBackground() {
                User admin = Session.getCurrentUser();
                try {
                    AuthServiceRemote authService = RMIConnector.getAuthService();
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();

                    int totalUsers = authService.getAllUsers(admin.getUserId()).size();
                    int totalReports = itemService.countAllItems();
                    int lost = itemService.countByStatus(ItemStatus.LOST);
                    int found = itemService.countByStatus(ItemStatus.FOUND);
                    int verified = itemService.countByStatus(ItemStatus.VERIFIED);
                    int returned = itemService.countByStatus(ItemStatus.RETURNED);
                    int pendingClaims = claimService.countByStatus(ClaimStatus.PENDING);

                    return new int[]{totalUsers, totalReports, lost, found, pendingClaims, verified, returned};

                } catch (RemoteException | NotBoundException | DatabaseException | AuthenticationException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    statusLabel.setText("Failed to load statistics: " + failure.getMessage());
                    return;
                }
                try {
                    int[] stats = get();
                    totalUsersValue.setText(String.valueOf(stats[0]));
                    totalReportsValue.setText(String.valueOf(stats[1]));
                    lostItemsValue.setText(String.valueOf(stats[2]));
                    foundItemsValue.setText(String.valueOf(stats[3]));
                    pendingClaimsValue.setText(String.valueOf(stats[4]));
                    verifiedItemsValue.setText(String.valueOf(stats[5]));
                    returnedItemsValue.setText(String.valueOf(stats[6]));
                    statusLabel.setText(" ");
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}

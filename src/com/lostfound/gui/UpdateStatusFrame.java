package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.User;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;









public class UpdateStatusFrame extends JFrame {

    private final int itemId;
    private ItemStatus currentStatus;

    private JLabel currentStatusLabel;
    private JComboBox<ItemStatus> newStatusCombo;
    private JButton updateButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    public UpdateStatusFrame(int itemId) {
        super("Update Item Status - Admin - Lost & Found System");
        this.itemId = itemId;
        buildUI();
        loadItem();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 260);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Update Item Status");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Item ID:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(String.valueOf(itemId)), gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Current Status:"), gbc);
        currentStatusLabel = new JLabel("Loading...");
        gbc.gridx = 1;
        panel.add(currentStatusLabel, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("New Status:"), gbc);
        newStatusCombo = new JComboBox<>();
        newStatusCombo.setEnabled(false);
        gbc.gridx = 1;
        panel.add(newStatusCombo, gbc);

        updateButton = new JButton("Update Status");
        updateButton.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        panel.add(updateButton, gbc);

        cancelButton = new JButton("Cancel");
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        updateButton.addActionListener(e -> attemptUpdate());
        cancelButton.addActionListener(e -> dispose());

        add(panel);
    }

    private void loadItem() {
        SwingWorker<Item, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Item doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    return itemService.getItem(itemId);
                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    statusLabel.setText("Failed to load item: " + failure.getMessage());
                    return;
                }
                try {
                    Item item = get();
                    currentStatus = item.getStatus();
                    currentStatusLabel.setText(currentStatus.toString());

                    List<ItemStatus> validTargets = new ArrayList<>();
                    for (ItemStatus candidate : ItemStatus.values()) {
                        if (currentStatus.canTransitionTo(candidate)) {
                            validTargets.add(candidate);
                        }
                    }
                    newStatusCombo.setModel(new DefaultComboBoxModel<>(validTargets.toArray(new ItemStatus[0])));

                    if (validTargets.isEmpty()) {
                        statusLabel.setForeground(Color.BLUE);
                        statusLabel.setText("This item is in a terminal state — no further transitions possible.");
                    } else {
                        newStatusCombo.setEnabled(true);
                        updateButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void attemptUpdate() {
        User admin = Session.getCurrentUser();
        if (admin == null) {
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        ItemStatus newStatus = (ItemStatus) newStatusCombo.getSelectedItem();
        if (newStatus == null) {
            statusLabel.setText("Select a target status.");
            return;
        }

        updateButton.setEnabled(false);
        cancelButton.setEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Updating...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    itemService.updateItemStatus(itemId, newStatus, admin.getUserId());
                } catch (RemoteException | NotBoundException | DatabaseException | ValidationException | AuthenticationException e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                updateButton.setEnabled(true);
                cancelButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(failure.getMessage());
                    return;
                }
                JOptionPane.showMessageDialog(UpdateStatusFrame.this,
                        "Item #" + itemId + " status updated to " + newStatus + ".",
                        "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        };
        worker.execute();
    }
}

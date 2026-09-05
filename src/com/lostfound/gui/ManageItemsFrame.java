package com.lostfound.gui;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Item;
import com.lostfound.model.User;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Admin screen: view every item report system-wide, delete
 * inappropriate/spam reports. Double-clicking a row opens the same
 * ItemDetailsFrame students use, for a full look before deciding.
 */
public class ManageItemsFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton backButton;

    public ManageItemsFrame() {
        super("Manage Items - Admin - Lost & Found System");
        buildUI();
        loadItems();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Manage Items");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Item ID", "Reporter ID", "Name", "Category", "Type", "Location", "Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int itemId = (int) tableModel.getValueAt(table.getSelectedRow(), 0);
                    new ItemDetailsFrame(itemId).setVisible(true);
                }
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Double-click a row to view full details.");
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        refreshButton = new JButton("Refresh");
        JButton updateStatusButton = new JButton("Update Status");
        deleteButton = new JButton("Delete Selected Item");
        backButton = new JButton("Back to Dashboard");
        buttonPanel.add(refreshButton);
        buttonPanel.add(updateStatusButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(backButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadItems());
        updateStatusButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                statusLabel.setText("Select an item first.");
                return;
            }
            int itemId = (int) tableModel.getValueAt(row, 0);
            new UpdateStatusFrame(itemId).setVisible(true);
        });
        deleteButton.addActionListener(e -> attemptDelete());
        backButton.addActionListener(e -> {
            new AdminDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void loadItems() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected List<Item> doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    return itemService.getAllItems();
                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Failed to load items: " + failure.getMessage());
                    return;
                }
                try {
                    List<Item> items = get();
                    tableModel.setRowCount(0);
                    for (Item item : items) {
                        tableModel.addRow(new Object[]{
                                item.getItemId(), item.getUserId(), item.getItemName(), item.getCategory(),
                                item.getType(), item.getLocation(), item.getItemDate(), item.getStatus()
                        });
                    }
                    statusLabel.setText(items.size() + " item(s). Double-click a row to view full details.");
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
            statusLabel.setText("Select an item first.");
            return;
        }
        int itemId = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete item \"" + name + "\" (#" + itemId + ")? This also removes any claims on it.",
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
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    itemService.deleteItem(itemId, admin.getUserId());
                } catch (RemoteException | NotBoundException | DatabaseException | AuthenticationException e) {
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
                statusLabel.setText("Item deleted.");
                loadItems();
            }
        };
        worker.execute();
    }
}

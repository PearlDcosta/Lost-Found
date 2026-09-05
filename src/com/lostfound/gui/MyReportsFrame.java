package com.lostfound.gui;

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
 * Shows every item the currently logged-in student has reported
 * (both LOST and FOUND), fetched via ItemServiceRemote.getItemsByUser
 * over RMI and displayed in a JTable.
 */
public class MyReportsFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton backButton;

    public MyReportsFrame() {
        super("My Reports - Lost & Found System");
        buildUI();
        loadReports();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("My Reports");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Item ID", "Name", "Category", "Type", "Location", "Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" ");
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        refreshButton = new JButton("Refresh");
        backButton = new JButton("Back to Dashboard");
        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadReports());
        backButton.addActionListener(e -> {
            new StudentDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void loadReports() {
        User user = Session.getCurrentUser();
        if (user == null) {
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected List<Item> doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    return itemService.getItemsByUser(user.getUserId());
                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Failed to load reports: " + failure.getMessage());
                    return;
                }
                try {
                    List<Item> items = get();
                    populateTable(items);
                    statusLabel.setText(items.size() + " report(s) found.");
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Item> items) {
        tableModel.setRowCount(0);
        for (Item item : items) {
            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getItemName(),
                    item.getCategory(),
                    item.getType(),
                    item.getLocation(),
                    item.getItemDate(),
                    item.getStatus()
            });
        }
    }
}

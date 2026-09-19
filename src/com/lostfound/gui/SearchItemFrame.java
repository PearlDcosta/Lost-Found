package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;








public class SearchItemFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private JTextField nameField;
    private JTextField categoryField;
    private JTextField locationField;
    private JComboBox<String> typeCombo;
    private JComboBox<String> statusCombo;
    private JTextField dateField;
    private JButton searchButton;
    private JButton clearButton;
    private JButton backButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    public SearchItemFrame() {
        super("Search Items - Lost & Found System");
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Search Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Item Name:"), gbc);
        nameField = new JTextField(12);
        gbc.gridx = 1;
        filterPanel.add(nameField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        filterPanel.add(new JLabel("Category:"), gbc);
        categoryField = new JTextField(12);
        gbc.gridx = 3;
        filterPanel.add(categoryField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        filterPanel.add(new JLabel("Location:"), gbc);
        locationField = new JTextField(12);
        gbc.gridx = 1;
        filterPanel.add(locationField, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        filterPanel.add(new JLabel("Date (yyyy-MM-dd):"), gbc);
        dateField = new JTextField(12);
        gbc.gridx = 3;
        filterPanel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        filterPanel.add(new JLabel("Type:"), gbc);
        typeCombo = new JComboBox<>(new String[]{"Any", "LOST", "FOUND"});
        gbc.gridx = 1;
        filterPanel.add(typeCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        filterPanel.add(new JLabel("Status:"), gbc);
        statusCombo = new JComboBox<>(new String[]{"Any", "LOST", "FOUND", "CLAIM_REQUESTED", "VERIFIED", "RETURNED"});
        gbc.gridx = 3;
        filterPanel.add(statusCombo, gbc);

        JPanel buttonRow = new JPanel();
        searchButton = new JButton("Search");
        clearButton = new JButton("Clear Filters");
        backButton = new JButton("Back to Dashboard");
        buttonRow.add(searchButton);
        buttonRow.add(clearButton);
        buttonRow.add(backButton);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        filterPanel.add(buttonRow, gbc);

        root.add(filterPanel, BorderLayout.NORTH);

        
        String[] columns = {"Item ID", "Name", "Category", "Type", "Location", "Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(24);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedItemDetails();
                }
            }
        });
        root.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        statusLabel = new JLabel("Enter filters above and click Search (leave fields blank to match anything).");
        root.add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> clearFilters());
        backButton.addActionListener(e -> {
            new StudentDashboardFrame().setVisible(true);
            dispose();
        });

        add(root);
    }

    private void performSearch() {
        String name = blankToNull(nameField.getText());
        String category = blankToNull(categoryField.getText());
        String location = blankToNull(locationField.getText());
        String dateText = dateField.getText().trim();

        ItemType type = "Any".equals(typeCombo.getSelectedItem()) ? null
                : ItemType.valueOf((String) typeCombo.getSelectedItem());
        ItemStatus status = "Any".equals(statusCombo.getSelectedItem()) ? null
                : ItemStatus.valueOf((String) statusCombo.getSelectedItem());

        LocalDate itemDate = null;
        if (!dateText.isEmpty()) {
            try {
                itemDate = LocalDate.parse(dateText, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                statusLabel.setText("Date must be in yyyy-MM-dd format, or left blank.");
                return;
            }
        }

        final String fName = name, fCategory = category, fLocation = location;
        final ItemType fType = type;
        final ItemStatus fStatus = status;
        final LocalDate fDate = itemDate;

        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");

        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected List<Item> doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    return itemService.searchItems(fName, fCategory, fLocation, fType, fStatus, fDate);
                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Search failed: " + failure.getMessage());
                    return;
                }
                try {
                    List<Item> results = get();
                    populateTable(results);
                    statusLabel.setText(results.size() + " result(s) found. Double-click a row for details.");
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
                    item.getItemId(), item.getItemName(), item.getCategory(),
                    item.getType(), item.getLocation(), item.getItemDate(), item.getStatus()
            });
        }
    }

    private void openSelectedItemDetails() {
        int row = resultsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int itemId = (int) tableModel.getValueAt(row, 0);
        new ItemDetailsFrame(itemId).setVisible(true);
    }

    private void clearFilters() {
        nameField.setText("");
        categoryField.setText("");
        locationField.setText("");
        dateField.setText("");
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        statusLabel.setText("Filters cleared.");
    }

    private static String blankToNull(String s) {
        String trimmed = s == null ? "" : s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

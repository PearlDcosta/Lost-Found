package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Claim;
import com.lostfound.model.Item;
import com.lostfound.model.User;
import com.lostfound.remote.ClaimServiceRemote;
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
 * Shows every claim the currently logged-in student has submitted,
 * with the claimed item's name looked up alongside it for readability
 * (a small extra RMI call per row — fine at this scale, and a
 * reasonable trade-off to avoid overloading the Claim model itself
 * with a denormalized item name).
 */
public class MyClaimsFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton backButton;

    public MyClaimsFrame() {
        super("My Claims - Lost & Found System");
        buildUI();
        loadClaims();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("My Claims");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Claim ID", "Item", "Description", "Claim Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

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

        refreshButton.addActionListener(e -> loadClaims());
        backButton.addActionListener(e -> {
            new StudentDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    private void loadClaims() {
        User user = Session.getCurrentUser();
        if (user == null) {
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        SwingWorker<Object[][], Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Object[][] doInBackground() {
                try {
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();
                    ItemServiceRemote itemService = RMIConnector.getItemService();

                    List<Claim> claims = claimService.getClaimsByUser(user.getUserId());
                    Object[][] rows = new Object[claims.size()][5];

                    for (int i = 0; i < claims.size(); i++) {
                        Claim claim = claims.get(i);
                        String itemLabel;
                        try {
                            Item item = itemService.getItem(claim.getItemId());
                            itemLabel = item.getItemName() + " (#" + item.getItemId() + ")";
                        } catch (DatabaseException e) {
                            itemLabel = "Item #" + claim.getItemId() + " (unavailable)";
                        }

                        rows[i][0] = claim.getClaimId();
                        rows[i][1] = itemLabel;
                        rows[i][2] = claim.getClaimDescription();
                        rows[i][3] = claim.getClaimDate();
                        rows[i][4] = claim.getStatus();
                    }
                    return rows;

                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Failed to load claims: " + failure.getMessage());
                    return;
                }
                try {
                    Object[][] rows = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                    statusLabel.setText(rows.length + " claim(s) found.");
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}

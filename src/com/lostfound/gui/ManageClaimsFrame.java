package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Claim;
import com.lostfound.model.Item;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Admin screen: view every claim system-wide. Double-clicking a row
 * (or selecting it and clicking "Verify Selected Claim") opens
 * VerifyClaimFrame for that specific claim.
 */
public class ManageClaimsFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton verifyButton;
    private JButton backButton;

    public ManageClaimsFrame() {
        super("Manage Claims - Admin - Lost & Found System");
        buildUI();
        refresh();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Manage Claims");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Claim ID", "Item", "Claimant User ID", "Claim Date", "Status"};
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
                if (e.getClickCount() == 2) {
                    openVerify();
                }
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Double-click a row, or select it and click Verify, to review a claim.");
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        refreshButton = new JButton("Refresh");
        verifyButton = new JButton("Verify Selected Claim");
        backButton = new JButton("Back to Dashboard");
        buttonPanel.add(refreshButton);
        buttonPanel.add(verifyButton);
        buttonPanel.add(backButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refresh());
        verifyButton.addActionListener(e -> openVerify());
        backButton.addActionListener(e -> {
            new AdminDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    /** Public so VerifyClaimFrame can trigger a reload after approving/rejecting. */
    public void refresh() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        SwingWorker<Object[][], Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Object[][] doInBackground() {
                try {
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();
                    ItemServiceRemote itemService = RMIConnector.getItemService();

                    List<Claim> claims = claimService.getAllClaims();
                    Object[][] rows = new Object[claims.size()][5];
                    for (int i = 0; i < claims.size(); i++) {
                        Claim c = claims.get(i);
                        String itemLabel;
                        try {
                            Item item = itemService.getItem(c.getItemId());
                            itemLabel = item.getItemName() + " (#" + item.getItemId() + ")";
                        } catch (DatabaseException e) {
                            itemLabel = "Item #" + c.getItemId() + " (unavailable)";
                        }
                        rows[i] = new Object[]{c.getClaimId(), itemLabel, c.getUserId(), c.getClaimDate(), c.getStatus()};
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
                    statusLabel.setText(rows.length + " claim(s).");
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openVerify() {
        int row = table.getSelectedRow();
        if (row < 0) {
            statusLabel.setText("Select a claim first.");
            return;
        }
        int claimId = (int) tableModel.getValueAt(row, 0);
        new VerifyClaimFrame(claimId, this).setVisible(true);
    }
}

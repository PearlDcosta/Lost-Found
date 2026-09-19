package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Claim;
import com.lostfound.model.Item;
import com.lostfound.model.User;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;









public class VerifyClaimFrame extends JFrame {

    private final int claimId;
    private final ManageClaimsFrame parentFrame; 

    private JPanel contentPanel;
    private JButton approveButton;
    private JButton rejectButton;
    private JButton closeButton;
    private JLabel statusLabel;

    public VerifyClaimFrame(int claimId, ManageClaimsFrame parentFrame) {
        super("Verify Claim #" + claimId + " - Admin - Lost & Found System");
        this.claimId = claimId;
        this.parentFrame = parentFrame;
        buildUI();
        loadClaimData();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 420);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Verify Claim");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        root.add(title, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(new JLabel("Loading..."));
        root.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonRow = new JPanel();
        approveButton = new JButton("Approve Claim");
        rejectButton = new JButton("Reject Claim");
        closeButton = new JButton("Close");
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        buttonRow.add(approveButton);
        buttonRow.add(rejectButton);
        buttonRow.add(closeButton);
        bottomPanel.add(buttonRow, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        root.add(bottomPanel, BorderLayout.SOUTH);

        approveButton.addActionListener(e -> decide(true));
        rejectButton.addActionListener(e -> decide(false));
        closeButton.addActionListener(e -> dispose());

        add(root);
    }

    private void loadClaimData() {
        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Object[] doInBackground() {
                try {
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();
                    ItemServiceRemote itemService = RMIConnector.getItemService();

                    Claim foundClaim = null;
                    for (Claim c : claimService.getAllClaims()) {
                        if (c.getClaimId() == claimId) {
                            foundClaim = c;
                            break;
                        }
                    }
                    if (foundClaim == null) {
                        throw new DatabaseException("Claim #" + claimId + " no longer exists.");
                    }
                    Item item = itemService.getItem(foundClaim.getItemId());
                    return new Object[]{foundClaim, item};

                } catch (RemoteException | NotBoundException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    contentPanel.removeAll();
                    contentPanel.add(new JLabel("Failed to load claim: " + failure.getMessage()));
                    contentPanel.revalidate();
                    contentPanel.repaint();
                    return;
                }
                try {
                    Object[] result = get();
                    Claim claim = (Claim) result[0];
                    Item item = (Item) result[1];
                    populate(claim, item);
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void populate(Claim claim, Item item) {
        contentPanel.removeAll();

        contentPanel.add(sectionLabel("Claim Details"));
        contentPanel.add(row("Claim ID:", String.valueOf(claim.getClaimId())));
        contentPanel.add(row("Claimant User ID:", String.valueOf(claim.getUserId())));
        contentPanel.add(row("Claim Date:", String.valueOf(claim.getClaimDate())));
        contentPanel.add(row("Claim Status:", claim.getStatus().toString()));

        JLabel descLabel = new JLabel("Claim Description:");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(descLabel);
        JTextArea descArea = new JTextArea(claim.getClaimDescription());
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(contentPanel.getBackground());
        contentPanel.add(descArea);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(sectionLabel("Item Details"));
        contentPanel.add(row("Item ID:", String.valueOf(item.getItemId())));
        contentPanel.add(row("Item Name:", item.getItemName()));
        contentPanel.add(row("Category:", item.getCategory()));
        contentPanel.add(row("Location:", item.getLocation()));
        contentPanel.add(row("Item Status:", item.getStatus().toString()));

        boolean isPending = claim.getStatus() == com.lostfound.model.ClaimStatus.PENDING;
        approveButton.setEnabled(isPending);
        rejectButton.setEnabled(isPending);
        if (!isPending) {
            statusLabel.setForeground(Color.BLUE);
            statusLabel.setText("This claim has already been " + claim.getStatus() + " — no action needed.");
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void decide(boolean approve) {
        User admin = Session.getCurrentUser();
        if (admin == null) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        setButtonsEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText(approve ? "Approving..." : "Rejecting...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();
                    if (approve) {
                        claimService.approveClaim(claimId, admin.getUserId());
                    } else {
                        claimService.rejectClaim(claimId, admin.getUserId());
                    }
                } catch (RemoteException | NotBoundException | DatabaseException | ValidationException e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                if (failure != null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(failure.getMessage());
                    return;
                }
                JOptionPane.showMessageDialog(VerifyClaimFrame.this,
                        "Claim " + (approve ? "approved" : "rejected") + " successfully.",
                        "Done", JOptionPane.INFORMATION_MESSAGE);
                if (parentFrame != null) {
                    parentFrame.refresh();
                }
                dispose();
            }
        };
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        approveButton.setEnabled(enabled);
        rejectButton.setEnabled(enabled);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private JPanel row(String label, String value) {
        JPanel r = new JPanel(new BorderLayout());
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(130, 16));
        r.add(l, BorderLayout.WEST);
        r.add(new JLabel(value), BorderLayout.CENTER);
        return r;
    }
}

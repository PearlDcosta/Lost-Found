package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Claim;
import com.lostfound.model.User;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Screen for submitting a claim against a FOUND item. Opened from
 * ItemDetailsFrame's "Submit Claim" button (only visible when the
 * item is FOUND and not the viewer's own report — see
 * ItemDetailsFrame, Phase 9).
 *
 * On success, both this frame and the ItemDetailsFrame that opened it
 * are closed, since the item's status has now changed (FOUND ->
 * CLAIM_REQUESTED) and the details on screen would be stale.
 */
public class SubmitClaimFrame extends JFrame {

    private final int itemId;
    private final ItemDetailsFrame parentDetailsFrame; // may be null

    private JTextArea descriptionArea;
    private JButton submitButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    public SubmitClaimFrame(int itemId, ItemDetailsFrame parentDetailsFrame) {
        super("Submit Claim - Lost & Found System");
        this.itemId = itemId;
        this.parentDetailsFrame = parentDetailsFrame;
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(440, 360);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Submit a Claim");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        JLabel instructions = new JLabel("<html>Explain why you believe this item is yours. " +
                "An admin will review your claim.</html>");
        centerPanel.add(instructions, BorderLayout.NORTH);

        descriptionArea = new JTextArea(6, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        centerPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonRow = new JPanel();
        submitButton = new JButton("Submit Claim");
        cancelButton = new JButton("Cancel");
        buttonRow.add(submitButton);
        buttonRow.add(cancelButton);
        bottomPanel.add(buttonRow, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        submitButton.addActionListener(e -> attemptSubmit());
        cancelButton.addActionListener(e -> dispose());

        add(panel);
    }

    private void attemptSubmit() {
        User user = Session.getCurrentUser();
        if (user == null) {
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            statusLabel.setText("Please describe why this item belongs to you.");
            return;
        }

        setFormEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Submitting claim...");

        SwingWorker<Claim, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Claim doInBackground() {
                try {
                    ClaimServiceRemote claimService = RMIConnector.getClaimService();
                    return claimService.submitClaim(itemId, user.getUserId(), description);
                } catch (RemoteException | NotBoundException | ValidationException | DatabaseException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                if (failure != null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(failure.getMessage());
                    return;
                }
                try {
                    Claim claim = get();
                    JOptionPane.showMessageDialog(SubmitClaimFrame.this,
                            "Claim submitted successfully!\nClaim ID: " + claim.getClaimId() +
                                    "\nStatus: " + claim.getStatus() +
                                    "\n\nAn admin will review it soon.",
                            "Claim Submitted", JOptionPane.INFORMATION_MESSAGE);
                    if (parentDetailsFrame != null) {
                        parentDetailsFrame.dispose();
                    }
                    dispose();
                } catch (Exception e) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setFormEnabled(boolean enabled) {
        descriptionArea.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}

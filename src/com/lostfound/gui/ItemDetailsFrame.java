package com.lostfound.gui;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.User;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;











public class ItemDetailsFrame extends JFrame {

    private final int itemId;
    private JLabel statusLabel;
    private JPanel detailsPanel;
    private JButton claimButton;
    private JButton closeButton;

    public ItemDetailsFrame(int itemId) {
        super("Item Details - Lost & Found System");
        this.itemId = itemId;
        buildUI();
        loadItem();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 420);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Item Details");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        root.add(title, BorderLayout.NORTH);

        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        statusLabel = new JLabel("Loading...");
        detailsPanel.add(statusLabel);
        root.add(detailsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        claimButton = new JButton("Submit Claim");
        claimButton.setVisible(false);
        closeButton = new JButton("Close");
        buttonPanel.add(claimButton);
        buttonPanel.add(closeButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        closeButton.addActionListener(e -> dispose());
        claimButton.addActionListener(e -> new SubmitClaimFrame(itemId, this).setVisible(true));

        add(root);
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
                    populateDetails(item);
                } catch (Exception e) {
                    statusLabel.setText("Unexpected error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void populateDetails(Item item) {
        detailsPanel.removeAll();

        if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            JLabel imageLabel = loadImagePreview(item.getImageUrl());
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            detailsPanel.add(imageLabel);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        detailsPanel.add(detailRow("Item ID:", String.valueOf(item.getItemId())));
        detailsPanel.add(detailRow("Name:", item.getItemName()));
        detailsPanel.add(detailRow("Category:", item.getCategory()));
        detailsPanel.add(detailRow("Type:", item.getType().toString()));
        detailsPanel.add(detailRow("Location:", item.getLocation()));
        detailsPanel.add(detailRow("Date:", item.getItemDate().toString()));
        detailsPanel.add(detailRow("Status:", item.getStatus().toString()));
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(descLabel);

        JTextArea descArea = new JTextArea(item.getDescription() == null ? "" : item.getDescription());
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(detailsPanel.getBackground());
        detailsPanel.add(descArea);

        User currentUser = Session.getCurrentUser();
        boolean isOwnReport = currentUser != null && currentUser.getUserId() == item.getUserId();
        boolean isClaimable = item.getStatus() == ItemStatus.FOUND && !isOwnReport;
        claimButton.setVisible(isClaimable);

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private JPanel detailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
        labelComponent.setPreferredSize(new Dimension(90, 18));
        row.add(labelComponent, BorderLayout.WEST);
        row.add(new JLabel(value), BorderLayout.CENTER);
        return row;
    }

    







    private JLabel loadImagePreview(String imageUrl) {
        try {
            ImageIcon icon = new ImageIcon(URI.create(imageUrl).toURL());
            Image scaled = icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH);
            return new JLabel(new ImageIcon(scaled));
        } catch (Exception e) {
            JLabel fallback = new JLabel("[Image unavailable]");
            fallback.setFont(fallback.getFont().deriveFont(Font.ITALIC));
            return fallback;
        }
    }
}

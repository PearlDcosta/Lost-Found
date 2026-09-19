package com.lostfound.gui;

import com.lostfound.exception.StorageException;
import com.lostfound.exception.ValidationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemType;
import com.lostfound.model.User;
import com.lostfound.remote.ItemServiceRemote;
import com.lostfound.service.StorageService;
import com.lostfound.service.StorageServiceFactory;
import com.lostfound.util.RMIConnector;
import com.lostfound.util.Session;
import com.lostfound.util.ValidationUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
















public class ReportItemFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ItemType type;

    private JTextField itemNameField;
    private JTextField categoryField;
    private JTextField locationField;
    private JTextField dateField;
    private JTextArea descriptionArea;
    private JButton chooseImageButton;
    private JLabel imageStatusLabel;
    private File selectedImageFile;
    private JButton submitButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    public ReportItemFrame(ItemType type) {
        super((type == ItemType.LOST ? "Report Lost Item" : "Report Found Item") + " - Lost & Found System");
        this.type = type;
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(type == ItemType.LOST ? "Report a Lost Item" : "Report a Found Item");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        itemNameField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(itemNameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Category:"), gbc);
        categoryField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Location:"), gbc);
        locationField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(locationField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("Date (yyyy-MM-dd):"), gbc);
        dateField = new JTextField(LocalDate.now().format(DATE_FORMAT), 18);
        gbc.gridx = 1;
        panel.add(dateField, gbc);

        gbc.gridy = 5; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Description:"), gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        descriptionArea = new JTextArea(4, 18);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        gbc.gridx = 1;
        panel.add(descScroll, gbc);

        JLabel imageNote = new JLabel("Image (optional):");
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(imageNote, gbc);

        chooseImageButton = new JButton("Choose Image...");
        gbc.gridx = 1;
        panel.add(chooseImageButton, gbc);

        imageStatusLabel = new JLabel("No image selected");
        imageStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(imageStatusLabel, gbc);

        submitButton = new JButton("Submit Report");
        gbc.gridy = 8; gbc.gridwidth = 1; gbc.gridx = 0;
        panel.add(submitButton, gbc);

        cancelButton = new JButton("Cancel");
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        chooseImageButton.addActionListener(e -> chooseImage());
        submitButton.addActionListener(e -> attemptSubmit());
        cancelButton.addActionListener(e -> {
            new StudentDashboardFrame().setVisible(true);
            dispose();
        });

        add(panel);
    }

    






    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (jpg, jpeg, png, gif)", "jpg", "jpeg", "png", "gif"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            handleImageSelected(chooser.getSelectedFile());
        }
    }

    void handleImageSelected(File file) {
        try {
            ValidationUtil.requireValidImageExtension(file.getName());
            ValidationUtil.requireValidImageSize(file.length());
            selectedImageFile = file;
            imageStatusLabel.setForeground(Color.BLACK);
            imageStatusLabel.setText("Selected: " + file.getName() +
                    " (" + (file.length() / 1024) + " KB)");
        } catch (ValidationException e) {
            selectedImageFile = null;
            imageStatusLabel.setForeground(Color.RED);
            imageStatusLabel.setText(e.getMessage());
        }
    }

    private void attemptSubmit() {
        User user = Session.getCurrentUser();
        if (user == null) {
            statusLabel.setText("Session expired. Please log in again.");
            return;
        }

        String itemName = itemNameField.getText().trim();
        String category = categoryField.getText().trim();
        String location = locationField.getText().trim();
        String dateText = dateField.getText().trim();
        String description = descriptionArea.getText().trim();

        LocalDate itemDate;
        try {
            itemDate = LocalDate.parse(dateText, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            statusLabel.setText("Date must be in yyyy-MM-dd format.");
            return;
        }

        setFormEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Submitting report...");

        SwingWorker<Item, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Item doInBackground() {
                try {
                    ItemServiceRemote itemService = RMIConnector.getItemService();
                    Item created = itemService.reportItem(user.getUserId(), itemName, category, type,
                            description, location, itemDate);

                    if (selectedImageFile != null) {
                        StorageService storageService = StorageServiceFactory.getInstance();
                        String imageUrl = storageService.uploadImage(selectedImageFile,
                                "item-" + created.getItemId() + "-" + selectedImageFile.getName());
                        itemService.updateItemImage(created.getItemId(), imageUrl);
                        created.setImageUrl(imageUrl);
                    }

                    return created;
                } catch (RemoteException | NotBoundException | ValidationException | DatabaseException | StorageException e) {
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
                    Item created = get();
                    JOptionPane.showMessageDialog(ReportItemFrame.this,
                            "Report submitted successfully!\nItem ID: " + created.getItemId() +
                                    "\nStatus: " + created.getStatus(),
                            "Report Submitted", JOptionPane.INFORMATION_MESSAGE);
                    new StudentDashboardFrame().setVisible(true);
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
        itemNameField.setEnabled(enabled);
        categoryField.setEnabled(enabled);
        locationField.setEnabled(enabled);
        dateField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
        chooseImageButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}

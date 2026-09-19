package com.lostfound.gui;

import com.lostfound.model.ItemType;
import com.lostfound.model.User;
import com.lostfound.util.Session;

import javax.swing.*;
import java.awt.*;








public class StudentDashboardFrame extends JFrame {

    public StudentDashboardFrame() {
        super("Student Dashboard - Lost & Found System");
        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 420);
        setLocationRelativeTo(null);

        User user = Session.getCurrentUser();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel welcome = new JLabel("Welcome, " + (user != null ? user.getName() : "Student") + "!");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Student Dashboard");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton reportLostButton = new JButton("Report Lost Item");
        JButton reportFoundButton = new JButton("Report Found Item");
        JButton searchButton = new JButton("Search Items");
        JButton myReportsButton = new JButton("My Reports");
        JButton myClaimsButton = new JButton("My Claims");
        JButton logoutButton = new JButton("Logout");

        for (JButton b : new JButton[]{reportLostButton, reportFoundButton, searchButton,
                myReportsButton, myClaimsButton, logoutButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(280, 35));
        }

        searchButton.setEnabled(true);
        myClaimsButton.setEnabled(true);

        reportLostButton.addActionListener(e -> {
            new ReportItemFrame(ItemType.LOST).setVisible(true);
            dispose();
        });

        reportFoundButton.addActionListener(e -> {
            new ReportItemFrame(ItemType.FOUND).setVisible(true);
            dispose();
        });

        myReportsButton.addActionListener(e -> {
            new MyReportsFrame().setVisible(true);
            dispose();
        });

        searchButton.addActionListener(e -> {
            new SearchItemFrame().setVisible(true);
            dispose();
        });

        myClaimsButton.addActionListener(e -> {
            new MyClaimsFrame().setVisible(true);
            dispose();
        });

        logoutButton.addActionListener(e -> {
            Session.logout();
            new WelcomeFrame().setVisible(true);
            dispose();
        });

        panel.add(welcome);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(subtitle);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(reportLostButton);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(reportFoundButton);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(searchButton);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(myReportsButton);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(myClaimsButton);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(logoutButton);

        add(panel);
    }
}

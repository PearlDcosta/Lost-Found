package com.lostfound;

import com.lostfound.gui.WelcomeFrame;

import javax.swing.SwingUtilities;












public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }
}

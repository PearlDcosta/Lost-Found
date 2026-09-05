package com.lostfound;

import com.lostfound.gui.WelcomeFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the CLIENT application (the Swing GUI a student or
 * admin runs on their own machine). This does NOT start any server
 * logic — it only opens WelcomeFrame, which then talks to whatever
 * LostFoundServer is reachable at rmi.host:rmi.port in
 * config.properties (localhost by default, or a cloud VM's IP once
 * deployed — see Phase 20).
 *
 * Run com.lostfound.remote.LostFoundServer FIRST, separately, before
 * running this class.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }
}

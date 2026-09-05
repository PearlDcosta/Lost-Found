package com.lostfound.util;

import com.lostfound.model.User;

/**
 * Holds the currently logged-in user on the CLIENT side, for the
 * lifetime of the Swing application process. This is purely a
 * convenience for the GUI (knowing who to greet, which dashboard to
 * open, what userId to pass on remote calls) — it is NOT a security
 * boundary. Every remote service method still re-validates its own
 * business rules server-side (see AuthServiceImpl, ItemServiceImpl,
 * ClaimServiceImpl); a client can't gain privileges just by editing
 * this object in memory, because the server never trusts anything
 * the client claims about itself beyond the userId/role returned by
 * a real login() call.
 */
public final class Session {

    private static User currentUser;

    private Session() {
        // static utility class, no instances
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}

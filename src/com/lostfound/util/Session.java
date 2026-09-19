package com.lostfound.util;

import com.lostfound.model.User;













public final class Session {

    private static User currentUser;

    private Session() {
        
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

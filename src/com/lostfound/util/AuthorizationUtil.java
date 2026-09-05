package com.lostfound.util;

import com.lostfound.dao.UserDAO;
import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Role;
import com.lostfound.model.User;

/**
 * Server-side role enforcement for admin-only remote operations.
 *
 * The Swing GUI already hides admin-only buttons from students (e.g.
 * StudentDashboardFrame never shows "Manage Users"), but that is only
 * a UI convenience — RMI is a plain network protocol, so nothing stops
 * a client from calling AuthServiceRemote.deleteUser(...) directly,
 * with any userId it likes, whether or not the GUI ever offered that
 * button. Every admin-only remote method MUST call requireAdmin(...)
 * as the very first thing it does, so the actual security boundary
 * lives on the server, not in which buttons happen to be visible.
 */
public final class AuthorizationUtil {

    private AuthorizationUtil() {
        // static utility class, no instances
    }

    /**
     * Verifies that {@code callerUserId} refers to an existing user
     * with the ADMIN role. Throws AuthenticationException otherwise —
     * whether because the id doesn't exist, or because it belongs to
     * a STUDENT account attempting an admin-only action.
     */
    public static void requireAdmin(UserDAO userDAO, int callerUserId)
            throws DatabaseException, AuthenticationException {

        User caller = userDAO.findById(callerUserId)
                .orElseThrow(() -> new AuthenticationException(
                        "Unknown user (id=" + callerUserId + ") attempted an admin-only action."));

        if (caller.getRole() != Role.ADMIN) {
            throw new AuthenticationException(
                    "User '" + caller.getEmail() + "' does not have admin privileges for this action.");
        }
    }
}

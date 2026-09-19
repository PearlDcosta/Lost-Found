package com.lostfound.util;

import com.lostfound.dao.UserDAO;
import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Role;
import com.lostfound.model.User;













public final class AuthorizationUtil {

    private AuthorizationUtil() {
        
    }

    





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

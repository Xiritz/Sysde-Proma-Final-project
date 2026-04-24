package org.example.Service;

import org.example.Model.User;
import java.util.Optional;

public class LoginService {
    private final UserService userService;
    private User currentUser;

    public LoginService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Attempts to log in a user with the provided credentials.
     * @return true if login is successful, false otherwise.
     */
    public boolean login(String username, String password) {
        Optional<User> user = userService.authenticate(username, password);
        if (user.isPresent()) {
            this.currentUser = user.get();
            System.out.println("Login successful! Welcome, " + currentUser.getUsername() + " [" + currentUser.getRole() + "]");
            return true;
        } else {
            System.out.println("Login failed: Invalid username or password.");
            return false;
        }
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("User " + currentUser.getUsername() + " has logged out.");
            this.currentUser = null;
        }
    }

    /**
     * Checks if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Gets the currently logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }
}

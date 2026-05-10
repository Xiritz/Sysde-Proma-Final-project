package org.example.Service;

import org.example.Model.Role;
import org.example.Model.User;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    public UserService() {
        // No longer needs to pre-populate list as data is in DB
    }

    public String generateNextId() {
        String query = "SELECT userId FROM users ORDER BY userId DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("userId");
                int numericPart = Integer.parseInt(lastId.substring(1));
                return String.format("U%03d", numericPart + 1);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return "U001"; // Fallback for first user
    }

    public Optional<User> authenticate(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getString("userId"),
                            rs.getString("username"),
                            rs.getString("password"),
                            Role.valueOf(rs.getString("role"))
                    );
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void addUser(User requester, User newUser) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can add users.");
        }

        // Rule: Only Owners can create Admin accounts
        if (newUser.getRole() == Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only the Owner can create Admin accounts.");
        }

        String query = "INSERT INTO users (userId, username, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newUser.getUserId());
            stmt.setString(2, newUser.getUsername());
            stmt.setString(3, newUser.getPassword());
            stmt.setString(4, newUser.getRole().name());

            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 || e.getMessage().contains("UNIQUE")) { // Handle uniqueness constraint
                throw new IllegalArgumentException("Username '" + newUser.getUsername() + "' already exists.");
            }
            e.printStackTrace();
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public void removeUser(User requester, String userId) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove users.");
        }

        // Rule: Admins cannot delete themselves
        if (requester.getUserId().equals(userId)) {
            throw new SecurityException("You cannot delete your own account. Contact the system owner.");
        }

        // Check target user's role
        User target = null;
        for (User u : getAllUsers()) {
            if (u.getUserId().equals(userId)) {
                target = u;
                break;
            }
        }

        if (target != null) {
            // Rule: Admins cannot remove other Admins or the Owner
            if (requester.getRole() == Role.ADMIN && (target.getRole() == Role.ADMIN || target.getRole() == Role.OWNER)) {
                throw new SecurityException("Admins do not have permission to remove other Admins or the Owner.");
            }
        }

        String query = "DELETE FROM users WHERE userId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUser(User requester, User updatedUser) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can update users.");
        }

        // Check target user's role
        User target = null;
        for (User u : getAllUsers()) {
            if (u.getUserId().equals(updatedUser.getUserId())) {
                target = u;
                break;
            }
        }

        if (target != null) {
            // Rule: Admins cannot update other Admins or the Owner
            if (requester.getRole() == Role.ADMIN && !requester.getUserId().equals(updatedUser.getUserId()) 
                && (target.getRole() == Role.ADMIN || target.getRole() == Role.OWNER)) {
                throw new SecurityException("Admins do not have permission to edit other Admins or the Owner.");
            }
            
            // Rule: Admins cannot change someone else's role TO Admin
            if (requester.getRole() == Role.ADMIN && updatedUser.getRole() == Role.ADMIN && target.getRole() != Role.ADMIN) {
                throw new SecurityException("Only the Owner can promote users to Admin.");
            }
        }

        String query = "UPDATE users SET username = ?, password = ?, role = ? WHERE userId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updatedUser.getUsername());
            stmt.setString(2, updatedUser.getPassword());
            stmt.setString(3, updatedUser.getRole().name());
            stmt.setString(4, updatedUser.getUserId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("User not found.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 || e.getMessage().contains("UNIQUE")) {
                throw new IllegalArgumentException("Username '" + updatedUser.getUsername() + "' already exists.");
            }
            e.printStackTrace();
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                        rs.getString("userId"),
                        rs.getString("username"),
                        rs.getString("password"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}

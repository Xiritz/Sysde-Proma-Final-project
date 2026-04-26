package org.example.Service;

import org.example.Model.Role;
import org.example.Model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    private List<User> users;
    private int idCounter;

    public UserService() {
        this.users = new ArrayList<>();
        this.idCounter = 1;
        // Pre-populate with accounts for testing
        addUserInternal(new User(generateNextId(), "admin", "admin", Role.ADMIN));
        addUserInternal(new User(generateNextId(), "employee", "employee", Role.EMPLOYEE));
    }

    private void addUserInternal(User user) {
        users.add(user);
    }

    public String generateNextId() {
        return String.format("U%03d", idCounter++);
    }

    public Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst();
    }

    public void addUser(User requester, User newUser) {
        if (requester.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can add users.");
        }
        if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(newUser.getUsername()))) {
            throw new IllegalArgumentException("Username '" + newUser.getUsername() + "' already exists.");
        }
        users.add(newUser);
    }

    public void removeUser(User requester, String userId) {
        if (requester.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can remove users.");
        }
        users.removeIf(u -> u.getUserId().equals(userId));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
}

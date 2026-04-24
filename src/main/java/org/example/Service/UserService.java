package org.example.Service;

import org.example.Model.Role;
import org.example.Model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    private List<User> users;

    public UserService() {
        this.users = new ArrayList<>();
        // Pre-populate with an admin for testing
        users.add(new User("U001", "admin", "admin123", Role.ADMIN));
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

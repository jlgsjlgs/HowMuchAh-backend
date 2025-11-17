package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User upsertUser(UUID id, String email, String name) {
        Optional<User> existingUser = userRepository.findById(id);

        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            user.setEmail(email);
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }
            return userRepository.save(user);
        } else {
            // Create new user
            User newUser = new User();
            newUser.setId(id);
            newUser.setEmail(email);
            newUser.setName(name);
            return userRepository.save(newUser);
        }
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
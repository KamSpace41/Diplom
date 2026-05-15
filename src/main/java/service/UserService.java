package service;

import model.User;
import repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("User already exists: " + username);
        }

        User user = User.builder()
                .username(username)
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        user.setEncodedPassword(rawPassword);

        return userRepository.save(user);
    }

    @Transactional
    public User authenticate(String username, String rawPassword) {
        log.info("=== START AUTHENTICATION ===");
        log.info("Username: {}", username);
        log.info("Raw password: {}", rawPassword);

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        log.info("User found: {}", user.getUsername());
        log.info("Stored password hash: {}", user.getPassword());


        if (!user.checkPassword(rawPassword)) {
            log.error("Password mismatch for user: {}", username);
            throw new RuntimeException("Invalid password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("Authentication successful for user: {}", username);
        log.info("=== END AUTHENTICATION ===");

        return savedUser;
    }
}
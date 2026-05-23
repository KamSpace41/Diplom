package service;

import model.AuthToken;
import model.User;
import repository.AuthTokenRepository;
import exception.TokenExpiredException;
import exception.TokenInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthTokenRepository authTokenRepository;

    @Value("${security.token.expiry-hours:24}")
    private int tokenExpiryHours;

    @Transactional
    public String createToken(User user) {
        AuthToken authToken = AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpiryHours))
                .revoked(false)
                .build();

        authTokenRepository.save(authToken);
        log.info("Token created for user: {}", user.getUsername());
        return authToken.getToken();
    }

    @Transactional(readOnly = true)
    public User validateToken(String token) {
        AuthToken authToken = authTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new TokenInvalidException("Invalid or expired token"));

        if (authToken.isExpired()) {
            throw new TokenExpiredException("Token expired");
        }

        return authToken.getUser();
    }

    @Transactional
    public void logout(String token) {
        authTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(authToken -> {
            authToken.setRevoked(true);
            authTokenRepository.save(authToken);
            log.info("Token revoked for user: {}", authToken.getUser().getUsername());
        });
    }

    @Transactional
    public void cleanupExpiredTokens() {
        long deleted = authTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired tokens", deleted);
        }
    }
}
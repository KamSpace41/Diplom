package repository;

import model.AuthToken;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByTokenAndRevokedFalse(String token);

    void deleteByUser(User user);

    long deleteByExpiresAtBefore(java.time.LocalDateTime now);
}
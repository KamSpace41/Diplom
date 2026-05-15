package service;

import model.AuthToken;
import model.User;
import repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void createToken_ShouldReturnValidToken() {
        User user = User.builder().id(1L).username("testuser").build();

        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.createToken(user);

        assertNotNull(token);
        verify(authTokenRepository, times(1)).save(any(AuthToken.class));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnUser() {
        User user = User.builder().id(1L).username("testuser").build();
        AuthToken authToken = AuthToken.builder()
                .token("valid-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .revoked(false)
                .build();

        when(authTokenRepository.findByTokenAndRevokedFalse("valid-token"))
                .thenReturn(Optional.of(authToken));

        User result = authService.validateToken("valid-token");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void validateToken_WithExpiredToken_ShouldThrowException() {
        AuthToken authToken = AuthToken.builder()
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .revoked(false)
                .build();

        when(authTokenRepository.findByTokenAndRevokedFalse("expired-token"))
                .thenReturn(Optional.of(authToken));

        assertThrows(RuntimeException.class, () -> authService.validateToken("expired-token"));
    }

    @Test
    void logout_ShouldRevokeToken() {
        User user = User.builder().id(1L).username("testuser").build();
        AuthToken authToken = AuthToken.builder()
                .token("test-token")
                .user(user)
                .revoked(false)
                .build();

        when(authTokenRepository.findByTokenAndRevokedFalse("test-token"))
                .thenReturn(Optional.of(authToken));
        when(authTokenRepository.save(any(AuthToken.class))).thenReturn(authToken);

        authService.logout("test-token");

        assertTrue(authToken.isRevoked());
        verify(authTokenRepository).save(authToken);
    }
}
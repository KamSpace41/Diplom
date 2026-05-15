package service;

import model.User;
import repository.UserRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void authenticate_WithValidCredentials_ShouldReturnUser() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$RoIT1Rpo/Z/wvXz32rvWwObamgVira3hcnH3d0cGvyIU2xSffoK7m")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.authenticate("admin", "password123");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticate_WithInvalidPassword_ShouldThrowException() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$RoIT1Rpo/Z/wvXz32rvWwObamgVira3hcnH3d0cGvyIU2xSffoK7m")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> userService.authenticate("admin", "wrongpassword"));
    }

    @Test
    void authenticate_UserNotFound_ShouldThrowException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.authenticate("unknown", "password123"));
    }

    @Test
    void createUser_ShouldCreateNewUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("newuser", "password123");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertTrue(result.isActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.createUser("existing", "password123"));
    }
}
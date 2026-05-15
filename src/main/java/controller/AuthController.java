package controller;

import request.LoginRequest;
import request.LoginResponse;
import model.User;
import service.AuthService;
import service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getLogin());
        User user = userService.authenticate(request.getLogin(), request.getPassword());
        String token = authService.createToken(user);

        LoginResponse response = new LoginResponse();
        response.setAuthToken(token);
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setStatus("success");
        response.setMessage("Login successful");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "auth-token", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            authService.logout(token);
            log.info("User logged out, token revoked");
        }
        return ResponseEntity.ok().build();
    }
}
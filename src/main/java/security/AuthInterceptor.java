package security;

import model.User;
import service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {


        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String authHeader = request.getHeader("auth-token");
        log.debug("Auth header: {}", authHeader);

        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Missing auth-token header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }


        String token = authHeader;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        try {
            User user = authService.validateToken(token);
            request.setAttribute("currentUser", user);
            return true;
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
package image.gen.image.controller;

import image.gen.image.model.User;
import image.gen.image.security.JwtUtil;
import image.gen.image.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager) {
        this.userService           = userService;
        this.jwtUtil               = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ─── POST /api/auth/register ───────────────────────────────────────────────
    // Body: { "username": "...", "email": "...", "password": "..." }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email    = body.get("email");
        String password = body.get("password");

        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "username, email and password are required")
            );
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Password must be at least 6 characters")
            );
        }

        try {
            User user = userService.register(username, email, password);
            String token = jwtUtil.generateToken(user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message",  "Registration successful",
                    "token",    token,
                    "username", user.getUsername(),
                    "credits",  user.getCredits()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── POST /api/auth/login ──────────────────────────────────────────────────
    // Body: { "username": "...", "password": "..." }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "username and password are required")
            );
        }

        try {
            // Spring Security validates credentials + password hash
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String token = jwtUtil.generateToken(username);
            User user    = userService.getByUsername(username);

            return ResponseEntity.ok(Map.of(
                    "message",  "Login successful",
                    "token",    token,
                    "username", user.getUsername(),
                    "credits",  user.getCredits()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }
}
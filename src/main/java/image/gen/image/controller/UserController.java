package image.gen.image.controller;

import image.gen.image.model.ImageHistory;
import image.gen.image.model.User;
import image.gen.image.repository.ImageHistoryRepository;
import image.gen.image.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final ImageHistoryRepository imageHistoryRepository;

    public UserController(UserService userService,
                          ImageHistoryRepository imageHistoryRepository) {
        this.userService            = userService;
        this.imageHistoryRepository = imageHistoryRepository;
    }

    // ─── GET /api/user/me — returns profile + credit balance ──────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getByUsername(userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "username",  user.getUsername(),
                "email",     user.getEmail(),
                "credits",   user.getCredits(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }

    // ─── GET /api/user/history — returns all generated images for this user ────
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getByUsername(userDetails.getUsername());
        List<ImageHistory> history = imageHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        return ResponseEntity.ok(history.stream().map(h -> Map.of(
                "id",        h.getId(),
                "prompt",    h.getPrompt(),
                "imageUrl",  h.getImageUrl(),
                "credits",   h.getCreditsUsed(),
                "createdAt", h.getCreatedAt().toString()
        )).toList());
    }

    // ─── POST /api/user/credits/add — admin use or payment webhook ────────────
    // Body: { "username": "...", "amount": 10 }
    @PostMapping("/credits/add")
    public ResponseEntity<?> addCredits(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        int amount      = (int) body.get("amount");

        if (username == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }

        userService.addCredits(username, amount);
        User updated = userService.getByUsername(username);

        return ResponseEntity.ok(Map.of(
                "message", "Credits added",
                "credits", updated.getCredits()
        ));
    }
}
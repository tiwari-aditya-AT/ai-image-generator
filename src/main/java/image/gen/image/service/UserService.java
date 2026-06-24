package image.gen.image.service;

import image.gen.image.model.User;
import image.gen.image.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Spring Security needs this to load user during JWT validation ─────────
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .build();
    }

    // ─── Register new user ─────────────────────────────────────────────────────
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // ✅ always hash
        user.setCredits(10); // free credits on signup

        return userRepository.save(user);
    }

    // ─── Get user by username (used in controllers) ────────────────────────────
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ─── Deduct credits (called during image generation) ──────────────────────
    public void deductCredit(User user) {
        if (user.getCredits() < 1) {
            throw new RuntimeException("Insufficient credits");
        }
        user.setCredits(user.getCredits() - 1);
        userRepository.save(user);
    }

    // ─── Add credits (admin / payment webhook) ─────────────────────────────────
    public void addCredits(String username, int amount) {
        User user = getByUsername(username);
        user.setCredits(user.getCredits() + amount);
        userRepository.save(user);
    }
}
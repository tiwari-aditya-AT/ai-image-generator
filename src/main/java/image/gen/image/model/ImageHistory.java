package image.gen.image.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_history")
@Data
@NoArgsConstructor
public class ImageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String prompt;          // original user prompt
    private String enhancedPrompt;  // Ollama-improved prompt (future use)
    private String imageUrl;        // ComfyUI result URL
    private int creditsUsed = 1;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
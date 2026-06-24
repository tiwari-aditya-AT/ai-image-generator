package image.gen.image.repository;

import image.gen.image.model.ImageHistory;
import image.gen.image.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageHistoryRepository extends JpaRepository<ImageHistory, Long> {
    List<ImageHistory> findByUserOrderByCreatedAtDesc(User user);


}
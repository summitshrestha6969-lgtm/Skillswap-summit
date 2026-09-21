package io.skillswap.springweb.Repository;

import io.skillswap.springweb.Model.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserImageRepository extends JpaRepository<UserImage, Long> {

    // Spring Data walks the User relationship automatically: "UserId" -> user.id
    Optional<UserImage> findByUserId(Long userId);
}

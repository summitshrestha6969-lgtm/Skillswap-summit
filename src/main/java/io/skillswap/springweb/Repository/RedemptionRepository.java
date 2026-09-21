package io.skillswap.springweb.Repository;

import io.skillswap.springweb.Model.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    List<Redemption> findByUserIdOrderByRedeemedAtDesc(Long userId);
}

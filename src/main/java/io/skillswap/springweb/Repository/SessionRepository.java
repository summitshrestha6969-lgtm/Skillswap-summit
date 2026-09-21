package io.skillswap.springweb.Repository;

import io.skillswap.springweb.Model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByMatchId(Long matchId);
}

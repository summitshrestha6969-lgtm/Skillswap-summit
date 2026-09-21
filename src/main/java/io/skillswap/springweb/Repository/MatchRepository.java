package io.skillswap.springweb.Repository;

import io.skillswap.springweb.Model.Match;
import io.skillswap.springweb.Model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("select m from Match m where m.userA.id = :userId or m.userB.id = :userId order by m.createdAt desc")
    List<Match> findAllInvolvingUser(@Param("userId") Long userId);

    @Query("select m from Match m where "
            + "((m.userA.id = :userId1 and m.userB.id = :userId2) or (m.userA.id = :userId2 and m.userB.id = :userId1)) "
            + "and m.status <> io.skillswap.springweb.Model.MatchStatus.REJECTED")
    Optional<Match> findActiveBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    long countByStatus(MatchStatus status);
}

package io.skillswap.springweb.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// A Match links two users who want to swap skills with each other.
@Entity
@Table(name = "skill_match")
@Data
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One: many matches can reference the same user as "user A"
    @ManyToOne
    @JoinColumn(name = "user_a_id")
    private User userA;

    // Many-to-One: many matches can reference the same user as "user B"
    @ManyToOne
    @JoinColumn(name = "user_b_id")
    private User userB;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.PENDING;

    private double matchScore;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    /** Returns whichever side of the match is NOT the given user. */
    @JsonIgnore
    public User otherUser(User me) {
        return userA.getId().equals(me.getId()) ? userB : userA;
    }

    @JsonIgnore
    public boolean involves(User user) {
        return userA.getId().equals(user.getId()) || userB.getId().equals(user.getId());
    }
}

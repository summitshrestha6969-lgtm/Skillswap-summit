package io.skillswap.springweb.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One-to-Many from the Session's point of view: a session can collect
// ratings from both participants.
@Entity
@Table(name = "session_rating")
@Data
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne
    @JoinColumn(name = "rater_id")
    private User rater;

    @ManyToOne
    @JoinColumn(name = "rated_user_id")
    private User ratedUser;

    private int score; // 1-5
    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}

package io.skillswap.springweb.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// One-to-One: each match has at most one scheduled session.
@Entity
@Table(name = "skill_session")
@Data
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    private LocalDateTime scheduledAt;
    private String location;
    private int capacity = 1;

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode = DeliveryMode.ONLINE;

    private String downloadUrl;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.SCHEDULED;

    // Many-to-Many: tracks who already claimed "session attended" points,
    // so re-clicking Join/Download doesn't farm points endlessly.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "session_attended_by",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> attendedUsers = new HashSet<>();
}

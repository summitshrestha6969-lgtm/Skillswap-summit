package io.skillswap.springweb.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// @Entity creates the table with provided name in database
// User -> app_user ("user" is a reserved word in some databases)
@Entity
@Table(name = "app_user")
@Data
public class User {

    @Id //primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //Generated Value- Auto Increment
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    // Hashed with MD5 (DigestUtils), same as the rest of the course -
    // fine for coursework, swap for BCrypt in a real product.
    @JsonIgnore
    private String password;

    private String city;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // "ROLE_USER" or "ROLE_ADMIN"
    private String role = "ROLE_USER";

    private boolean emailVerified = false;
    private boolean active = true;

    // ---- gamification ----
    private int points = 0;
    private int streakDays = 0;
    private LocalDate lastLoginDate;
    private int messageCount = 0;
    private boolean awardedProfileCompleteBonus = false;

    // ---- rating aggregates ----
    private double ratingSum = 0;
    private int ratingCount = 0;
    private int totalEnrollments = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Many-to-Many: a user can teach many skills, a skill can be taught by many users
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_offered_skills",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> offeredSkills = new HashSet<>();

    // Many-to-Many: a user can want to learn many skills
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_wanted_skills",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> wantedSkills = new HashSet<>();

    // Many-to-Many: a user can earn many badges, a badge can be earned by many users
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_badges",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "badge_id")
    )
    private Set<Badge> badges = new HashSet<>();

    // Not persisted - computed from ratingSum/ratingCount
    @Transient
    public double getAvgRating() {
        return ratingCount == 0 ? 0 : ratingSum / ratingCount;
    }
}

package io.skillswap.springweb.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One-to-Many from the User's point of view: a user can redeem many rewards.
@Entity
@Table(name = "redemption")
@Data
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String rewardCode;
    private String rewardName;
    private int cost;

    private LocalDateTime redeemedAt = LocalDateTime.now();
}

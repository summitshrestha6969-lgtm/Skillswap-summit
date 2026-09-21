package io.skillswap.springweb.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One-to-Many from the Match's point of view: one match has many messages.
@Entity
@Table(name = "chat_message")
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}

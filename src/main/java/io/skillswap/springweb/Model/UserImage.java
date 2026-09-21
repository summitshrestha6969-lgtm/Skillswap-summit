package io.skillswap.springweb.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

// Images must NOT be stored as static files on disk - they are stored
// in the database as a Base64-encoded string, held in a BLOB column.
// BLOB is used for image management: mysql cannot hold the huge string
// produced by Base64-encoding an image file as a normal VARCHAR, so we
// use @Lob with a MEDIUMBLOB column definition instead.
@Entity
@Table(name = "user_image")
@Data
public class UserImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private String image;

    private String contentType; // e.g. "image/png"

    // One-to-One: each user has at most one profile image, this side owns
    // the foreign key (user_id).
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User user;
}

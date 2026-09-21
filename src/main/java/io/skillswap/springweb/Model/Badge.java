package io.skillswap.springweb.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge")
@Data
@NoArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code; // e.g. "FIRST_SWAP"

    private String name; // e.g. "First Swap"
    private String icon; // e.g. an emoji
    private String description;

    public Badge(String code, String name, String icon, String description) {
        this.code = code;
        this.name = name;
        this.icon = icon;
        this.description = description;
    }
}

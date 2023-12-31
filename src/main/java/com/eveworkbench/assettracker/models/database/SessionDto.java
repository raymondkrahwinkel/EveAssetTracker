package com.eveworkbench.assettracker.models.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = SessionDto.TABLE_NAME)
@Getter
@Setter
public class SessionDto {
    public static final String TABLE_NAME = "Sessions";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "characterId", nullable = false)
    private CharacterDto character;

    private String browser;
    private String device;
    private String os;

    private String token;
    private Date expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

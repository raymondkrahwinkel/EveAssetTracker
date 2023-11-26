package com.eveworkbench.assettracker.models.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = CharacterDto.TABLE_NAME)
@Getter
@Setter
public class CharacterDto {
    public static final String TABLE_NAME = "Characters";

    @Id
    @Column(name = "id", nullable = false)
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Date tokenExpiresAt;
    @Column(length = 2048)
    private String accessToken;
    private String refreshToken;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;
}

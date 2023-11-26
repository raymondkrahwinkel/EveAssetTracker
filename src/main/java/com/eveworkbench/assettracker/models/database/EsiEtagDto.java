package com.eveworkbench.assettracker.models.database;

import com.sun.jdi.LongValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = EsiEtagDto.TABLE_NAME)
@Getter
@Setter
public class EsiEtagDto {
    public static final String TABLE_NAME = "EsiEtags";

    @Id
    @GeneratedValue
    private Long id;

    private String url;
    private String etag;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

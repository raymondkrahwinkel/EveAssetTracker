package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = LoginStateDto.TABLE_NAME)
public class LoginStateDto {
    public static final String TABLE_NAME = "LoginStates";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private UUID state;

    private boolean reAuthenticate = false;
    private boolean addCharacter = false;

    @JsonBackReference // break a reference loop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characterId")
    private CharacterDto parentCharacter;

    @Nullable
    private UUID session;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public LoginStateDto() {

    }

    public LoginStateDto(UUID state) {
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getState() {
        return state;
    }

    public void setState(UUID state) {
        this.state = state;
    }

    public boolean isReAuthenticate() {
        return reAuthenticate;
    }

    public void setReAuthenticate(boolean reAuthenticate) {
        this.reAuthenticate = reAuthenticate;
    }

    public boolean isAddCharacter() {
        return addCharacter;
    }

    public void setAddCharacter(boolean addCharacter) {
        this.addCharacter = addCharacter;
    }

    public UUID getSession() {
        return session;
    }

    public void setSession(UUID session) {
        this.session = session;
    }

    public CharacterDto getParentCharacter() {
        return parentCharacter;
    }

    public void setParentCharacter(CharacterDto parentCharacter) {
        this.parentCharacter = parentCharacter;
    }
}

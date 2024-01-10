package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = CharacterDto.TABLE_NAME)
public class CharacterDto {
    public static final String TABLE_NAME = "Characters";

    @Id
    @Column(name = "id", nullable = false)
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonBackReference // break a reference loop
    @ManyToOne(fetch = FetchType.LAZY)
    private CharacterDto parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<CharacterDto> children;

    private String name;
    private Date tokenExpiresAt;
    @Column(length = 2048)
    private String accessToken;
    private String refreshToken;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    @OneToMany(mappedBy = "character")
    private Set<SessionDto> sessions;

    @OneToMany(mappedBy = "parentCharacter")
    private Set<LoginStateDto> loginStates;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Date tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<SessionDto> getSessions() {
        return sessions;
    }

    public void setSessions(Set<SessionDto> sessions) {
        this.sessions = sessions;
    }

    public Set<CharacterDto> getChildren() {
        return children;
    }

    public void setChildren(Set<CharacterDto> children) {
        this.children = children;
    }

    public CharacterDto getParent() {
        return parent;
    }

    public void setParent(CharacterDto parent) {
        this.parent = parent;
    }
}

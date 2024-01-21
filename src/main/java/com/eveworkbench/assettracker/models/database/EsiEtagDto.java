package com.eveworkbench.assettracker.models.database;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "esi_etags", indexes = {
        @Index(name = "idx_etag", columnList = "etag"),
        @Index(name = "idx_url", columnList = "url"),
})
public class EsiEtagDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2048)
    private String url;

    @Column(length = 255)
    private String etag;

    private Integer lastNumberOfPages;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "esiEtag", fetch = FetchType.LAZY)
    private Set<EsiTypeDto> types;

    @OneToMany(mappedBy = "esiEtag", fetch = FetchType.LAZY)
    private Set<EsiTypeDto> esiListEtag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<EsiTypeDto> getTypes() {
        return types;
    }

    public void setTypes(Set<EsiTypeDto> types) {
        this.types = types;
    }

    public Set<EsiTypeDto> getEsiListEtag() {
        return esiListEtag;
    }

    public void setEsiListEtag(Set<EsiTypeDto> esiListEtag) {
        this.esiListEtag = esiListEtag;
    }

    public Integer getLastNumberOfPages() {
        return lastNumberOfPages;
    }

    public void setLastNumberOfPages(Integer lastNumberOfPages) {
        this.lastNumberOfPages = lastNumberOfPages;
    }
}

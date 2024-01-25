package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "esi_type_categories", indexes = {
        @Index(name = "idx_esi_type_categories_esi_etag", columnList = "esi_etag_id"),
        @Index(name = "idx_esi_type_categories_esi_list_etag", columnList = "esi_list_etag_id")
})
public class EsiTypeCategoryDto {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    private boolean published;    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiEtag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiListEtag;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<EsiTypeGroupDto> groups;

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

    public boolean isPublished() {
        return published;
    }

    public EsiEtagDto getEsiEtag() {
        return esiEtag;
    }

    public void setEsiEtag(EsiEtagDto esiEtag) {
        this.esiEtag = esiEtag;
    }

    public EsiEtagDto getEsiListEtag() {
        return esiListEtag;
    }

    public void setEsiListEtag(EsiEtagDto esiListEtag) {
        this.esiListEtag = esiListEtag;
    }

    public Set<EsiTypeGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(Set<EsiTypeGroupDto> groups) {
        this.groups = groups;
    }
}

package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "esi_type_groups", indexes = {
        @Index(name = "idx_esi_type_groups_category", columnList = "category_id"),
        @Index(name = "idx_esi_type_groups_esi_etag", columnList = "esi_etag_id"),
        @Index(name = "idx_esi_type_groups_esi_list_etag", columnList = "esi_list_etag_id")
})
public class EsiTypeGroupDto {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "published")
    private boolean published;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiTypeCategoryDto category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiEtag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiListEtag;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<EsiTypeDto> types;

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

    public void setPublished(boolean published) {
        this.published = published;
    }

    public EsiTypeCategoryDto getCategory() {
        return category;
    }

    public void setCategory(EsiTypeCategoryDto category) {
        this.category = category;
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

    public Set<EsiTypeDto> getTypes() {
        return types;
    }

    public void setTypes(Set<EsiTypeDto> types) {
        this.types = types;
    }
}

package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "esi_market_groups", indexes = {
        @Index(name = "idx_esi_market_groups_esi_etag", columnList = "esi_etag_id"),
        @Index(name = "idx_esi_market_groups_esi_list_etag", columnList = "esi_list_etag_id")
})
public class EsiMarketGroupDto {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 65535)
    private String description;

    @Column(name = "published")
    private Boolean published;

    @OneToMany(mappedBy = "marketGroup", fetch = FetchType.LAZY)
    private Set<EsiTypeDto> types;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiEtag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiListEtag;

    @JsonBackReference // break a reference loop
    @ManyToOne(fetch = FetchType.LAZY)
    private EsiMarketGroupDto parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<EsiMarketGroupDto> children;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Set<EsiTypeDto> getTypes() {
        return types;
    }

    public void setTypes(Set<EsiTypeDto> types) {
        this.types = types;
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

    public EsiMarketGroupDto getParent() {
        return parent;
    }

    public void setParent(EsiMarketGroupDto parent) {
        this.parent = parent;
    }

    public Set<EsiMarketGroupDto> getChildren() {
        return children;
    }

    public void setChildren(Set<EsiMarketGroupDto> children) {
        this.children = children;
    }
}

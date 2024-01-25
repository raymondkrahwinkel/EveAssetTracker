package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "esi_types", indexes = {
        @Index(name = "idx_esi_types_esi_etag", columnList = "esi_etag_id"),
        @Index(name = "idx_esi_types_esi_list_etag", columnList = "esi_list_etag_id"),
        @Index(name = "idx_esi_types_group_id", columnList = "group_id"),
        @Index(name = "idx_esi_types_market_group_id", columnList = "market_group_id")
})
public class EsiTypeDto {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiEtag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiEtagDto esiListEtag;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 65535)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EsiTypeGroupDto group;

    @ManyToOne(fetch = FetchType.LAZY)
    private EsiMarketGroupDto marketGroup;

    @Column(name = "mass")
    private float mass;

    @Column(name = "package_volume")
    private float packaged_volume;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "capacity")
    private float capacity;

    @Column(name = "volume")
    private float volume;

    public float getCapacity() {
        return capacity;
    }

    public void setCapacity(float capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EsiEtagDto getEsiEtag() {
        return esiEtag;
    }

    public void setEsiEtag(EsiEtagDto esiEtag) {
        this.esiEtag = esiEtag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public float getPackaged_volume() {
        return packaged_volume;
    }

    public void setPackaged_volume(float packaged_volume) {
        this.packaged_volume = packaged_volume;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public EsiEtagDto getEsiListEtag() {
        return esiListEtag;
    }

    public void setEsiListEtag(EsiEtagDto esiListEtag) {
        this.esiListEtag = esiListEtag;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EsiTypeGroupDto getGroup() {
        return group;
    }

    public void setGroup(EsiTypeGroupDto group) {
        this.group = group;
    }

    public EsiMarketGroupDto getMarketGroup() {
        return marketGroup;
    }

    public void setMarketGroup(EsiMarketGroupDto marketGroup) {
        this.marketGroup = marketGroup;
    }
}

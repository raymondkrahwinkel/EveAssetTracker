package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "esi_types", indexes = {
        @Index(name = "idx_esi_etag", columnList = "esi_etag_id"),
        @Index(name = "idx_esi_list_etag", columnList = "esi_list_etag_id"),
        @Index(name = "idx_group_id", columnList = "group_id")
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

    @Column(name = "group_id", nullable = false)
    private int group_id;

    @Column(name = "market_group_id")
    private int market_group_id;

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

    public int getGroup_id() {
        return group_id;
    }

    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }

    public int getMarket_group_id() {
        return market_group_id;
    }

    public void setMarket_group_id(int market_group_id) {
        this.market_group_id = market_group_id;
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
}

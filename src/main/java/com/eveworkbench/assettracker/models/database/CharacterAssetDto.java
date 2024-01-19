package com.eveworkbench.assettracker.models.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "character_assets")
public class CharacterAssetDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference // break a reference loop
    @ManyToOne(fetch = FetchType.LAZY)
    private CharacterDto character;

    @JsonBackReference // break a reference loop
    @ManyToOne(fetch = FetchType.LAZY)
    private EsiEtagDto esiEtag;

    private Integer typeId; // todo: link to type table when created

    private Boolean isBlueprintCopy;

    private Boolean isSingleton;

    private Long itemId;

    private String locationFlag;

    private Long locationId;

    private String locationType;

    private Integer quantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CharacterDto getCharacter() {
        return character;
    }

    public void setCharacter(CharacterDto character) {
        this.character = character;
    }

    public Boolean getBlueprintCopy() {
        return isBlueprintCopy;
    }

    public void setBlueprintCopy(Boolean blueprintCopy) {
        isBlueprintCopy = blueprintCopy;
    }

    public Boolean getSingleton() {
        return isSingleton;
    }

    public void setSingleton(Boolean singleton) {
        isSingleton = singleton;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getLocationFlag() {
        return locationFlag;
    }

    public void setLocationFlag(String locationFlag) {
        this.locationFlag = locationFlag;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public EsiEtagDto getEsiEtag() {
        return esiEtag;
    }

    public void setEsiEtag(EsiEtagDto esiEtag) {
        this.esiEtag = esiEtag;
    }
}

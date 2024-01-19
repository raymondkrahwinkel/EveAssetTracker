package com.eveworkbench.assettracker.models.esi;

import com.eveworkbench.assettracker.models.database.CharacterAssetDto;

import java.util.List;

public class AssetResponse extends EsiBaseResponse<List<AssetResponse.Asset>> {

    public static class Asset {
        public Boolean is_blueprint_copy = false;
        public Boolean is_singleton;
        public Long item_id;
        public String location_flag;
        public Long location_id;
        public String location_type;
        public Integer quantity;
        public Integer type_id;

        public static Asset fromDto(CharacterAssetDto dto) {
            var asset = new AssetResponse.Asset();

            asset.item_id = dto.getItemId();
            asset.is_blueprint_copy = dto.getBlueprintCopy();
            asset.is_singleton = dto.getSingleton();
            asset.location_type = dto.getLocationType();
            asset.location_flag = dto.getLocationFlag();
            asset.location_id = dto.getLocationId();
            asset.type_id = dto.getTypeId();
            asset.quantity = dto.getQuantity();

            return asset;
        }

        public CharacterAssetDto toDto(CharacterAssetDto dto) {
            if(dto == null) {
                dto = new CharacterAssetDto();
            }

            dto.setBlueprintCopy(is_blueprint_copy);
            dto.setSingleton(is_singleton);
            dto.setItemId(item_id);
            dto.setLocationFlag(location_flag);
            dto.setLocationType(location_type);
            dto.setLocationId(location_id);
            dto.setQuantity(quantity);
            dto.setTypeId(type_id);

            return dto;
        }
    }
}

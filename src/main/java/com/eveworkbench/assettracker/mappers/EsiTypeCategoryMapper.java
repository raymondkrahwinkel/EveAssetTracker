package com.eveworkbench.assettracker.mappers;

import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.esi.types.universe.TypeCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EsiTypeCategoryMapper {
    EsiTypeCategoryMapper INSTANCE = Mappers.getMapper(EsiTypeCategoryMapper.class);

    @Mapping(source = "category_id", target = "id")
    @Mapping(source = "groups", target = "groups", ignore = true)
    EsiTypeCategoryDto toDto(TypeCategory category);

    @Mapping(source = "category_id", target = "id")
    @Mapping(source = "groups", target = "groups", ignore = true)
    void toDto(TypeCategory category, @MappingTarget EsiTypeCategoryDto dto);

    @Mapping(source = "id", target = "category_id")
    @Mapping(source = "groups", target = "groups", ignore = true)
    TypeCategory fromDto(EsiTypeCategoryDto dto);

    @Mapping(source = "id", target = "category_id")
    @Mapping(source = "groups", target = "groups", ignore = true)
    void fromDto(EsiTypeCategoryDto dto, @MappingTarget TypeCategory category);
}

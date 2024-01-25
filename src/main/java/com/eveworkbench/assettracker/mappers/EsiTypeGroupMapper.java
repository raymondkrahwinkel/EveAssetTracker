package com.eveworkbench.assettracker.mappers;

import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.models.esi.types.universe.TypeGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EsiTypeGroupMapper {
    EsiTypeGroupMapper INSTANCE = Mappers.getMapper(EsiTypeGroupMapper.class);

    @Mapping(source = "group_id", target = "id")
    @Mapping(source = "types", target = "types", ignore = true)
    EsiTypeGroupDto toDto(TypeGroup category);

    @Mapping(source = "group_id", target = "id")
    @Mapping(source = "types", target = "types", ignore = true)
    void toDto(TypeGroup category, @MappingTarget EsiTypeGroupDto dto);

    @Mapping(source = "id", target = "group_id")
    @Mapping(source = "types", target = "types", ignore = true)
    TypeGroup fromDto(EsiTypeGroupDto dto);

    @Mapping(source = "id", target = "group_id")
    @Mapping(source = "types", target = "types", ignore = true)
    void fromDto(EsiTypeGroupDto dto, @MappingTarget TypeGroup category);
}

package com.eveworkbench.assettracker.mappers;

import com.eveworkbench.assettracker.models.database.EsiTypeDto;
import com.eveworkbench.assettracker.models.esi.types.universe.Type;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EsiTypeMapper {
    EsiTypeMapper INSTANCE = Mappers.getMapper(EsiTypeMapper.class);

    @Mapping(source = "type_id", target = "id")
    EsiTypeDto toDto(Type type);

    @Mapping(source = "type_id", target = "id")
    void toDto(Type type, @MappingTarget EsiTypeDto dto);

    @Mapping(source = "id", target = "type_id")
    Type fromDto(EsiTypeDto dto);

    @Mapping(source = "id", target = "type_id")
    void fromDto(EsiTypeDto dto, @MappingTarget Type type);
}

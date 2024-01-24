package com.eveworkbench.assettracker.mappers;

import com.eveworkbench.assettracker.models.database.EsiMarketGroupDto;
import com.eveworkbench.assettracker.models.esi.types.market.MarketGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EsiMarketGroupMapper {
    EsiMarketGroupMapper INSTANCE = Mappers.getMapper(EsiMarketGroupMapper.class);

    @Mapping(source = "market_group_id", target = "id")
    @Mapping(source = "types", target = "types", ignore = true)
    EsiMarketGroupDto toDto(MarketGroup category);

    @Mapping(source = "market_group_id", target = "id")
    @Mapping(source = "types", target = "types", ignore = true)
    void toDto(MarketGroup category, @MappingTarget EsiMarketGroupDto dto);

    @Mapping(source = "id", target = "market_group_id")
    @Mapping(source = "types", target = "types", ignore = true)
    MarketGroup fromDto(EsiMarketGroupDto dto);

    @Mapping(source = "id", target = "market_group_id")
    @Mapping(source = "types", target = "types", ignore = true)
    void fromDto(EsiMarketGroupDto dto, @MappingTarget MarketGroup category);
}

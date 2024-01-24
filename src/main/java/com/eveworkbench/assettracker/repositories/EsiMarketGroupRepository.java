package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiMarketGroupDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsiMarketGroupRepository extends CrudRepository<EsiMarketGroupDto, Integer> {
    List<EsiMarketGroupDto> findByEsiListEtag_Etag(String etag);
}

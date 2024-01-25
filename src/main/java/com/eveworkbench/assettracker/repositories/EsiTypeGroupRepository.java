package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsiTypeGroupRepository extends CrudRepository<EsiTypeGroupDto, Integer> {
    List<EsiTypeGroupDto> findByEsiListEtag_Etag(String etag);
}

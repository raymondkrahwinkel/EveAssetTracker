package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiTypeDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EsiTypeRepository extends CrudRepository<EsiTypeDto, Integer> {
    List<EsiTypeDto> findByIdIn(Collection<Integer> ids);
    List<EsiTypeDto> findByEsiEtag_Etag(String etag);
    List<EsiTypeDto> findByEsiListEtag_Etag(String etag);
    void deleteAllByIdNotIn(Collection<Integer> ids);
}

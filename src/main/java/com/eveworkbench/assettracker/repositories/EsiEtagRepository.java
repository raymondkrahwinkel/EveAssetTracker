package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsiEtagRepository extends CrudRepository<EsiEtagDto, Long> {
}

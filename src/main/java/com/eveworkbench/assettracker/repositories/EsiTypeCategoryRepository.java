package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsiTypeCategoryRepository extends CrudRepository<EsiTypeCategoryDto, Integer> {
}

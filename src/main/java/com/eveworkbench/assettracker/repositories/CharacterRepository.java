package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterRepository extends CrudRepository<CharacterDto, Integer> {
}

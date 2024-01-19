package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends CrudRepository<CharacterDto, Integer> {
    List<CharacterDto> findByAccessTokenIsNotNullAndRefreshTokenIsNotNull();
    List<CharacterDto> findByIdOrParentOrderByName(Integer id, CharacterDto parent);
}

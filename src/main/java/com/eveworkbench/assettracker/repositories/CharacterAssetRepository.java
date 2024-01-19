package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.CharacterAssetDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CharacterAssetRepository extends CrudRepository<CharacterAssetDto, Long> {
    List<CharacterAssetDto> findByEsiEtag_Etag(String etag);

    List<CharacterAssetDto> findByItemIdIn(Collection<Long> itemIds);

    void deleteAllByItemIdNotInAndCharacter_Id(Collection<Long> itemIds, Integer characterId);
}

package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EsiEtagRepository extends CrudRepository<EsiEtagDto, Long> {
    Optional<EsiEtagDto> findByUrlIgnoreCase(String url);
    Optional<EsiEtagDto> findTopByEtagAndUrl(String etag, String url);
}

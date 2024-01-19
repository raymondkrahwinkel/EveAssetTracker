package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EsiEtagRepository extends CrudRepository<EsiEtagDto, Long> {
    Optional<EsiEtagDto> findByUrlIgnoreCase(String url);
    List<EsiEtagDto> findByEtagIn(Collection<String> eTags);
    Optional<EsiEtagDto> findByEtag(String eTags);
}

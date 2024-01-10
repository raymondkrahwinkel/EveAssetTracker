package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.SessionDto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
public interface SessionRepository extends CrudRepository<SessionDto, Long> {
    Optional<SessionDto> findByCharacterIdAndToken(Integer characterId, String token);
    Optional<SessionDto> findByToken(String token);
    @Modifying
    @Transactional
    void removeByExpiresAtBefore(Date expiryDate);
}

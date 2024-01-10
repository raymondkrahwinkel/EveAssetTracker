package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.LoginStateDto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginStateRepository extends CrudRepository<LoginStateDto, Long> {
    @Modifying
    @Transactional
    void removeByCreatedAtBefore(LocalDateTime expiryDate);

    Optional<LoginStateDto> findByState(UUID state);
}

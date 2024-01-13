package com.eveworkbench.assettracker.repositories;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.WalletHistoryDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Optional;

@Repository
public interface WalletHistoryRepository extends CrudRepository<WalletHistoryDto, Long> {
    Optional<WalletHistoryDto> findByCharacterAndDate(CharacterDto character, Date date);
}

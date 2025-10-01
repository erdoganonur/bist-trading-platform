package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.Position;
import com.bisttrading.entity.trading.enums.PositionSide;
import com.bisttrading.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Position entity operations
 * Simplified version with only basic JpaRepository operations
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface PositionRepository extends BaseRepository<Position, String> {

    // Basic position lookups using Spring Data JPA method naming conventions

    /**
     * Find positions by user ID
     */
    List<Position> findByUserId(String userId);

    /**
     * Find positions by account ID
     */
    List<Position> findByAccountId(String accountId);

    /**
     * Find positions by symbol ID
     */
    List<Position> findBySymbolId(String symbolId);

    /**
     * Find positions by user ID and account ID
     */
    List<Position> findByUserIdAndAccountId(String userId, String accountId);

    /**
     * Find position by user ID, account ID and symbol ID
     */
    Optional<Position> findByUserIdAndAccountIdAndSymbolId(String userId, String accountId, String symbolId);

    /**
     * Find positions by position side
     */
    List<Position> findByPositionSide(PositionSide positionSide);

    /**
     * Find positions by user ID and position side
     */
    List<Position> findByUserIdAndPositionSide(String userId, PositionSide positionSide);
}
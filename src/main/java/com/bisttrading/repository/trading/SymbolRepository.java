package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.Symbol;
import com.bisttrading.entity.trading.enums.MarketType;
import com.bisttrading.entity.trading.enums.SymbolStatus;
import com.bisttrading.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Symbol entity operations
 * Simplified version with only basic JpaRepository operations
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface SymbolRepository extends BaseRepository<Symbol, String> {

    // Basic symbol lookups using Spring Data JPA method naming conventions

    /**
     * Find symbol by symbol code (exact match)
     */
    Optional<Symbol> findBySymbolAndDeletedAtIsNull(String symbol);

    /**
     * Find symbol by ISIN code
     */
    Optional<Symbol> findByIsinCodeAndDeletedAtIsNull(String isinCode);

    /**
     * Find symbols by exchange
     */
    List<Symbol> findByExchangeAndDeletedAtIsNull(String exchange);

    /**
     * Find symbols by market type
     */
    List<Symbol> findByMarketTypeAndDeletedAtIsNull(MarketType marketType);

    /**
     * Find symbols by status
     */
    List<Symbol> findBySymbolStatusAndDeletedAtIsNull(SymbolStatus status);

    /**
     * Find symbols by sector
     */
    List<Symbol> findBySectorAndDeletedAtIsNull(String sector);

    /**
     * Find symbols by industry
     */
    List<Symbol> findByIndustryAndDeletedAtIsNull(String industry);

    /**
     * Find symbols by currency code
     */
    List<Symbol> findByCurrencyCodeAndDeletedAtIsNull(String currencyCode);

    /**
     * Find all active symbols (not deleted)
     */
    List<Symbol> findByDeletedAtIsNull();
}
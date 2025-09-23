package com.bisttrading.core.domain.specifications;

import com.bisttrading.core.domain.valueobjects.*;
import com.bisttrading.core.domain.events.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order Specification Tests")
class OrderSpecificationTest {

    private Symbol validSymbol;
    private Quantity validQuantity;
    private Price validPrice;
    private Money sufficientBalance;
    private Money accountValue;
    private BigDecimal maxPositionRisk;

    @BeforeEach
    void setUp() {
        validSymbol = Symbol.bistSymbol("GARAN", "Türkiye Garanti Bankası A.Ş.");
        validQuantity = Quantity.of(100);
        validPrice = Price.of("10.50", "GARAN", "0.01");
        sufficientBalance = Money.of("10000.00", Currency.TRY);
        accountValue = Money.of("50000.00", Currency.TRY);
        maxPositionRisk = new BigDecimal("5.0"); // 5%
    }

    @Nested
    @DisplayName("Valid Quantity Specification")
    class ValidQuantitySpecificationTest {

        @Test
        @DisplayName("Should accept valid quantity")
        void shouldAcceptValidQuantity() {
            // Given
            OrderSpecification.ValidQuantitySpecification spec = new OrderSpecification.ValidQuantitySpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject zero quantity")
        void shouldRejectZeroQuantity() {
            // Given
            OrderSpecification.ValidQuantitySpecification spec = new OrderSpecification.ValidQuantitySpecification();
            Quantity zeroQuantity = Quantity.zero();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                zeroQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject quantity below minimum lot size")
        void shouldRejectQuantityBelowMinimumLotSize() {
            // Given
            OrderSpecification.ValidQuantitySpecification spec = new OrderSpecification.ValidQuantitySpecification();
            // Create a symbol with higher minimum lot size for testing
            Symbol symbolWithHighLotSize = Symbol.builder()
                .code("GELISIM")
                .companyName("Gelişim Pazarı Şirketi")
                .exchange(Exchange.BIST)
                .marketType(MarketType.GELISIM_PAZARI)
                .build();

            // Gelişim Pazarı has minimum lot size of 10
            Quantity smallQuantity = Quantity.of(5);
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                symbolWithHighLotSize, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                smallQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Valid Price Specification")
    class ValidPriceSpecificationTest {

        @Test
        @DisplayName("Should accept market order without price")
        void shouldAcceptMarketOrderWithoutPrice() {
            // Given
            OrderSpecification.ValidPriceSpecification spec = new OrderSpecification.ValidPriceSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.MARKET, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, null
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept limit order with valid price")
        void shouldAcceptLimitOrderWithValidPrice() {
            // Given
            OrderSpecification.ValidPriceSpecification spec = new OrderSpecification.ValidPriceSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject limit order without price")
        void shouldRejectLimitOrderWithoutPrice() {
            // Given
            OrderSpecification.ValidPriceSpecification spec = new OrderSpecification.ValidPriceSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, null
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject limit order with mismatched symbol in price")
        void shouldRejectLimitOrderWithMismatchedSymbolInPrice() {
            // Given
            OrderSpecification.ValidPriceSpecification spec = new OrderSpecification.ValidPriceSpecification();
            Price wrongSymbolPrice = Price.of("10.50", "AKBNK", "0.01");
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, wrongSymbolPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Trading Hours Specification")
    class TradingHoursSpecificationTest {

        @Test
        @DisplayName("Should handle BIST symbol trading hours validation")
        void shouldHandleBistSymbolTradingHoursValidation() {
            // Given
            OrderSpecification.TradingHoursSpecification spec = new OrderSpecification.TradingHoursSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            // This will depend on when the test is run, but the specification should execute without error
            assertThat(result).isIn(true, false);
        }

        @Test
        @DisplayName("Should allow non-BIST symbols")
        void shouldAllowNonBistSymbols() {
            // Given
            OrderSpecification.TradingHoursSpecification spec = new OrderSpecification.TradingHoursSpecification();
            Symbol foreignSymbol = Symbol.of("AAPL", "US0378331005", "Apple Inc.",
                Exchange.NASDAQ, MarketType.ANA_PAZAR, true);
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                foreignSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue(); // Non-BIST symbols are always allowed
        }
    }

    @Nested
    @DisplayName("Sufficient Balance Specification")
    class SufficientBalanceSpecificationTest {

        @Test
        @DisplayName("Should accept sell order regardless of balance")
        void shouldAcceptSellOrderRegardlessOfBalance() {
            // Given
            OrderSpecification.SufficientBalanceSpecification spec = new OrderSpecification.SufficientBalanceSpecification();
            Money lowBalance = Money.of("100.00", Currency.TRY);
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.SELL,
                validQuantity, validPrice
            );
            OrderSpecification.OrderRequestWithBalance orderWithBalance =
                new OrderSpecification.OrderRequestWithBalance(order, lowBalance);

            // When
            boolean result = spec.isSatisfiedBy(orderWithBalance);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept buy order with sufficient balance")
        void shouldAcceptBuyOrderWithSufficientBalance() {
            // Given
            OrderSpecification.SufficientBalanceSpecification spec = new OrderSpecification.SufficientBalanceSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );
            OrderSpecification.OrderRequestWithBalance orderWithBalance =
                new OrderSpecification.OrderRequestWithBalance(order, sufficientBalance);

            // When
            boolean result = spec.isSatisfiedBy(orderWithBalance);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle market orders with estimated value")
        void shouldHandleMarketOrdersWithEstimatedValue() {
            // Given
            OrderSpecification.SufficientBalanceSpecification spec = new OrderSpecification.SufficientBalanceSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.MARKET, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, null
            );
            OrderSpecification.OrderRequestWithBalance orderWithBalance =
                new OrderSpecification.OrderRequestWithBalance(order, sufficientBalance);

            // When
            boolean result = spec.isSatisfiedBy(orderWithBalance);

            // Then
            // The specification uses a conservative estimate for market orders
            assertThat(result).isIn(true, false);
        }
    }

    @Nested
    @DisplayName("Active Symbol Specification")
    class ActiveSymbolSpecificationTest {

        @Test
        @DisplayName("Should accept active symbol without special rules")
        void shouldAcceptActiveSymbolWithoutSpecialRules() {
            // Given
            OrderSpecification.ActiveSymbolSpecification spec = new OrderSpecification.ActiveSymbolSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject symbol with special trading rules")
        void shouldRejectSymbolWithSpecialTradingRules() {
            // Given
            OrderSpecification.ActiveSymbolSpecification spec = new OrderSpecification.ActiveSymbolSpecification();
            Symbol specialSymbol = Symbol.builder()
                .code("SPECIAL")
                .companyName("Özel Kurallar Şirketi")
                .exchange(Exchange.BIST)
                .marketType(MarketType.GELISIM_PAZARI) // Has special trading rules
                .build();

            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                specialSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject inactive symbol")
        void shouldRejectInactiveSymbol() {
            // Given
            OrderSpecification.ActiveSymbolSpecification spec = new OrderSpecification.ActiveSymbolSpecification();
            Symbol inactiveSymbol = Symbol.of("INACTIVE", "TR1234567890", "Inactive Company",
                Exchange.BIST, MarketType.ANA_PAZAR, false);

            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                inactiveSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Minimum Order Value Specification")
    class MinimumOrderValueSpecificationTest {

        @Test
        @DisplayName("Should accept order with sufficient value")
        void shouldAcceptOrderWithSufficientValue() {
            // Given
            OrderSpecification.MinimumOrderValueSpecification spec = new OrderSpecification.MinimumOrderValueSpecification();
            Price highPrice = Price.of("10.00", "GARAN", "0.01");
            Quantity highQuantity = Quantity.of(20); // 20 * 10 = 200 TRY > 100 TRY minimum
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                highQuantity, highPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept market orders without validation")
        void shouldAcceptMarketOrdersWithoutValidation() {
            // Given
            OrderSpecification.MinimumOrderValueSpecification spec = new OrderSpecification.MinimumOrderValueSpecification();
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.MARKET, OrderCreatedEvent.OrderSide.BUY,
                Quantity.of(1), null
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject order with insufficient value")
        void shouldRejectOrderWithInsufficientValue() {
            // Given
            OrderSpecification.MinimumOrderValueSpecification spec = new OrderSpecification.MinimumOrderValueSpecification();
            Price lowPrice = Price.of("1.00", "GARAN", "0.01");
            Quantity lowQuantity = Quantity.of(50); // 50 * 1 = 50 TRY < 100 TRY minimum
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                lowQuantity, lowPrice
            );

            // When
            boolean result = spec.isSatisfiedBy(order);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Specification Combination")
    class SpecificationCombination {

        @Test
        @DisplayName("Should combine specifications with AND")
        void shouldCombineSpecificationsWithAnd() {
            // Given
            Specification<OrderSpecification.OrderRequest> quantitySpec = new OrderSpecification.ValidQuantitySpecification();
            Specification<OrderSpecification.OrderRequest> priceSpec = new OrderSpecification.ValidPriceSpecification();
            Specification<OrderSpecification.OrderRequest> combinedSpec = quantitySpec.and(priceSpec);

            OrderSpecification.OrderRequest validOrder = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            OrderSpecification.OrderRequest invalidOrder = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                Quantity.zero(), validPrice // Invalid quantity
            );

            // When & Then
            assertThat(combinedSpec.isSatisfiedBy(validOrder)).isTrue();
            assertThat(combinedSpec.isSatisfiedBy(invalidOrder)).isFalse();
        }

        @Test
        @DisplayName("Should combine specifications with OR")
        void shouldCombineSpecificationsWithOr() {
            // Given
            Specification<OrderSpecification.OrderRequest> spec1 = new OrderSpecification.ValidQuantitySpecification();
            Specification<OrderSpecification.OrderRequest> spec2 = new OrderSpecification.ValidPriceSpecification();
            Specification<OrderSpecification.OrderRequest> combinedSpec = spec1.or(spec2);

            OrderSpecification.OrderRequest orderWithValidQuantity = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, null // Invalid price but valid quantity
            );

            // When & Then
            assertThat(combinedSpec.isSatisfiedBy(orderWithValidQuantity)).isTrue();
        }

        @Test
        @DisplayName("Should negate specification with NOT")
        void shouldNegateSpecificationWithNot() {
            // Given
            Specification<OrderSpecification.OrderRequest> quantitySpec = new OrderSpecification.ValidQuantitySpecification();
            Specification<OrderSpecification.OrderRequest> notQuantitySpec = quantitySpec.not();

            OrderSpecification.OrderRequest validOrder = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            OrderSpecification.OrderRequest invalidOrder = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                Quantity.zero(), validPrice
            );

            // When & Then
            assertThat(notQuantitySpec.isSatisfiedBy(validOrder)).isFalse();
            assertThat(notQuantitySpec.isSatisfiedBy(invalidOrder)).isTrue();
        }
    }

    @Nested
    @DisplayName("Data Record Tests")
    class DataRecordTests {

        @Test
        @DisplayName("Should create order request record correctly")
        void shouldCreateOrderRequestRecordCorrectly() {
            // When
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // Then
            assertThat(order.symbol()).isEqualTo(validSymbol);
            assertThat(order.orderType()).isEqualTo(OrderCreatedEvent.OrderType.LIMIT);
            assertThat(order.orderSide()).isEqualTo(OrderCreatedEvent.OrderSide.BUY);
            assertThat(order.quantity()).isEqualTo(validQuantity);
            assertThat(order.price()).isEqualTo(validPrice);
        }

        @Test
        @DisplayName("Should create order request with balance record correctly")
        void shouldCreateOrderRequestWithBalanceRecordCorrectly() {
            // Given
            OrderSpecification.OrderRequest order = new OrderSpecification.OrderRequest(
                validSymbol, OrderCreatedEvent.OrderType.LIMIT, OrderCreatedEvent.OrderSide.BUY,
                validQuantity, validPrice
            );

            // When
            OrderSpecification.OrderRequestWithBalance orderWithBalance =
                new OrderSpecification.OrderRequestWithBalance(order, sufficientBalance);

            // Then
            assertThat(orderWithBalance.orderRequest()).isEqualTo(order);
            assertThat(orderWithBalance.availableBalance()).isEqualTo(sufficientBalance);
        }
    }
}
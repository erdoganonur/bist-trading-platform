package com.bisttrading.graphql.types;

import java.math.BigDecimal;

/**
 * Type alias for BigDecimal to match GraphQL schema
 */
public class Decimal extends BigDecimal {

    public Decimal(String val) {
        super(val);
    }

    public Decimal(double val) {
        super(val);
    }

    public Decimal(BigDecimal val) {
        super(val.toString());
    }

    public static Decimal valueOf(double val) {
        return new Decimal(BigDecimal.valueOf(val));
    }

    public static Decimal valueOf(String val) {
        return new Decimal(val);
    }

    public static Decimal valueOf(BigDecimal val) {
        return new Decimal(val);
    }
}
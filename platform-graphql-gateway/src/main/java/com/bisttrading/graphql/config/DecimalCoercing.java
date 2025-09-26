package com.bisttrading.graphql.config;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.math.BigDecimal;

/**
 * Custom GraphQL Scalar for BigDecimal handling
 *
 * Handles decimal values with proper precision for financial calculations
 */
public class DecimalCoercing implements Coercing<BigDecimal, BigDecimal> {

    @Override
    public BigDecimal serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof BigDecimal) {
            return (BigDecimal) dataFetcherResult;
        } else if (dataFetcherResult instanceof Double) {
            return BigDecimal.valueOf((Double) dataFetcherResult);
        } else if (dataFetcherResult instanceof Float) {
            return BigDecimal.valueOf((Float) dataFetcherResult);
        } else if (dataFetcherResult instanceof Integer) {
            return BigDecimal.valueOf((Integer) dataFetcherResult);
        } else if (dataFetcherResult instanceof Long) {
            return BigDecimal.valueOf((Long) dataFetcherResult);
        } else if (dataFetcherResult instanceof String) {
            try {
                return new BigDecimal((String) dataFetcherResult);
            } catch (NumberFormatException e) {
                throw new CoercingSerializeException("Invalid decimal format: " + dataFetcherResult);
            }
        }
        throw new CoercingSerializeException("Expected BigDecimal but was " + dataFetcherResult.getClass());
    }

    @Override
    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof BigDecimal) {
            return (BigDecimal) input;
        } else if (input instanceof Double) {
            return BigDecimal.valueOf((Double) input);
        } else if (input instanceof Float) {
            return BigDecimal.valueOf((Float) input);
        } else if (input instanceof Integer) {
            return BigDecimal.valueOf((Integer) input);
        } else if (input instanceof Long) {
            return BigDecimal.valueOf((Long) input);
        } else if (input instanceof String) {
            try {
                return new BigDecimal((String) input);
            } catch (NumberFormatException e) {
                throw new CoercingParseValueException("Invalid decimal format: " + input);
            }
        }
        throw new CoercingParseValueException("Expected decimal value but was " + input.getClass());
    }

    @Override
    public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof FloatValue) {
            return ((FloatValue) input).getValue();
        } else if (input instanceof IntValue) {
            return new BigDecimal(((IntValue) input).getValue());
        } else if (input instanceof StringValue) {
            try {
                return new BigDecimal(((StringValue) input).getValue());
            } catch (NumberFormatException e) {
                throw new CoercingParseLiteralException("Invalid decimal format: " + ((StringValue) input).getValue());
            }
        }
        throw new CoercingParseLiteralException("Expected decimal literal but was " + input.getClass());
    }
}
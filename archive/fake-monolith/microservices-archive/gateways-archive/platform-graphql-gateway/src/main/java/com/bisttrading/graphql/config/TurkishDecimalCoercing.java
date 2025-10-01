package com.bisttrading.graphql.config;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Custom GraphQL Scalar for Turkish decimal formatting
 */
public class TurkishDecimalCoercing implements Coercing<BigDecimal, String> {

    private final DecimalFormat turkishFormatter;

    public TurkishDecimalCoercing() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("tr", "TR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        this.turkishFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof BigDecimal) {
            return turkishFormatter.format((BigDecimal) dataFetcherResult);
        } else if (dataFetcherResult instanceof Double) {
            return turkishFormatter.format((Double) dataFetcherResult);
        } else if (dataFetcherResult instanceof Float) {
            return turkishFormatter.format((Float) dataFetcherResult);
        } else if (dataFetcherResult instanceof Integer) {
            return turkishFormatter.format((Integer) dataFetcherResult);
        } else if (dataFetcherResult instanceof Long) {
            return turkishFormatter.format((Long) dataFetcherResult);
        }
        throw new CoercingSerializeException("Expected numeric value but was " + dataFetcherResult.getClass());
    }

    @Override
    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof String) {
            try {
                String normalized = ((String) input).replace(',', '.').replace(" ", "");
                return new BigDecimal(normalized);
            } catch (NumberFormatException e) {
                throw new CoercingParseValueException("Invalid Turkish decimal format: " + input);
            }
        } else if (input instanceof BigDecimal) {
            return (BigDecimal) input;
        } else if (input instanceof Double) {
            return BigDecimal.valueOf((Double) input);
        } else if (input instanceof Float) {
            return BigDecimal.valueOf((Float) input);
        } else if (input instanceof Integer) {
            return BigDecimal.valueOf((Integer) input);
        } else if (input instanceof Long) {
            return BigDecimal.valueOf((Long) input);
        }
        throw new CoercingParseValueException("Expected Turkish decimal value but was " + input.getClass());
    }

    @Override
    public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            String value = ((StringValue) input).getValue();
            try {
                String normalized = value.replace(',', '.').replace(" ", "");
                return new BigDecimal(normalized);
            } catch (NumberFormatException e) {
                throw new CoercingParseLiteralException("Invalid Turkish decimal format: " + value);
            }
        } else if (input instanceof FloatValue) {
            return ((FloatValue) input).getValue();
        } else if (input instanceof IntValue) {
            return new BigDecimal(((IntValue) input).getValue());
        }
        throw new CoercingParseLiteralException("Expected Turkish decimal literal but was " + input.getClass());
    }
}
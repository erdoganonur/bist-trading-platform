package com.bisttrading.graphql.config;

import com.bisttrading.graphql.util.ValidationUtils;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

/**
 * Custom GraphQL Scalar for Turkish Citizenship Number (TCKN) validation
 *
 * Validates Turkish Identity Numbers according to the official algorithm
 */
public class TCKNCoercing implements Coercing<String, String> {

    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof String) {
            String tckn = (String) dataFetcherResult;
            if (ValidationUtils.isValidTCKN(tckn)) {
                return tckn;
            }
            throw new CoercingSerializeException("Invalid TCKN: " + tckn);
        }
        throw new CoercingSerializeException("Expected String but was " + dataFetcherResult.getClass());
    }

    @Override
    public String parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof String) {
            String tckn = ((String) input).trim();

            // Basic format validation
            if (!tckn.matches("\\d{11}")) {
                throw new CoercingParseValueException("TCKN must be 11 digits: " + tckn);
            }

            // Turkish ID validation algorithm
            if (!ValidationUtils.isValidTCKN(tckn)) {
                throw new CoercingParseValueException("Invalid TCKN checksum: " + tckn);
            }

            return tckn;
        }
        throw new CoercingParseValueException("Expected String but was " + input.getClass());
    }

    @Override
    public String parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return parseValue(((StringValue) input).getValue());
        }
        throw new CoercingParseLiteralException("Expected StringValue but was " + input.getClass());
    }
}
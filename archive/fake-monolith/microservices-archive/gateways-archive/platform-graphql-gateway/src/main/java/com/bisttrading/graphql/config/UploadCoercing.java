package com.bisttrading.graphql.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

/**
 * Custom GraphQL Scalar for file upload handling
 *
 * Handles file upload references in GraphQL
 */
public class UploadCoercing implements Coercing<String, String> {

    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof String) {
            return (String) dataFetcherResult;
        }
        throw new CoercingSerializeException("Expected String but was " + dataFetcherResult.getClass());
    }

    @Override
    public String parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof String) {
            return (String) input;
        }
        throw new CoercingParseValueException("Expected String value but was " + input.getClass());
    }

    @Override
    public String parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return ((StringValue) input).getValue();
        }
        throw new CoercingParseLiteralException("Expected String literal but was " + input.getClass());
    }
}
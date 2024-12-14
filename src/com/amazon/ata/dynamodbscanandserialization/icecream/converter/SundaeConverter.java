package com.amazon.ata.dynamodbscanandserialization.icecream.converter;

import com.amazon.ata.dynamodbscanandserialization.icecream.exception.SundaeSerializationException;
import com.amazon.ata.dynamodbscanandserialization.icecream.model.Sundae;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.platform.commons.util.StringUtils.isBlank;

public class SundaeConverter implements DynamoDBTypeConverter<String, List<Sundae>> {

    private ObjectMapper mapper;

    public SundaeConverter() {
        mapper = new ObjectMapper();
    }

    /**
     * Converts a List of Sundae objects to a serialized String.
     * @param sundaes List of Sundae objects to serialize
     * @return serialized String of the List of Sundae objects
     */
    public String convert(List<Sundae> sundaes) {
        if (sundaes == null) {
            return "";
        }

        String jsonSundaes;

        try {
            jsonSundaes = mapper.writeValueAsString(sundaes);
        } catch (JsonProcessingException e) {
            throw new SundaeSerializationException(e.getMessage(), e);
        }

        return jsonSundaes;
    }
    // serialization <3

    /**
     * Converts a serialized String to a List of Sundae objects.
     * @param jsonSundaes serialized String
     * @return a List of Sundae objects from the serialized String
     */
    public List<Sundae> unconvert(String jsonSundaes) {
        List<Sundae> sundaes = new ArrayList<>();

        if (isBlank(jsonSundaes)) {
            return sundaes;
        }

        try {
            sundaes = mapper.readValue(jsonSundaes, new TypeReference<List<Sundae>>(){});
        } catch (IOException e) {
            throw new SundaeSerializationException(e.getMessage(), e);
        }

        return sundaes;
    }

}

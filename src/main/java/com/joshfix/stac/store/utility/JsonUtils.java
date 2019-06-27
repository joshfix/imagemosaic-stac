package com.joshfix.stac.store.utility;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * @author joshfix
 * Created on 1/10/18
 */
@Slf4j
public class JsonUtils {

    private static ObjectMapper objectMapper;

    public JsonUtils() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] toJson(Object obj) throws StacException {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception ex) {
            log.error("Error converting object to Json string: {}", ex);
            throw new StacException("Error converting object to Json string");
        }
    }

    public Map fromJson(String itemString) {
        try {
            return objectMapper.readValue(itemString, Map.class);
        } catch (IOException e) {
            log.error("Error deserializing hit for item: {}", itemString, e);
        }
        return null;
    }

    public Map itemCollectionFromJson(byte[] itemCollectionBytes) {
        try {
            return objectMapper.readValue(itemCollectionBytes, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map fromJsonBytes(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Asset getAsset(Object o) {
        try {
            return objectMapper.convertValue(o, Asset.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

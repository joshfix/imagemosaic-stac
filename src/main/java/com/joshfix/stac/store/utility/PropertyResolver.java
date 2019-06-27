package com.joshfix.stac.store.utility;

/**
 * @author joshfix
 * Created on 2/13/18
 */
public class PropertyResolver {

    public static String getPropertyValue(String key, String defaultValue) {

        String value = System.getenv(key);
        if (null != value) {
            return value;
        }
        value = System.getProperty(key);
        if (null != value) {
            return value;
        }

        return defaultValue;
    }

}


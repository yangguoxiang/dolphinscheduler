/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.common.utils;

import static org.apache.dolphinscheduler.common.constants.Constants.COMMON_PROPERTIES_PATH;

import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.ResUploadType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Strings;

@Slf4j
public class PropertyUtils {

    private static final Properties properties = new Properties();

    private PropertyUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    static {
        loadPropertyFile(COMMON_PROPERTIES_PATH);
    }

    public static synchronized void loadPropertyFile(String... propertyFiles) {
        for (String fileName : propertyFiles) {
            try (InputStream fis = PropertyUtils.class.getResourceAsStream(fileName);) {
                Properties subProperties = new Properties();
                subProperties.load(fis);
                subProperties.forEach((k, v) -> {
                    log.debug("Get property {} -> {}", k, v);
                });
                properties.putAll(subProperties);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                System.exit(1);
            }
        }

        // Override from system properties
        System.getProperties().forEach((k, v) -> {
            final String key = String.valueOf(k);
            log.info("Overriding property from system property: {}", key);
            PropertyUtils.setValue(key, String.valueOf(v));
        });
    }

    /**
     * @return judge whether resource upload startup
     */
    public static boolean getResUploadStartupState() {
        String resUploadStartupType = PropertyUtils.getUpperCaseString(Constants.RESOURCE_STORAGE_TYPE);
        ResUploadType resUploadType = ResUploadType.valueOf(
                Strings.isNullOrEmpty(resUploadStartupType) ? ResUploadType.NONE.name() : resUploadStartupType);
        return resUploadType != ResUploadType.NONE;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static String getString(String key) {
        if (key == null) {
            return null;
        }
        return properties.getProperty(key.trim());
    }

    /**
     * get property value with upper case
     *
     * @param key property name
     * @return property value  with upper case
     */
    public static String getUpperCaseString(String key) {
        String val = getString(key);
        return Strings.isNullOrEmpty(val) ? val : val.toUpperCase();
    }

    /**
     * get property value
     *
     * @param key property name
     * @param defaultVal default value
     * @return property value
     */
    public static String getString(String key, String defaultVal) {
        String val = getString(key);
        return Strings.isNullOrEmpty(val) ? defaultVal : val;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return get property int value , if key == null, then return -1
     */
    public static int getInt(String key) {
        return getInt(key, -1);
    }

    /**
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (Strings.isNullOrEmpty(value)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * get property value
     *
     * @param key property name
     * @param defaultValue default value
     * @return property value
     */
    public static Boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        return Strings.isNullOrEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * get property long value
     *
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static long getLong(String key, long defaultValue) {
        String value = getString(key);
        if (Strings.isNullOrEmpty(value)) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * @param key key
     * @return property value
     */
    public static long getLong(String key) {
        return getLong(key, -1);
    }

    /**
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static double getDouble(String key, double defaultValue) {
        String value = getString(key);
        if (Strings.isNullOrEmpty(value)) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get array
     *
     * @param key property name
     * @param splitStr separator
     * @return property value through array
     */
    public static String[] getArray(String key, String splitStr) {
        String value = getString(key);
        if (Strings.isNullOrEmpty(value)) {
            return new String[0];
        }
        return value.split(splitStr);
    }

    /**
     * @param key key
     * @param type type
     * @param defaultValue default value
     * @param <T> T
     * @return get enum value
     */
    public static <T extends Enum<T>> T getEnum(String key, Class<T> type,
                                                T defaultValue) {
        String value = getString(key);
        if (Strings.isNullOrEmpty(value)) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            log.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get all properties with specified prefix, like: fs.
     *
     * @param prefix prefix to search
     * @return all properties with specified prefix
     */
    public static Map<String, String> getPrefixedProperties(String prefix) {
        Map<String, String> matchedProperties = new HashMap<>();
        for (String propName : properties.stringPropertyNames()) {
            if (propName.startsWith(prefix)) {
                matchedProperties.put(propName, properties.getProperty(propName));
            }
        }
        return matchedProperties;
    }

    /**
     * set value
     * @param key key
     * @param value value
     */
    public static void setValue(String key, String value) {
        properties.setProperty(key, value);
    }

    public static Map<String, String> getPropertiesByPrefix(String prefix) {
        if (Strings.isNullOrEmpty(prefix)) {
            return null;
        }
        Set<Object> keys = properties.keySet();
        if (CollectionUtils.isEmpty(keys)) {
            return null;
        }
        Map<String, String> propertiesMap = new HashMap<>();
        keys.forEach(k -> {
            if (k.toString().contains(prefix)) {
                propertiesMap.put(k.toString().replaceFirst(prefix + ".", ""), properties.getProperty((String) k));
            }
        });
        return propertiesMap;
    }

    public static <T> Set<T> getSet(String key, Function<String, Set<T>> transformFunction, Set<T> defaultValue) {
        String value = (String) properties.get(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return transformFunction.apply(value);
    }
}

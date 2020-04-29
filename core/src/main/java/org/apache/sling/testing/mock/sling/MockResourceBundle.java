/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Mock implementation of an i18n {@link ResourceBundle}.
 * Contains no translations by default and returns the key itself.
 * But you can add your own mappings.
 */
public final class MockResourceBundle extends ResourceBundle {

    private final String baseName;
    private final Locale locale;
    private final Map<String, String> mappings = new HashMap<>();

    /**
     * @param baseName Base name
     * @param locale Locale
     */
    public MockResourceBundle(String baseName, Locale locale) {
        this.baseName = baseName;
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    protected Object handleGetObject(String key) {
        return mappings.getOrDefault(key,  key);
    }

    @Override
    public Set<String> keySet() {
        return mappings.keySet();
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(mappings.keySet());
    }

    /**
     * @return Base name
     */
    public String getBaseName() {
        return baseName;
    }    

    /**
     * Add translation.
     * @param key Key
     * @param value Value
     */
    public void put(@NotNull String key, @NotNull String value) {
        mappings.put(key, value);
    }

    /**
     * Add translations.
     * @param map Translation map
     */
    public void putAll(@NotNull Map<? extends String, ? extends String> map) {
        mappings.putAll(map);
    }

}

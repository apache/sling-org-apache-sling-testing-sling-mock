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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.xss.XSSAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

/**
 * This is a very simplified mock implementation of {@link XSSAPI} which in most cases just returns
 * the value that was passed in, or does only very basic validation.
 */
@Component(service = XSSAPI.class)
public final class MockXSSAPIImpl implements XSSAPI {

    private static final Pattern PATTERN_AUTO_DIMENSION = Pattern.compile("['\"]?auto['\"]?");
    
    @Override
    public @Nullable Integer getValidInteger(@Nullable String integer, int defaultValue) {
        if (StringUtils.isNotBlank(integer)) {
            try {
                return Integer.parseInt(integer);
            }
            catch (NumberFormatException ex) {
                // ignore
            }
        }
        return defaultValue;
    }

    @Override
    public @Nullable Long getValidLong(@Nullable String source, long defaultValue) {
        if (StringUtils.isNotBlank(source)) {
            try {
                return Long.parseLong(source);
            }
            catch (NumberFormatException ex) {
                // ignore
            }
        }
        return defaultValue;
    }

    @Override
    public @Nullable Double getValidDouble(@Nullable String source, double defaultValue) {
        if (StringUtils.isNotBlank(source)) {
            try {
                return Double.parseDouble(source);
            }
            catch (NumberFormatException ex) {
                // ignore
            }
        }
        return defaultValue;
    }

    @Override
    public @Nullable String getValidDimension(@Nullable String dimension, @Nullable String defaultValue) {
        if (StringUtils.isNotBlank(dimension)) {
            if (PATTERN_AUTO_DIMENSION.matcher(dimension).matches()) {
                return "\"auto\"";
            }
            try {
                return Integer.toString(Integer.parseInt(dimension));
            }
            catch (NumberFormatException ex) {
                // ignore
            }
        }
        return defaultValue;
    }

    @Override
    public @NotNull String getValidHref(@Nullable String url) {
        return StringUtils.defaultString(url);
    }

    @Override
    public @Nullable String getValidJSToken(@Nullable String token, @Nullable String defaultValue) {
        return StringUtils.defaultIfBlank(token, defaultValue);
    }

    @Override
    public @Nullable String getValidStyleToken(@Nullable String token, @Nullable String defaultValue) {
        return StringUtils.defaultIfBlank(token, defaultValue);
    }

    @Override
    public @Nullable String getValidCSSColor(@Nullable String color, @Nullable String defaultColor) {
        return StringUtils.defaultIfBlank(color, defaultColor);
    }

    @Override
    public String getValidMultiLineComment(@Nullable String comment, @Nullable String defaultComment) {
        return StringUtils.defaultIfBlank(comment, defaultComment);
    }

    @Override
    public String getValidJSON(@Nullable String json, @Nullable String defaultJson) {
        return StringUtils.defaultIfBlank(json, defaultJson);
    }

    @Override
    public String getValidXML(@Nullable String xml, @Nullable String defaultXml) {
        return StringUtils.defaultIfBlank(xml, defaultXml);
    }

    @Override
    public @Nullable String encodeForHTML(@Nullable String source) {
        return source;
    }

    @Override
    public @Nullable String encodeForHTMLAttr(@Nullable String source) {
        return source;
    }

    @Override
    public @Nullable String encodeForXML(@Nullable String source) {
        return source;
    }

    @Override
    public @Nullable String encodeForXMLAttr(@Nullable String source) {
        return source;
    }

    @Override
    public @Nullable String encodeForJSString(@Nullable String source) {
        return source;
    }

    @Override
    public @Nullable String encodeForCSSString(@Nullable String source) {
        return source;
    }

    @Override
    public @NotNull String filterHTML(@Nullable String source) {
        return StringUtils.defaultString(source);
    }

}

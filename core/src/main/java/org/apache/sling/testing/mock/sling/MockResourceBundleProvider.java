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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Mock implementation of @link {@link ResourceBundleProvider} then ensures
 * resolving i18n keys does not lead to exceptions. By default it contains no
 * translations, but it's possible to add you own mapping in unit tests.
 */
@Component(service = ResourceBundleProvider.class)
public final class MockResourceBundleProvider implements ResourceBundleProvider {

    private final ConcurrentHashMap<Key, MockResourceBundle> resourceBundleCache = new ConcurrentHashMap<>();
    private Locale defaultLocale = Locale.US;

    @Override
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public ResourceBundle getResourceBundle(final Locale locale) {
        return getResourceBundle(null, locale);
    }

    @Override
    public ResourceBundle getResourceBundle(final String baseName, final Locale locale) {
        Locale bundleLocale = locale != null ? locale : defaultLocale;
        Key key = new Key(baseName, bundleLocale);
        return resourceBundleCache.computeIfAbsent(key, k -> new MockResourceBundle(k.baseName, k.locale));
    }

    /**
     * Sets the default locale.
     * @param defaultLocale Default locale
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * The <code>Key</code> class encapsulates the base name and Locale in a
     * single object that can be used as the key in a <code>HashMap</code>.
     */
    private static final class Key {
        final String baseName;
        final Locale locale;

        // precomputed hash code, because this will always be used due to
        // this instance being used as a key in a HashMap.
        private final int hashCode;

        Key(final String baseName, final Locale locale) {

            int hc = 0;
            if (baseName != null) {
                hc += 17 * baseName.hashCode();
            }
            if (locale != null) {
                hc += 13 * locale.hashCode();
            }

            this.baseName = baseName;
            this.locale = locale;
            this.hashCode = hc;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Key) {
                Key other = (Key) obj;
                return equals(this.baseName, other.baseName)
                    && equals(this.locale, other.locale);
            }

            return false;
        }

        private static boolean equals(Object o1, Object o2) {
            if (o1 == null) {
                if (o2 != null) {
                    return false;
                }
            } else if (!o1.equals(o2)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Key(" + baseName + ", " + locale + ")";
        }
    }

}

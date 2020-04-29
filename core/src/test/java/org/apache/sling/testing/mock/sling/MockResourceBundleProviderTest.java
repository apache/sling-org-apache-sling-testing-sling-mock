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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

public class MockResourceBundleProviderTest {

    private static final String MY_NAME = "my-name";
    
    @Rule
    public SlingContext context = new SlingContext();

    @Test
    public void testGetResourceBundleFromRequest() {
        ResourceBundle bundle = context.request().getResourceBundle(Locale.CANADA_FRENCH);
        assertEquals(Locale.CANADA_FRENCH, bundle.getLocale());
        assertNull(((MockResourceBundle)bundle).getBaseName());
    }

    @Test
    public void testGetResourceBundleFromRequestWithBaseName() {
        ResourceBundle bundle = context.request().getResourceBundle(MY_NAME, Locale.CANADA_FRENCH);
        assertEquals(Locale.CANADA_FRENCH, bundle.getLocale());
        assertEquals(MY_NAME, ((MockResourceBundle)bundle).getBaseName());
    }

    @Test
    public void testDefaultLocale() {
        MockResourceBundleProvider bundleProvider = (MockResourceBundleProvider)context.getService(ResourceBundleProvider.class);
        assertNotNull(bundleProvider);
        bundleProvider.setDefaultLocale(Locale.KOREA);

        ResourceBundle bundle = context.request().getResourceBundle(null);
        assertEquals(Locale.KOREA, bundle.getLocale());
        assertNull(((MockResourceBundle)bundle).getBaseName());
    }

    @Test
    public void testCaching() {
        ResourceBundle bundle = context.request().getResourceBundle(Locale.GERMAN);

        ((MockResourceBundle)bundle).put("key1", "value1");
        assertEquals("value1", bundle.getString("key1"));

        ResourceBundle bundle_cached = context.request().getResourceBundle(Locale.GERMAN);
        assertEquals("value1", bundle_cached.getString("key1"));

        ResourceBundle bundle_otherlocale = context.request().getResourceBundle(Locale.FRANCE);
        assertEquals("key1", bundle_otherlocale.getString("key1"));

        ResourceBundle bundle_otherbasename = context.request().getResourceBundle(MY_NAME, Locale.GERMAN);
        assertEquals("key1", bundle_otherbasename.getString("key1"));
    }

}

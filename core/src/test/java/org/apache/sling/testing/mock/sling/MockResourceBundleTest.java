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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class MockResourceBundleTest {

    private static final String MY_NAME = "my-name";
    private MockResourceBundle underTest;

    @Before
    public void setUp() {
        underTest = new MockResourceBundle(MY_NAME, Locale.US);
        assertEquals(MY_NAME, underTest.getBaseName());
        assertEquals(Locale.US, underTest.getLocale());
    }

    @Test
    public void testEmpty() {
        assertEquals("unknown", underTest.getString("unknown"));
        assertEquals(ImmutableSet.of(), underTest.keySet());
        assertFalse(underTest.getKeys().hasMoreElements());
    }

    @Test
    public void testWithMappings() {
        underTest.put("key1", "value1");
        underTest.putAll(ImmutableMap.of("key2", "value2", "key3" ,"value3"));

        assertEquals("value1", underTest.getString("key1"));
        assertEquals("value2", underTest.getString("key2"));
        assertEquals("value3", underTest.getString("key3"));

        assertEquals(ImmutableSet.of("key1", "key2", "key3"), underTest.keySet());
        assertTrue(underTest.getKeys().hasMoreElements());
    }

}

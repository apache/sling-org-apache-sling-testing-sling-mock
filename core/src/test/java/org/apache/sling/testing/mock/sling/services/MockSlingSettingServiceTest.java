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
package org.apache.sling.testing.mock.sling.services;

import java.util.Set;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MockSlingSettingServiceTest {

    @Test
    public void testDefaultRunModes() {
        Set<String> defaultRunModes = Set.of("mode0");
        MockSlingSettingService underTest = new MockSlingSettingService(defaultRunModes);
        assertEquals(defaultRunModes, underTest.getRunModes());

        Set<String> newRunModes = Set.of("mode1", "mode2");
        underTest.setRunModes(newRunModes);
        assertEquals(newRunModes, underTest.getRunModes());
    }

    @Test
    public void testNoDefaultRunModes() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertTrue(underTest.getRunModes().isEmpty());

        Set<String> newRunModes = Set.of("mode1", "mode2");
        underTest.setRunModes(newRunModes);
        assertEquals(newRunModes, underTest.getRunModes());
    }

    @Test
    public void slingId() {
        assertThat(new MockSlingSettingService().getSlingId(), notNullValue());
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getAbsolutePathWithinSlingHome(java.lang.String)}.
     */
    @Test()
    public void testGetAbsolutePathWithinSlingHome() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, () -> underTest.getAbsolutePathWithinSlingHome("relPath1"));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getSlingHomePath()}.
     */
    @Test
    public void testGetSlingHomePath() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, underTest::getSlingHomePath);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getSlingHome()}.
     */
    @Test
    public void testGetSlingHome() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, underTest::getSlingHome);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getSlingName()}.
     */
    @Test
    public void testGetSlingName() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, underTest::getSlingName);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getSlingDescription()}.
     */
    @Test
    public void testGetSlingDescription() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, underTest::getSlingDescription);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.services.MockSlingSettingService#getBestRunModeMatchCountFromSpec(java.lang.String)}.
     */
    @Test
    public void testGetBestRunModeMatchCountFromSpec() {
        MockSlingSettingService underTest = new MockSlingSettingService();
        assertThrows(UnsupportedOperationException.class, () -> underTest.getBestRunModeMatchCountFromSpec("test"));
    }
}

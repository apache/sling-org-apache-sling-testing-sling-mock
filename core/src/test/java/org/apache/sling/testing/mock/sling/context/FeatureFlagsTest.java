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
package org.apache.sling.testing.mock.sling.context;

import org.apache.sling.featureflags.Features;
import org.apache.sling.featureflags.impl.ConfiguredFeature;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FeatureFlagsTest {

    @Rule
    public SlingContext context = new SlingContext();

    @Test
    public void testFeatureFlag_NotSet() {
        assertEnabled("feature.1", false);
    }

    @Test
    public void testFeatureFlag_Enabled() {
        context.registerInjectActivateService(new ConfiguredFeature(), "name", "feature.1", "enabled", true);
        assertEnabled("feature.1", true);
    }

    @Test
    public void testFeatureFlag__Disabled() {
        context.registerInjectActivateService(new ConfiguredFeature(), "name", "feature.1", "enabled", false);
        assertEnabled("feature.1", false);
    }

    private void assertEnabled(String featureFlag, boolean enabled) {
        Features features = context.getService(Features.class);
        assertNotNull(features);
        assertEquals("enabled", enabled, features.isEnabled("feature.1"));
    }
}

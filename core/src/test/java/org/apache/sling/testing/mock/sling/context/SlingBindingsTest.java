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

import javax.script.Bindings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.models.SlingBindingsModel;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sling.testing.mock.sling.context.MockSlingBindings.SERVICE_PROPERTY_MOCK_SLING_BINDINGS_IGNORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("null")
public class SlingBindingsTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private Resource currentResource;

    @Before
    public void setUp() throws Exception {
        // setup a custom BindingsValuesProvider
        context.registerService(BindingsValuesProvider.class, new BindingsValuesProvider() {
            @Override
            public void addBindings(Bindings bindings) {
                bindings.put("custom-param-1", "value-1");
            }
        });

        // setup another custom BindingsValuesProvider which should be ignored
        context.registerService(
                BindingsValuesProvider.class,
                new BindingsValuesProvider() {
                    @Override
                    public void addBindings(Bindings bindings) {
                        bindings.put("custom-param-2", "value-2");
                    }
                },
                SERVICE_PROPERTY_MOCK_SLING_BINDINGS_IGNORE,
                true);

        context.addModelsForClasses(SlingBindingsModel.class);
        currentResource = context.create().resource("/content/testPage/testResource");
        context.currentResource(currentResource);

        // setup a custom BindingsValuesProvider after touching request first time/setting current resource
        context.registerService(BindingsValuesProvider.class, new BindingsValuesProvider() {
            @Override
            public void addBindings(Bindings bindings) {
                bindings.put("custom-param-3", "value-3");
            }
        });
        // wait a short time to get sure OSGi events get distributed announcing the new BindingsValueProvider
        Thread.sleep(25);
    }

    @Test
    public void testModelBindings() {
        SlingBindingsModel model = context.request().adaptTo(SlingBindingsModel.class);

        assertNotNull(model);
        assertNotNull(model.getResolver());
        assertNotNull(model.getResource());
        assertEquals(currentResource.getPath(), model.getResource().getPath());
        assertNotNull(model.getRequest());
        assertNotNull(model.getResponse());
        assertNotNull(model.getCurrentNode());
        assertNotNull(model.getCurrentSession());
        assertEquals("value-1", model.getCustomParam1());
        assertNull(model.getCustomParam2());
        assertEquals("value-3", model.getCustomParam3());
    }

    @Test
    public void testCustomBindingsValuesProvider() {
        SlingBindings bindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        assertNotNull(bindings);
        assertEquals(currentResource.getPath(), bindings.getResource().getPath());
        assertEquals("value-1", bindings.get("custom-param-1"));
        assertNull(bindings.get("custom-param-2"));
        assertEquals("value-3", bindings.get("custom-param-3"));
    }
}

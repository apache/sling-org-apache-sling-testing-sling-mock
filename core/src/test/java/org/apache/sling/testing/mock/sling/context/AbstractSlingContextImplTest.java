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

import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.models.OsgiServiceModel;
import org.apache.sling.testing.mock.sling.context.models.RequestAttributeModel;
import org.apache.sling.testing.mock.sling.context.models.ServiceInterface;
import org.apache.sling.testing.mock.sling.context.modelsautoreg.ClasspathRegisteredModel;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("null")
public abstract class AbstractSlingContextImplTest {

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    @Before
    public void setUp() throws Exception {
        // make sure ResourceResolverFactory is available immediately and not lazily
        assertEquals(1, context.getServices(ResourceResolverFactory.class, null).length);

        context.addModelsForPackage("org.apache.sling.testing.mock.sling.context.models");

        ContentLoader contentLoader = this.context.load();
        contentLoader.json("/json-import-samples/content.json", "/content/sample/en");
    }

    protected abstract ResourceResolverType getResourceResolverType();

    @SuppressWarnings("deprecation")
    @Test
    public void testContextObjects() {
        assertNotNull(context.componentContext());
        assertNotNull(context.bundleContext());
        assertNotNull(context.resourceResolver());
        assertNotNull(context.jakartaRequest());
        assertNotNull(context.request());
        assertNotNull(context.requestPathInfo());
        assertNotNull(context.jakartaResponse());
        assertNotNull(context.response());
        assertNotNull(context.slingScriptHelper());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSlingBindings() {
        SlingBindings bindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        assertNotNull(bindings);
        assertSame(context.jakartaRequest(), bindings.get(SlingBindings.JAKARTA_REQUEST));
        assertSame(context.jakartaResponse(), bindings.get(SlingBindings.JAKARTA_RESPONSE));
        assertSame(context.request(), bindings.get(SlingBindings.REQUEST));
        assertSame(context.response(), bindings.get(SlingBindings.RESPONSE));
        assertSame(context.slingScriptHelper(), bindings.get(SlingBindings.SLING));
    }

    @Test
    public void testNonMockedSlingBindings() {
        final SlingBindings slingBindings = new SlingBindings();
        context.jakartaRequest().setAttribute(SlingBindings.class.getName(), slingBindings);
        SlingBindings bindings = (SlingBindings) context.jakartaRequest().getAttribute(SlingBindings.class.getName());
        assertNotNull(bindings);
    }

    @Test
    public void testSetCurrentResource() {
        context.currentResource("/content/sample/en/jcr:content/par/colctrl");
        assertEquals(
                "/content/sample/en/jcr:content/par/colctrl",
                context.currentResource().getPath());

        context.currentResource(context.resourceResolver().getResource("/content/sample/en/jcr:content/par"));
        assertEquals(
                "/content/sample/en/jcr:content/par", context.currentResource().getPath());

        context.currentResource((Resource) null);
        assertNull(context.jakartaRequest().getResource());

        context.currentResource((String) null);
        assertNull(context.jakartaRequest().getResource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetCurrentResourceNonExisting() {
        context.currentResource("/non/existing");
    }

    @Test
    public void testSlingModelsRequestAttribute() {
        context.jakartaRequest().setAttribute("prop1", "myValue");
        RequestAttributeModel model = context.jakartaRequest().adaptTo(RequestAttributeModel.class);
        assertEquals("myValue", model.getProp1());
    }

    @Test
    public void testSlingModelsOsgiService() {
        context.registerService(new MockMimeTypeService());

        OsgiServiceModel model = context.resourceResolver().adaptTo(OsgiServiceModel.class);
        assertNotNull(model.getMimeTypeService());
        assertEquals("text/html", model.getMimeTypeService().getMimeType("html"));
    }

    @Test
    public void testSlingModelsInvalidAdapt() {
        OsgiServiceModel model = context.jakartaRequest().adaptTo(OsgiServiceModel.class);
        assertNull(model);
    }

    @Test
    public void testSlingModelClasspathRegistered() {
        context.jakartaRequest().setAttribute("prop1", "myValue");
        ClasspathRegisteredModel model = context.jakartaRequest().adaptTo(ClasspathRegisteredModel.class);
        assertEquals("myValue", model.getProp1());
    }

    @Test
    public void testAdaptToInterface() {
        context.jakartaRequest().setAttribute("prop1", "myValue");
        ServiceInterface model = context.jakartaRequest().adaptTo(ServiceInterface.class);
        assertNotNull(model);
        assertEquals("myValue", model.getPropValue());
    }

    @Test
    public void testRunModes() {
        SlingSettingsService slingSettings = context.getService(SlingSettingsService.class);
        assertEquals(SlingContextImpl.DEFAULT_RUN_MODES, slingSettings.getRunModes());

        context.runMode("mode1", "mode2");
        Set<String> newRunModes = slingSettings.getRunModes();
        assertEquals(2, newRunModes.size());
        assertTrue(newRunModes.contains("mode1"));
        assertTrue(newRunModes.contains("mode2"));
    }

    @Test(expected = IllegalStateException.class)
    public void testReRegisteringResourceResolverFaactory() {
        // it is not allowed to create/register a new resource resolver factory instance if there is already one
        // so this should throw an IllegalStateException it this is detected
        MockSling.newResourceResolver(context.bundleContext());
    }
}

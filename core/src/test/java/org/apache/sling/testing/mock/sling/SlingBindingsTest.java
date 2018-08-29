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

import static org.junit.Assert.assertNotNull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.context.models.SlingBindingsModel;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("null")
public class SlingBindingsTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private Resource currentResource;

    @Before
    public void setUp() throws Exception {
        context.addModelsForClasses(SlingBindingsModel.class);
        currentResource = context.create().resource("/content/testPage/testResource");
        context.currentResource(currentResource);
    }

    @Test
    public void testBindings() {
        SlingBindingsModel model = context.request().adaptTo(SlingBindingsModel.class);

        assertNotNull(model);
        assertNotNull(model.getResolver());
        assertNotNull(model.getResource());
        assertNotNull(model.getRequest());
        assertNotNull(model.getResponse());
        assertNotNull(model.getCurrentNode());
        assertNotNull(model.getcurrentSession());
    }

}

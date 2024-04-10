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
package org.apache.sling.testing.mock.sling.resource;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests content access accross multiple resource resolvers.
 */
@SuppressWarnings("null")
public abstract class AbstractMultipleResourceResolverTest {

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    protected abstract ResourceResolverType getResourceResolverType();

    private ResourceResolverFactory resourceResolverFactory;

    protected ResourceResolverFactory newResourceResolerFactory() {
        return MockSling.newResourceResolverFactory(getResourceResolverType(), bundleContext);
    }

    @Before
    public void setUp() {
        this.resourceResolverFactory = newResourceResolerFactory();
    }

    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMultipleResourceResolver() throws Exception {
        ResourceResolver resolver1 = resourceResolverFactory.getAdministrativeResourceResolver(null);
        ResourceResolver resolver2 = resourceResolverFactory.getAdministrativeResourceResolver(null);

        // add a resource in resolver 1
        Resource root = resolver1.getResource("/");
        resolver1.create(root, "test", Map.<String, Object>of());
        resolver1.commit();

        // try to get resource in resolver 2
        Resource testResource2 = resolver2.getResource("/test");
        assertNotNull(testResource2);

        // delete resource and make sure it is removed in resolver 1 as well
        resolver2.delete(testResource2);
        resolver2.commit();

        assertNull(resolver1.getResource("/test"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsResourceTypeWithAdminResourceResolver() throws Exception {
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        createResourceAndCheckResourceType(resourceResolver);
    }

    @Test
    public void testIsResourceTypeWithNonAdminResourceResolver() throws Exception {
        ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
        createResourceAndCheckResourceType(resourceResolver);
    }

    private static void createResourceAndCheckResourceType(ResourceResolver serviceResolver)
            throws PersistenceException {
        Resource root = serviceResolver.getResource("/");
        Resource resource = serviceResolver.create(
                root, "testResource", Collections.singletonMap("sling:resourceType", "testResourceType"));
        assertTrue("is expected resource type 'testResourceType'", resource.isResourceType("testResourceType"));
        assertFalse(
                "is not unexpected resource type 'anotherResourceType'",
                resource.isResourceType("anotherResourceType"));
    }
}

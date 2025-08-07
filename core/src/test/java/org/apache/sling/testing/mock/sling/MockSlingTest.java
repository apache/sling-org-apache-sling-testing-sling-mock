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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.*;

public class MockSlingTest {

    private SlingRepository buildRepo(ResourceResolverTypeAdapter adapter) {
        BundleContext context = MockOsgi.newBundleContext();
        MockSling.buildFactoryFromRepository(NodeTypeMode.NOT_SUPPORTED, context, adapter);
        return context.getService(context.getServiceReference(SlingRepository.class));
    }

    @Test
    public void testAdapterDoesNotSupportSnapshots() {
        ResourceResolverTypeAdapter snapshotUnawareAdapter = new ResourceResolverTypeAdapter() {
            @Override
            public @Nullable ResourceResolverFactory newResourceResolverFactory() {
                return null;
            }

            @Override
            public SlingRepository newSlingRepository() {
                return new MockJcrSlingRepository();
            }
        };

        SlingRepository repo1 = buildRepo(snapshotUnawareAdapter);
        SlingRepository repo2 = buildRepo(snapshotUnawareAdapter);

        assertNotNull(repo1);
        assertNotNull(repo2);
        assertNotSame(repo2, repo1);
    }

    @Test
    public void testAdapterSupportsSnapshots() {
        SlingRepository freshRepo = new MockJcrSlingRepository();
        SlingRepository snapshotRepo = new MockJcrSlingRepository();
        ResourceResolverTypeAdapter snapshotAwareAdapter = new ResourceResolverTypeAdapter() {
            @Override
            public @Nullable ResourceResolverFactory newResourceResolverFactory() {
                return null;
            }

            @Override
            public SlingRepository newSlingRepository() {
                return freshRepo;
            }

            @Override
            public Object snapshot(SlingRepository repository) {
                return "dummy";
            }

            @Override
            public SlingRepository newSlingRepositoryFromSnapshot(Object snapshot) {
                return snapshotRepo;
            }
        };

        SlingRepository repo1 = buildRepo(snapshotAwareAdapter);
        SlingRepository repo2 = buildRepo(snapshotAwareAdapter);

        assertSame(freshRepo, repo1);
        assertSame(snapshotRepo, repo2);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSling#newSlingScriptHelper(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse, org.osgi.framework.BundleContext)}.
     * @deprecated use {@link #testNewSlingScriptHelperSlingJakartaHttpServletRequestSlingJakartaHttpServletResponseBundleContext() instead
     */
    @Deprecated(since = "4.1.0")
    @Test
    public void testNewSlingScriptHelperSlingHttpServletRequestSlingHttpServletResponseBundleContext() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest(
                        new MockSlingJakartaHttpServletRequest(bundleContext));
        org.apache.sling.api.SlingHttpServletResponse javaxResponse =
                new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse(
                        new MockSlingJakartaHttpServletResponse());
        assertNotNull(MockSling.newSlingScriptHelper(javaxRequest, javaxResponse, bundleContext));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSling#newSlingScriptHelper(org.apache.sling.api.SlingJakartaHttpServletRequest, org.apache.sling.api.SlingJakartaHttpServletResponse, org.osgi.framework.BundleContext)}.
     */
    @Test
    public void testNewSlingScriptHelperSlingJakartaHttpServletRequestSlingJakartaHttpServletResponseBundleContext() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        SlingJakartaHttpServletRequest jakartaRequest = new MockSlingJakartaHttpServletRequest(bundleContext);
        SlingJakartaHttpServletResponse jakartaResponse = new MockSlingJakartaHttpServletResponse();
        assertNotNull(MockSling.newSlingScriptHelper(jakartaRequest, jakartaResponse, bundleContext));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSling#newSlingScriptHelper(org.osgi.framework.BundleContext)}.
     * @deprecated use {@link #testNewJakartaSlingScriptHelper()} instead
     */
    @Deprecated(since = "4.1.0")
    @Test
    public void testNewSlingScriptHelperBundleContext() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        assertNotNull(MockSling.newSlingScriptHelper(bundleContext));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSling#newJakartaSlingScriptHelper(org.osgi.framework.BundleContext)}.
     */
    @Test
    public void testNewJakartaSlingScriptHelper() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        assertNotNull(MockSling.newJakartaSlingScriptHelper(bundleContext));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSling#newResourceResolverFactory(org.osgi.framework.BundleContext)}.
     */
    @Test
    public void testNewResourceResolverFactoryBundleContext() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        assertNotNull(MockSling.newResourceResolverFactory(bundleContext));
    }
}

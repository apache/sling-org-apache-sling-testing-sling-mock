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

import java.util.Arrays;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.JavaxToJakartaRequestWrapper;
import org.apache.sling.api.wrappers.JavaxToJakartaResponseWrapper;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MockSlingScriptHelperTest {

    private ResourceResolver resourceResolver;

    @SuppressWarnings("deprecation")
    private org.apache.sling.api.SlingHttpServletRequest request;

    @SuppressWarnings("deprecation")
    private org.apache.sling.api.SlingHttpServletResponse response;

    private MockSlingJakartaHttpServletRequest jakartaRequest;
    private MockSlingJakartaHttpServletResponse jakartaResponse;

    private BundleContext bundleContext;
    private SlingScriptHelper scriptHelper;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        this.bundleContext = MockOsgi.newBundleContext();
        this.resourceResolver = MockSling.newResourceResolver(bundleContext);
        jakartaRequest = new MockSlingJakartaHttpServletRequest(this.resourceResolver, this.bundleContext);
        this.request = new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest(jakartaRequest);
        jakartaResponse = new MockSlingJakartaHttpServletResponse();
        this.response = new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse(jakartaResponse);
        this.scriptHelper = MockSling.newSlingScriptHelper(this.request, this.response, this.bundleContext);
    }

    @After
    public void tearDown() {
        this.resourceResolver.close();
        MockOsgi.shutdown(this.bundleContext);
    }

    /**
     * @deprecated use {@link #testJakartaRequest()} instead
     */
    @Deprecated(since = "4.0.0")
    @Test
    public void testRequest() {
        assertSame(this.request, this.scriptHelper.getRequest());
    }

    /**
     * @deprecated use {@link #testJakartaResponse()} instead
     */
    @Deprecated(since = "4.0.0")
    @Test
    public void testResponse() {
        assertSame(this.response, this.scriptHelper.getResponse());
    }

    @Test
    public void testJakartaRequest() {
        assertSame(this.jakartaRequest, this.scriptHelper.getJakartaRequest());
    }

    @Test
    public void testJakartaResponse() {
        assertSame(this.jakartaResponse, this.scriptHelper.getJakartaResponse());
    }

    @Test
    public void testGetService() {
        this.bundleContext.registerService(String.class.getName(), "test", null);
        assertEquals("test", this.scriptHelper.getService(String.class));
    }

    @Test
    public void testGetServices() {
        Integer[] services = new Integer[] {1, 2, 3};
        for (Integer service : services) {
            this.bundleContext.registerService(Integer.class.getName(), service, null);
        }
        Integer[] servicesResult = this.scriptHelper.getServices(Integer.class, null);
        Arrays.sort(servicesResult);
        assertArrayEquals(services, servicesResult);
    }

    /**
     *
     */
    @Deprecated(since = "4.0.0")
    @Test
    public void testNewSlingScriptHelperForNotMockSlingRequest() {
        org.apache.sling.api.SlingHttpServletRequest mockRequest =
                Mockito.mock(org.apache.sling.api.SlingHttpServletRequest.class);
        org.apache.sling.api.SlingHttpServletResponse mockResponse =
                Mockito.mock(org.apache.sling.api.SlingHttpServletResponse.class);
        SlingScriptHelper slingScriptHelper =
                MockSling.newSlingScriptHelper(mockRequest, mockResponse, this.bundleContext);
        assertTrue(slingScriptHelper.getJakartaRequest() instanceof JavaxToJakartaRequestWrapper);
        assertTrue(slingScriptHelper.getJakartaResponse() instanceof JavaxToJakartaResponseWrapper);
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#getScript()}.
     */
    @Test
    public void testGetScript() {
        assertNull(this.scriptHelper.getScript());
        SlingScript mockSlingScript = Mockito.mock(SlingScript.class);
        ((MockSlingScriptHelper) this.scriptHelper).setScript(mockSlingScript);
        assertSame(mockSlingScript, this.scriptHelper.getScript());
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#setScript(org.apache.sling.api.scripting.SlingScript)}.
     */
    @Test
    public void testSetScript() {
        SlingScript mockSlingScript = Mockito.mock(SlingScript.class);
        ((MockSlingScriptHelper) this.scriptHelper).setScript(mockSlingScript);
        assertSame(mockSlingScript, this.scriptHelper.getScript());
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#dispose()}.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    @Test
    public void testDispose() {
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.dispose());
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(java.lang.String, org.apache.sling.api.request.RequestDispatcherOptions)}.
     */
    @Test
    public void testForwardStringRequestDispatcherOptions() {
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward("/path", options));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testForwardStringString() {
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward("/path", ""));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(java.lang.String)}.
     */
    @Test
    public void testForwardString() {
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward("/path"));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(org.apache.sling.api.resource.Resource)}.
     */
    @Test
    public void testForwardResource() {
        Resource resource = this.resourceResolver.resolve("/path");
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward(resource));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(org.apache.sling.api.resource.Resource, java.lang.String)}.
     */
    @Test
    public void testForwardResourceString() {
        Resource resource = this.resourceResolver.resolve("/path");
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward(resource, ""));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#forward(org.apache.sling.api.resource.Resource, org.apache.sling.api.request.RequestDispatcherOptions)}.
     */
    @Test
    public void testForwardResourceRequestDispatcherOptions() {
        Resource resource = this.resourceResolver.resolve("/path");
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.forward(resource, options));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(java.lang.String, org.apache.sling.api.request.RequestDispatcherOptions)}.
     */
    @Test
    public void testIncludeStringRequestDispatcherOptions() {
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include("/path", options));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIncludeStringString() {
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include("/path", ""));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(java.lang.String)}.
     */
    @Test
    public void testIncludeString() {
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include("/path"));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(org.apache.sling.api.resource.Resource)}.
     */
    @Test
    public void testIncludeResource() {
        Resource resource = this.resourceResolver.resolve("/path");
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include(resource));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(org.apache.sling.api.resource.Resource, java.lang.String)}.
     */
    @Test
    public void testIncludeResourceString() {
        Resource resource = this.resourceResolver.resolve("/path");
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include(resource, ""));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.MockSlingScriptHelper#include(org.apache.sling.api.resource.Resource, org.apache.sling.api.request.RequestDispatcherOptions)}.
     */
    @Test
    public void testIncludeResourceRequestDispatcherOptions() {
        Resource resource = this.resourceResolver.resolve("/path");
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        assertThrows(UnsupportedOperationException.class, () -> this.scriptHelper.include(resource, options));
    }
}

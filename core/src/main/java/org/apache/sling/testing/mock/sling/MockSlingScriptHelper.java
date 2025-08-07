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

import java.lang.reflect.Array;
import java.util.Collection;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.JavaxToJakartaRequestWrapper;
import org.apache.sling.api.wrappers.JavaxToJakartaResponseWrapper;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link SlingScriptHelper} implementation.
 */
public final class MockSlingScriptHelper implements SlingScriptHelper {

    private final @NotNull SlingJakartaHttpServletRequest jakartaRequest;
    private final @NotNull SlingJakartaHttpServletResponse jakartaResponse;

    /**
     * @deprecated Use jakartaRequest instead.
     */
    @Deprecated(since = "4.1.0")
    private final @NotNull org.apache.sling.api.SlingHttpServletRequest request;

    /**
     * @deprecated Use jakartaResponse instead.
     */
    @Deprecated(since = "4.1.0")
    private final @NotNull org.apache.sling.api.SlingHttpServletResponse response;

    private final @NotNull BundleContext bundleContext;
    private SlingScript script;

    /**
     * @param request Sling HTTP servlet request
     * @param response Sling HTTP servlet response
     * @param bundleContext OSGi bundle context
     *
     * @deprecated Use {@link MockSlingScriptHelper#MockSlingScriptHelper(SlingJakartaHttpServletRequest, SlingJakartaHttpServletResponse, BundleContext)} instead.
     */
    @Deprecated(since = "4.1.0")
    public MockSlingScriptHelper(
            @NotNull final org.apache.sling.api.SlingHttpServletRequest request,
            @NotNull final org.apache.sling.api.SlingHttpServletResponse response,
            @NotNull final BundleContext bundleContext) {
        this.jakartaRequest = toJakartaRequest(request);
        this.jakartaResponse = toJakartaResponse(response);
        this.request = request;
        this.response = response;
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings("deprecation")
    private SlingJakartaHttpServletRequest toJakartaRequest(org.apache.sling.api.SlingHttpServletRequest request) {
        if (request instanceof org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest mshsr) {
            return (SlingJakartaHttpServletRequest) mshsr.getRequest();
        } else {
            return JavaxToJakartaRequestWrapper.toJakartaRequest(request);
        }
    }

    @SuppressWarnings("deprecation")
    private SlingJakartaHttpServletResponse toJakartaResponse(org.apache.sling.api.SlingHttpServletResponse response) {
        if (response instanceof org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse mshsr) {
            return (SlingJakartaHttpServletResponse) mshsr.getResponse();
        } else {
            return JavaxToJakartaResponseWrapper.toJakartaResponse(response);
        }
    }

    /**
     * @param request Sling HTTP servlet request
     * @param response Sling HTTP servlet response
     * @param bundleContext OSGi bundle context
     */
    @SuppressWarnings("deprecation")
    public MockSlingScriptHelper(
            @NotNull final SlingJakartaHttpServletRequest request,
            @NotNull final SlingJakartaHttpServletResponse response,
            @NotNull final BundleContext bundleContext) {
        this.jakartaRequest = request;
        this.jakartaResponse = response;
        this.request = new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest(
                (MockSlingJakartaHttpServletRequest) this.jakartaRequest);
        this.response = new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse(
                (MockSlingJakartaHttpServletResponse) this.jakartaResponse);
        this.bundleContext = bundleContext;
    }

    @Override
    public @NotNull SlingJakartaHttpServletRequest getJakartaRequest() {
        return this.jakartaRequest;
    }

    @Override
    public @NotNull SlingJakartaHttpServletResponse getJakartaResponse() {
        return this.jakartaResponse;
    }

    /**
     * @deprecated Use {@link #getJakartaRequest()} instead.
     */
    @Deprecated(since = "4.1.0")
    @Override
    public @NotNull org.apache.sling.api.SlingHttpServletRequest getRequest() {
        return this.request;
    }

    /**
     * @deprecated Use {@link #getJakartaResponse()} instead.
     */
    @Deprecated(since = "4.1.0")
    @Override
    public @NotNull org.apache.sling.api.SlingHttpServletResponse getResponse() {
        return this.response;
    }

    @Override
    public @Nullable <T> T getService(@NotNull final Class<T> serviceType) {
        ServiceReference<T> serviceReference = this.bundleContext.getServiceReference(serviceType);
        if (serviceReference != null) {
            return this.bundleContext.getService(serviceReference);
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T[] getServices(@NotNull final Class<T> serviceType, final String filter) {
        try {
            Collection<ServiceReference<T>> serviceReferences =
                    this.bundleContext.getServiceReferences(serviceType, filter);
            return serviceReferences.stream()
                    .map(this.bundleContext::getService)
                    .toArray(size -> (T[]) Array.newInstance(serviceType, size));
        } catch (InvalidSyntaxException ex) {
            throw new InvalidServiceFilterSyntaxException(filter, ex.getMessage(), ex);
        }
    }

    @Override
    public @NotNull SlingScript getScript() {
        return this.script;
    }

    /**
     * @param script Script
     */
    public void setScript(@NotNull SlingScript script) {
        this.script = script;
    }

    // --- unsupported operations ---
    @Override
    public void dispose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final String path, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final String path, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final Resource resource, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(@NotNull final Resource resource, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final String path, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final String path, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final Resource resource, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(@NotNull final Resource resource, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }
}

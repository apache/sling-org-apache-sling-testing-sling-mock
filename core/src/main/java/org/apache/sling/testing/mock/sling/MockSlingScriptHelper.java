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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link SlingScriptHelper} implementation.
 */
public final class MockSlingScriptHelper implements SlingScriptHelper {

    private final @NotNull SlingHttpServletRequest request;
    private final @NotNull SlingHttpServletResponse response;
    private final @NotNull BundleContext bundleContext;
    private SlingScript script;

    public MockSlingScriptHelper(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response,
            @NotNull final BundleContext bundleContext) {
        this.request = request;
        this.response = response;
        this.bundleContext = bundleContext;
    }

    @Override
    public @NotNull SlingHttpServletRequest getRequest() {
        return this.request;
    }

    @Override
    public @NotNull SlingHttpServletResponse getResponse() {
        return this.response;
    }

    @Override
    @SuppressWarnings({ "unchecked", "null" })
    public <ServiceType> ServiceType getService(@NotNull final Class<ServiceType> serviceType) {
        ServiceReference serviceReference = this.bundleContext.getServiceReference(serviceType.getName());
        if (serviceReference != null) {
            return (ServiceType) this.bundleContext.getService(serviceReference);
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(@NotNull final Class<ServiceType> serviceType, final String filter) {
        try {
            ServiceReference[] serviceReferences = this.bundleContext.getServiceReferences(serviceType.getName(),
                    filter);
            if (serviceReferences != null) {
                ServiceType[] services = (ServiceType[]) Array.newInstance(serviceType, serviceReferences.length);
                for (int i = 0; i < serviceReferences.length; i++) {
                    services[i] = (ServiceType) this.bundleContext.getService(serviceReferences[i]);
                }
                return services;
            } else {
                return (ServiceType[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
            }
        } catch (InvalidSyntaxException ex) {
            throw new InvalidServiceFilterSyntaxException(filter, ex.getMessage(), ex);
        }
    }

    @Override
    public @NotNull SlingScript getScript() {
        return this.script;
    }
    
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

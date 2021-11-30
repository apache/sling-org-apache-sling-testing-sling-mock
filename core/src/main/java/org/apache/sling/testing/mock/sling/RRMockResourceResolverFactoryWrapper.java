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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Wraps the ResourceResolverFactory from ResourceResolverMock to add a service tracker
 * for ResourceProvider (e.g. injected by ContentLoader for mounting filesystem folders).
 */
class RRMockResourceResolverFactoryWrapper implements ResourceResolverFactory,
        ServiceTrackerCustomizer<ResourceProvider, ResourceProvider> {

    private final ResourceResolverFactory delegate;
    private final BundleContext bundleContext;
    private final ServiceTracker<ResourceProvider, ResourceProvider> serviceTracker;
    private final ConcurrentMap<String, ResourceProvider> resourceProviders = new ConcurrentHashMap<>();

    @SuppressWarnings("null")
    RRMockResourceResolverFactoryWrapper(ResourceResolverFactory delegate, BundleContext bundleContext) {
        this.delegate = delegate;
        this.bundleContext = bundleContext;
        // track registered resource provider implementations
        this.serviceTracker = new ServiceTracker<ResourceProvider, ResourceProvider>(bundleContext, ResourceProvider.class, this);
        this.serviceTracker.open();
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        return wrap(delegate.getResourceResolver(authenticationInfo));
    }

    @Override
    @Deprecated
    public @NotNull ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        return wrap(delegate.getAdministrativeResourceResolver(authenticationInfo));
    }

    @Override
    public @NotNull ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        return wrap(delegate.getServiceResourceResolver(authenticationInfo));
    }

    @Override
    public @Nullable ResourceResolver getThreadResourceResolver() {
        return wrap(delegate.getThreadResourceResolver());
    }

    private ResourceResolver wrap(ResourceResolver resourceResolver) {
        return new RRMockResourceResolverWrapper(resourceResolver, resourceProviders);
    }

    @Override
    public ResourceProvider addingService(ServiceReference<ResourceProvider> reference) {
        String rootPath = getRootPath(reference);
        ResourceProvider service = bundleContext.getService(reference);
        if (rootPath != null) {
            resourceProviders.put(rootPath, service);
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<ResourceProvider> reference, ResourceProvider service) {
        // ignore
    }

    @Override
    public void removedService(ServiceReference<ResourceProvider> reference, ResourceProvider service) {
        String rootPath = getRootPath(reference);
        if (rootPath != null) {
            resourceProviders.remove(rootPath);
        }
    }

    private String getRootPath(ServiceReference<ResourceProvider> reference) {
        Object value = reference.getProperty(ResourceProvider.PROPERTY_ROOT);
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public @NotNull List<String> getSearchPath() {
        // call delegate method via reflection, as it is not available in earlier versions of Sling API
        try {
            Method getSearchPathMethod = ResourceResolverFactory.class.getMethod("getSearchPath");
            return (List)getSearchPathMethod.invoke(delegate);
        }
        catch (NoSuchMethodException | SecurityException ex) {
            // earlier version of Sling API - return empty list
            return Collections.emptyList();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException("Unable to call getSearchPath on delegate.", ex);
        }
    }

}

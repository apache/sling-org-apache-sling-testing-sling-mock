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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Factory for mock Sling objects.
 */
public final class MockSling {

    /**
     * Default resource resolver type is {@link ResourceResolverType#RESOURCERESOLVER_MOCK}.
     */
    public static final @NotNull ResourceResolverType DEFAULT_RESOURCERESOLVER_TYPE =
            ResourceResolverType.RESOURCERESOLVER_MOCK;

    private static final ThreadsafeMockAdapterManagerWrapper ADAPTER_MANAGER =
            new ThreadsafeMockAdapterManagerWrapper();

    private static final ConcurrentMap<Class<? extends ResourceResolverTypeAdapter>, Object> SNAPSHOTS =
            new ConcurrentHashMap<>();

    static {
        // register mocked adapter manager
        SlingAdaptable.setAdapterManager(ADAPTER_MANAGER);
    }

    private MockSling() {
        // static methods only
    }

    /**
     * Creates new sling resource resolver factory instance.
     * @param bundleContext Bundle context
     * @return Resource resolver factory instance
     */
    public static @NotNull ResourceResolverFactory newResourceResolverFactory(
            @NotNull final BundleContext bundleContext) {
        return newResourceResolverFactory(DEFAULT_RESOURCERESOLVER_TYPE, bundleContext);
    }

    /**
     * Creates new sling resource resolver factory instance.
     * @param type Type of underlying repository.
     * @param bundleContext Bundle context
     * @return Resource resolver factory instance
     */
    @SuppressWarnings("null")
    public static @NotNull ResourceResolverFactory newResourceResolverFactory(
            @NotNull final ResourceResolverType type, @NotNull final BundleContext bundleContext) {

        ServiceReference<ResourceResolverFactory> existingReference =
                bundleContext.getServiceReference(ResourceResolverFactory.class);
        if (existingReference != null) {
            throw new IllegalStateException(
                    "A ResourceResolverFactory is already registered in this BundleContext - please get the existing service instance.");
        }

        ResourceResolverTypeAdapter adapter = getResourceResolverTypeAdapter(type, bundleContext);
        ResourceResolverFactory factory = adapter.newResourceResolverFactory();
        if (factory == null) {
            factory = buildFactoryFromRepository(type.getNodeTypeMode(), bundleContext, adapter);
        } else {
            bundleContext.registerService(ResourceResolverFactory.class.getName(), factory, null);
        }
        return factory;
    }

    @NotNull
    static ResourceResolverFactory buildFactoryFromRepository(
            @NotNull NodeTypeMode mode, @NotNull BundleContext bundleContext, ResourceResolverTypeAdapter adapter) {
        ResourceResolverFactory factory;
        Object existingSnapshot = SNAPSHOTS.get(adapter.getClass());
        SlingRepository repository;
        if (existingSnapshot == null) {
            repository = adapter.newSlingRepository();
        } else {
            repository = adapter.newSlingRepositoryFromSnapshot(existingSnapshot);
        }
        factory = ResourceResolverFactoryInitializer.setUp(
                repository, bundleContext, existingSnapshot == null ? mode : NodeTypeMode.NOT_SUPPORTED);
        if (existingSnapshot == null) {
            Object newSnapshot = adapter.snapshot(repository);
            if (newSnapshot != null) {
                SNAPSHOTS.putIfAbsent(adapter.getClass(), newSnapshot);
            }
        }
        return factory;
    }

    @SuppressWarnings("unchecked")
    private static ResourceResolverTypeAdapter getResourceResolverTypeAdapter(
            final ResourceResolverType type, @NotNull final BundleContext bundleContext) {
        try {
            @SuppressWarnings("rawtypes")
            Class clazz = Class.forName(type.getResourceResolverTypeAdapterClass());
            try {
                Constructor<ResourceResolverTypeAdapter> bundleContextConstructor =
                        clazz.getConstructor(BundleContext.class);
                // use constructor with bundle context
                return bundleContextConstructor.newInstance(bundleContext);
            } catch (NoSuchMethodException ex) {
                // fallback to default constructor
                return (ResourceResolverTypeAdapter)
                        clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | NoSuchMethodException
                | SecurityException
                | InvocationTargetException ex) {
            throw new RuntimeException(
                    "Unable to instantiate resourcer resolver: "
                            + type.getResourceResolverTypeAdapterClass()
                            + (type.getArtifactCoordinates() != null
                                    ? ". Make sure this maven dependency is included: " + type.getArtifactCoordinates()
                                    : ""),
                    ex);
        }
    }

    /**
     * Creates new sling resource resolver instance.
     * @param type Type of underlying repository.
     * @param bundleContext Bundle context
     * @return Resource resolver instance
     */
    @SuppressWarnings("deprecation")
    public static @NotNull ResourceResolver newResourceResolver(
            @NotNull final ResourceResolverType type, @NotNull BundleContext bundleContext) {
        ResourceResolverFactory factory = newResourceResolverFactory(type, bundleContext);
        try {
            return factory.getAdministrativeResourceResolver(null);
        } catch (LoginException ex) {
            throw new RuntimeException("Mock resource resolver factory implementation seems to require login.", ex);
        }
    }

    /**
     * Creates new sling resource resolver instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param bundleContext Bundle context
     * @return Resource resolver instance
     */
    public static @NotNull ResourceResolver newResourceResolver(@NotNull BundleContext bundleContext) {
        return newResourceResolver(DEFAULT_RESOURCERESOLVER_TYPE, bundleContext);
    }

    /**
     * Creates a new sling script helper instance.
     * @param request Request
     * @param response Response
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     * @deprecated use {@link #newSlingScriptHelper(SlingJakartaHttpServletRequest, SlingJakartaHttpServletResponse, BundleContext)} instead
     */
    @Deprecated(since = "4.1.0")
    public static @NotNull SlingScriptHelper newSlingScriptHelper(
            @NotNull final org.apache.sling.api.SlingHttpServletRequest request,
            @NotNull final org.apache.sling.api.SlingHttpServletResponse response,
            @NotNull final BundleContext bundleContext) {
        return new MockSlingScriptHelper(request, response, bundleContext);
    }

    /**
     * Creates a new sling script helper instance.
     * @param request Request
     * @param response Response
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     */
    public static @NotNull SlingScriptHelper newSlingScriptHelper(
            @NotNull final SlingJakartaHttpServletRequest request,
            @NotNull final SlingJakartaHttpServletResponse response,
            @NotNull final BundleContext bundleContext) {
        return new MockSlingScriptHelper(request, response, bundleContext);
    }

    /**
     * Creates a new sling script helper instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE} for the resource resolver.
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     * @deprecated use {@link #newJakartaSlingScriptHelper(BundleContext)} instead
     */
    @Deprecated(since = "4.1.0")
    public static @NotNull SlingScriptHelper newSlingScriptHelper(@NotNull BundleContext bundleContext) {
        MockSlingJakartaHttpServletRequest jakartaRequest =
                new MockSlingJakartaHttpServletRequest(newResourceResolver(bundleContext), bundleContext);
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest(jakartaRequest);
        MockSlingJakartaHttpServletResponse jakartaResponse = new MockSlingJakartaHttpServletResponse();
        org.apache.sling.api.SlingHttpServletResponse javaxResponse =
                new org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse(jakartaResponse);
        return newSlingScriptHelper(javaxRequest, javaxResponse, bundleContext);
    }

    /**
     * Creates a new sling script helper instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE} for the resource resolver.
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     */
    public static @NotNull SlingScriptHelper newJakartaSlingScriptHelper(@NotNull BundleContext bundleContext) {
        SlingJakartaHttpServletRequest request =
                new MockSlingJakartaHttpServletRequest(newResourceResolver(bundleContext), bundleContext);
        SlingJakartaHttpServletResponse response = new MockSlingJakartaHttpServletResponse();
        return newSlingScriptHelper(request, response, bundleContext);
    }

    /**
     * Set bundle context for adapter manager. From this bundle context the
     * adapter factories are detected.
     * @param bundleContext OSGi bundle context
     */
    public static void setAdapterManagerBundleContext(@NotNull final BundleContext bundleContext) {
        ADAPTER_MANAGER.setBundleContext(bundleContext);
    }

    /**
     * Clear adapter registrations..
     */
    public static void clearAdapterManagerBundleContext() {
        ADAPTER_MANAGER.clearBundleContext();
    }
}

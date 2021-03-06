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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProvider;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;

/**
 * Initializes Sling Resource Resolver factories with JCR-resource mapping.
 */
class ResourceResolverFactoryInitializer {

    private ResourceResolverFactoryInitializer() {
        // static methods only
    }

    /**
     * Setup resource resolver factory.
     * @param slingRepository Sling repository. If null resource resolver factory is setup without any resource provider.
     * @param bundleContext Bundle context
     */
    @SuppressWarnings("null")
    public static @NotNull ResourceResolverFactory setUp(@Nullable SlingRepository slingRepository,
            @NotNull BundleContext bundleContext, @NotNull NodeTypeMode nodeTypeMode) {

        if (slingRepository != null) {
            // register sling repository as OSGi service
            registerServiceIfNotPresent(bundleContext, SlingRepository.class, slingRepository);

            // register JCR node types found in classpath
            registerJcrNodeTypes(slingRepository, nodeTypeMode);

            // initialize JCR resource provider
            ensureJcrResourceProviderDependencies(bundleContext);
            initializeJcrResourceProvider(bundleContext);
        }

        // initialize resource resolver factory activator
        ensureResourceResolverFactoryActivatorDependencies(bundleContext);
        initializeResourceResolverFactoryActivator(bundleContext);

        ServiceReference<ResourceResolverFactory> factoryRef = bundleContext.getServiceReference(ResourceResolverFactory.class);
        if (factoryRef == null) {
            throw new IllegalStateException("Unable to get ResourceResolverFactory.");
        }
        return bundleContext.getService(factoryRef);
    }

    /**
     * Ensure dependencies for JcrResourceProvider are present.
     * @param bundleContext Bundle context
     */
    @SuppressWarnings("unchecked")
    private static void ensureJcrResourceProviderDependencies(@NotNull BundleContext bundleContext) {
        if (bundleContext.getServiceReference(DynamicClassLoaderManager.class) == null) {
            bundleContext.registerService(DynamicClassLoaderManager.class, new MockDynamicClassLoaderManager(), null);
        }

        try {
            Class pathMapperClass = Class.forName("org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper");
            Object pathMapper = pathMapperClass.newInstance();
            // eliminate logger in class to suppress deprecation warnings
            try {
                Field pathMapperLoggerField = pathMapperClass.getDeclaredField("log");
                pathMapperLoggerField.setAccessible(true);
                pathMapperLoggerField.set(pathMapper, Proxy.newProxyInstance(Logger.class.getClassLoader(),
                        new Class[] { Logger.class },
                        (proxy, method, methodArgs) -> { return null; }));
            } catch (Exception ex) {
                // ignore
            }
            registerServiceIfNotPresent(bundleContext, pathMapperClass, pathMapper);
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            // ignore - service was removed in org.apache.sling.jcr.resource 3.0.0
        }
    }

    /**
     * Initialize JCR resource provider.
     * @param bundleContext Bundle context
     */
    private static void initializeJcrResourceProvider(@NotNull BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<>();
        JcrResourceProvider provider = new JcrResourceProvider();
        MockOsgi.injectServices(provider, bundleContext);
        MockOsgi.activate(provider, bundleContext, config);
        bundleContext.registerService(ResourceProvider.class, provider, config);
    }

    /**
     * Ensure dependencies for ResourceResolverFactoryActivator are present.
     * @param bundleContext Bundle context
     */
    private static void ensureResourceResolverFactoryActivatorDependencies(@NotNull BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put("user.mapping", bundleContext.getBundle().getSymbolicName() + "=admin");
        registerServiceIfNotPresent(bundleContext, ServiceUserMapper.class, ServiceUserMapperImpl.class, config);

        registerServiceIfNotPresent(bundleContext, ResourceAccessSecurityTracker.class, ResourceAccessSecurityTracker.class);
        registerServiceIfNotPresent(bundleContext, EventAdmin.class, MockEventAdmin.class);
        // dependency required since resourceresolver 1.7.0
        registerServiceIfNotPresentByName(bundleContext, "org.apache.sling.resourceresolver.impl.mapping.StringInterpolationProvider",
                "org.apache.sling.resourceresolver.impl.mapping.StringInterpolationProviderImpl");
    }

    /**
     * Initialize resource resolver factory activator.
     * @param bundleContext Bundle context
     */
    private static void initializeResourceResolverFactoryActivator(@NotNull BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<>();
        // do not required a specific resource provider (otherwise "NONE" will not work)
        config.put("resource.resolver.required.providers", "");
        config.put("resource.resolver.required.providernames", "");
        ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
        MockOsgi.injectServices(activator, bundleContext);
        MockOsgi.activate(activator, bundleContext, config);
        bundleContext.registerService(ResourceResolverFactoryActivator.class.getName(), activator, config);
    }

    @SuppressWarnings({ "unchecked", "null" })
    private static void registerServiceIfNotPresentByName(@NotNull BundleContext bundleContext,
            @NotNull String interfaceClassName, @NotNull String implClassName) {
        try {
            Class<?> interfaceClass = Class.forName(interfaceClassName);
            Class<?> implClass = Class.forName(implClassName);
            registerServiceIfNotPresent(bundleContext, (Class)interfaceClass, implClass.newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // ignore - probably not the latest sling models impl version
        }
    }

    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param instance Service instance
     */
    private static <T> void registerServiceIfNotPresent(@NotNull BundleContext bundleContext, @NotNull Class<T> serviceClass,
            @NotNull T instance) {
        registerServiceIfNotPresent(bundleContext, serviceClass, instance, new Hashtable<>());
    }

    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param instance Service instance
     * @param config OSGi config
     */
    private static <T> void registerServiceIfNotPresent(@NotNull BundleContext bundleContext, @NotNull Class<T> serviceClass,
            @NotNull T instance, Dictionary<String, Object> config) {
        if (bundleContext.getServiceReference(serviceClass.getName()) == null) {
            MockOsgi.injectServices(instance, bundleContext);
            MockOsgi.activate(instance, bundleContext, config);
            bundleContext.registerService(serviceClass, instance, config);
        }
    }

    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param implClass Implementation class
     */
    private static <T> void registerServiceIfNotPresent(@NotNull BundleContext bundleContext, @NotNull Class<T> serviceClass,
            @NotNull Class<?> implClass) {
        registerServiceIfNotPresent(bundleContext, serviceClass, implClass, new Hashtable<>());
    }

    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param implClass Implementation class
     * @param config OSGi config
     */
    @SuppressWarnings("unchecked")
    private static <T> void registerServiceIfNotPresent(@NotNull BundleContext bundleContext, @NotNull Class<T> serviceClass,
            @NotNull Class<?> implClass, Dictionary<String, Object> config) {
        if (bundleContext.getServiceReference(serviceClass.getName()) == null) {
            T instance = (T)MockOsgi.activateInjectServices(implClass, bundleContext, config);
            bundleContext.registerService(serviceClass, instance, config);
        }
    }

    /**
     * Registers all JCR node types found in classpath.
     * @param slingRepository Sling repository
     */
    @SuppressWarnings("deprecation")
    private static void registerJcrNodeTypes(final SlingRepository slingRepository,
            final NodeTypeMode nodeTypeMode) {
      Session session = null;
      try {
          session = slingRepository.loginAdministrative(null);
          NodeTypeDefinitionScanner.get().register(session, nodeTypeMode);
      }
      catch (RepositoryException ex) {
          throw new RuntimeException("Error registering JCR nodetypes: " + ex.getMessage(), ex);
      }
      finally {
          if (session != null) {
              session.logout();
          }
      }
    }

}

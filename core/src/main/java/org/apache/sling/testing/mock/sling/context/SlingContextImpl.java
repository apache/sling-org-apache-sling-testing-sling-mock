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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.resource.internal.scripting.JcrObjectsBindingsValuesProvider;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.api.ResourceBuilderFactory;
import org.apache.sling.resourcebuilder.impl.ResourceBuilderFactoryService;
import org.apache.sling.scripting.core.impl.BindingsValuesProvidersByContextImpl;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.sling.MockResourceBundleProvider;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.MockXSSAPIImpl;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.apache.sling.testing.mock.sling.services.MockSlingSettingService;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Defines Sling context objects with lazy initialization.
 * Should not be used directly but via the SlingContext JUnit rule or extension.
 */
@ConsumerType
public class SlingContextImpl extends OsgiContextImpl {

    // default to publish instance run mode
    static final @NotNull Set<String> DEFAULT_RUN_MODES = Collections.singleton("publish");

    private static final @NotNull String RESOURCERESOLVERFACTORYACTIVATOR_PID = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl";
    
    protected ResourceResolverFactory resourceResolverFactory;
    protected ResourceResolverType resourceResolverType;
    protected ResourceResolver resourceResolver;
    protected MockSlingHttpServletRequest request;
    protected MockSlingHttpServletResponse response;
    protected SlingScriptHelper slingScriptHelper;
    protected ContentLoader contentLoader;
    protected ContentLoader contentLoaderAutoCommit;
    protected ContentBuilder contentBuilder;
    protected ResourceBuilder resourceBuilder;
    protected UniqueRoot uniqueRoot;
    
    private Map<String, Object> resourceResolverFactoryActivatorProps;
    private boolean registerSlingModelsFromClassPath = true;

    /**
     * @param resourceResolverType Resource resolver type
     */
    protected void setResourceResolverType(@Nullable final ResourceResolverType resourceResolverType) {
        if (resourceResolverType == null) {
            this.resourceResolverType = MockSling.DEFAULT_RESOURCERESOLVER_TYPE;
        }
        else {
            this.resourceResolverType = resourceResolverType;
        }
    }

    protected void setResourceResolverFactoryActivatorProps(@Nullable Map<String, Object> props) {
        this.resourceResolverFactoryActivatorProps = props;
    }
    
    protected void setRegisterSlingModelsFromClassPath(boolean registerSlingModelsFromClassPath) {
        this.registerSlingModelsFromClassPath = registerSlingModelsFromClassPath;
    }

    /**
     * Setup actions before test method execution
     */
    protected void setUp() {
        super.setUp();
        MockSling.setAdapterManagerBundleContext(bundleContext());
        
        if (this.resourceResolverFactoryActivatorProps != null) {
            // use OSGi ConfigurationAdmin to pass over customized configuration to Resource Resolver Factory Activator service
            MockOsgi.setConfigForPid(bundleContext(), RESOURCERESOLVERFACTORYACTIVATOR_PID, this.resourceResolverFactoryActivatorProps);
        }
        
        // automatically register resource resolver factory when ResourceResolverType != NONE,
        // so the ResourceResolverFactory is available as OSGi service immediately
        if (resourceResolverType != ResourceResolverType.NONE) {
            resourceResolverFactory();
        }
        
        registerDefaultServices();
    }
    
    /**
     * Initialize mocked resource resolver factory.
     * @return Resource resolver factory
     */
    protected @NotNull ResourceResolverFactory newResourceResolverFactory() {
        return ContextResourceResolverFactory.get(this.resourceResolverType, bundleContext());
    }
    
    private @NotNull ResourceResolverFactory resourceResolverFactory() {
        if (this.resourceResolverFactory == null) {
            this.resourceResolverFactory = newResourceResolverFactory();
        }
        return this.resourceResolverFactory;
    }

    /**
     * Default services that should be available for every unit test
     */
    protected void registerDefaultServices() {

        // scripting services (required by sling models impl since 1.3.6)
        registerInjectActivateServiceByClassName(
                "org.apache.sling.scripting.core.impl.ScriptEngineManagerFactory",
                "org.apache.sling.scripting.core.impl.jsr223.SlingScriptEngineManager");
        registerInjectActivateService(new BindingsValuesProvidersByContextImpl());
        
        // sling models
        registerInjectActivateService(new ModelAdapterFactory());
        registerInjectActivateServiceByClassName(
                "org.apache.sling.models.impl.FirstImplementationPicker",
                "org.apache.sling.models.impl.ResourceTypeBasedResourcePicker",
                "org.apache.sling.models.impl.injectors.BindingsInjector",
                "org.apache.sling.models.impl.injectors.ChildResourceInjector",
                "org.apache.sling.models.impl.injectors.OSGiServiceInjector",
                "org.apache.sling.models.impl.injectors.RequestAttributeInjector",
                "org.apache.sling.models.impl.injectors.ResourcePathInjector",
                "org.apache.sling.models.impl.injectors.SelfInjector",
                "org.apache.sling.models.impl.injectors.SlingObjectInjector",
                "org.apache.sling.models.impl.injectors.ValueMapInjector",
                "org.apache.sling.models.impl.via.BeanPropertyViaProvider",
                "org.apache.sling.models.impl.via.ChildResourceViaProvider",
                "org.apache.sling.models.impl.via.ForcedResourceTypeViaProvider",
                "org.apache.sling.models.impl.via.ResourceSuperTypeViaProvider");

        // other services
        registerService(SlingSettingsService.class, new MockSlingSettingService(DEFAULT_RUN_MODES));
        registerService(MimeTypeService.class, new MockMimeTypeService());
        registerInjectActivateService(new ResourceBuilderFactoryService());
        registerInjectActivateService(new JcrObjectsBindingsValuesProvider());
        registerInjectActivateService(new MockResourceBundleProvider());
        registerInjectActivateService(new MockXSSAPIImpl());
        
        // scan for models defined via bundle headers in classpath
        if (registerSlingModelsFromClassPath) {
            ModelAdapterFactoryUtil.addModelsForManifestEntries(this.bundleContext());
        }
    }
    
    @SuppressWarnings("null")
    private void registerInjectActivateServiceByClassName(@NotNull String @NotNull ... classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                registerInjectActivateService(clazz.newInstance());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                // ignore - probably not the latest sling models impl version
            }
        }
    }

    /**
     * Teardown actions after test method execution
     */
    protected void tearDown() {
        
        if (this.resourceResolver != null) {
            
            // revert potential unsaved changes in resource resolver/JCR session
            try {
                this.resourceResolver.revert();
            } catch (UnsupportedOperationException ex){
                // ignore - this may happen when jcr-mock is used
            }
            Session session = this.resourceResolver.adaptTo(Session.class);
            if (session != null) {
                try {
                    session.refresh(false);
                } catch (RepositoryException ex) {
                    // ignore
                } catch (UnsupportedOperationException ex){
                    // ignore - this may happen when jcr-mock is used
                }
            }
            
            // remove unique roots
            if (this.uniqueRoot != null) {
                this.uniqueRoot.cleanUp();
            }
            
            // close resource resolver
            this.resourceResolver.close();
        }

        this.resourceResolver = null;
        this.request = null;
        this.response = null;
        this.slingScriptHelper = null;
        this.contentLoader = null;
        this.contentLoaderAutoCommit = null;
        this.contentBuilder = null;
        this.resourceBuilder = null;
        this.uniqueRoot = null;
        this.resourceResolverFactory = null;
        
        super.tearDown();

        MockSling.clearAdapterManagerBundleContext();        
    }

    /**
     * @return Resource resolver type
     */
    public final @NotNull ResourceResolverType resourceResolverType() {
        return this.resourceResolverType;
    }
    
    /**
     * Returns the singleton resource resolver bound to this context.
     * It is automatically closed after the test.
     * @return Resource resolver
     */
    @SuppressWarnings("deprecation")
    public final @NotNull ResourceResolver resourceResolver() {
        if (this.resourceResolver == null) {
            try {
                this.resourceResolver = this.resourceResolverFactory().getAdministrativeResourceResolver(null);
            } catch (LoginException ex) {
                throw new RuntimeException("Creating resource resolver failed.", ex);
            }
        }
        return this.resourceResolver;
    }
    
    /**
     * @return Sling request
     */
    public final @NotNull MockSlingHttpServletRequest request() {
        if (this.request == null) {
            this.request = new MockSlingHttpServletRequest(this.resourceResolver(), this.bundleContext());

            // initialize sling bindings
            SlingBindings bindings = new MockSlingBindings(this);
            this.request.setAttribute(SlingBindings.class.getName(), bindings);
        }
        return this.request;
    }
    
    /**
     * Dynamically resolve property request for current request {@link SlingBindings}.
     * @param property Property key
     * @param request Context request
     * @return Resolved object or null if no result found
     */
    protected @Nullable Object resolveSlingBindingProperty(@NotNull String property, @NotNull SlingHttpServletRequest request) {
        return MockSlingBindings.resolveSlingBindingProperty(this, property);
    }

    /**
     * Dynamically resolve property request for current request {@link SlingBindings}.
     * @param property Property key
     * @return Resolved object or null if no result found
     * @deprecated Please use {@link #resolveSlingBindingProperty(String, SlingHttpServletRequest)}
     */
    @Deprecated
    protected @Nullable Object resolveSlingBindingProperty(@NotNull String property) {
        return MockSlingBindings.resolveSlingBindingProperty(this, property);
    }

    /**
     * @return Request path info
     */
    public final @NotNull MockRequestPathInfo requestPathInfo() {
        return (MockRequestPathInfo)request().getRequestPathInfo();
    }

    /**
     * @return Sling response
     */
    public final @NotNull MockSlingHttpServletResponse response() {
        if (this.response == null) {
            this.response = new MockSlingHttpServletResponse();
        }
        return this.response;
    }

    /**
     * @return Sling script helper
     */
    public final @NotNull SlingScriptHelper slingScriptHelper() {
        if (this.slingScriptHelper == null) {
            this.slingScriptHelper = MockSling.newSlingScriptHelper(this.request(), this.response(),
                    this.bundleContext());
        }
        return this.slingScriptHelper;
    }

    /**
     * @return Content loader
     */
    public @NotNull ContentLoader load() {
        return load(true);
    }

    /**
     * @param autoCommit Automatically commit changes after loading content (default: true)
     * @return Content loader
     */
    public @NotNull ContentLoader load(boolean autoCommit) {
        if (autoCommit) {
            if (this.contentLoaderAutoCommit == null) {
                this.contentLoaderAutoCommit = new ContentLoader(resourceResolver(), bundleContext(), true, this.resourceResolverType());
            }
            return this.contentLoaderAutoCommit;
        }
        else {
            if (this.contentLoader == null) {
                this.contentLoader = new ContentLoader(resourceResolver(), bundleContext(), false, this.resourceResolverType());
            }
            return this.contentLoader;
        }
    }

    /**
     * Creates a {@link ContentBuilder} object for easily creating test content.
     * This API was part of Sling Mocks since version 1.x.
     * You can use alternatively the {@link #build()} method and use the {@link ResourceBuilder} API.
     * @return Content builder for building test content
     */
    public @NotNull ContentBuilder create() {
        if (this.contentBuilder == null) {
            this.contentBuilder = new ContentBuilder(resourceResolver());
        }
        return this.contentBuilder;
    }
    
    /**
     * Creates a {@link ResourceBuilder} object for easily creating test content.
     * This is a separate API which can be used inside sling mocks or in a running instance.
     * You can use alternatively the {@link #create()} method to use the {@link ContentBuilder} API.
     * @return Resource builder for building test content.
     */
    @SuppressWarnings("null")
    public @NotNull ResourceBuilder build() {
        if (this.resourceBuilder == null) {
            this.resourceBuilder = getService(ResourceBuilderFactory.class).forResolver(this.resourceResolver());
        }
        return this.resourceBuilder;
    }

    /**
     * @return Current resource
     */
    public final @Nullable Resource currentResource() {
        return request().getResource();
    }

    /**
     * Set current resource in request.
     * @param resourcePath Resource path
     * @return Current resource
     */
    public final @Nullable Resource currentResource(@Nullable String resourcePath) {
        if (resourcePath != null) {
            Resource resource = resourceResolver().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalArgumentException("Resource does not exist: " + resourcePath);
            }
            return currentResource(resource);
        } else {
            return currentResource((Resource)null);
        }
    }

    /**
     * Set current resource in request.
     * @param resource Resource
     * @return Current resource
     */
    public @Nullable Resource currentResource(@Nullable Resource resource) {
        request().setResource(resource);
        return resource;
    }

    /**
     * Search classpath for given java package names (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageName Java package name
     */
    public final void addModelsForPackage(@NotNull String packageName) {
        ModelAdapterFactoryUtil.addModelsForPackages(bundleContext(),  packageName);
    }

    /**
     * Search classpath for given java package names (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageNames Java package names
     */
    public final void addModelsForPackage(@NotNull String @NotNull ... packageNames) {
        ModelAdapterFactoryUtil.addModelsForPackages(bundleContext(), packageNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param classNames Java class names
     */
    public final void addModelsForClasses(@NotNull String @NotNull ... classNames) {
        ModelAdapterFactoryUtil.addModelsForClasses(bundleContext(), classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param classes Java classes
     */
    public final void addModelsForClasses(@NotNull Class @NotNull ... classes) {
        ModelAdapterFactoryUtil.addModelsForClasses(bundleContext(), classes);
    }

    /**
     * Set current run mode(s).
     * @param runModes Run mode(s).
     */
    public final void runMode(@NotNull String @NotNull ... runModes) {
        Set<String> newRunModes = new HashSet<>(Arrays.asList(runModes));
        ServiceReference<SlingSettingsService> ref = bundleContext().getServiceReference(SlingSettingsService.class);
        if (ref != null) {
            MockSlingSettingService slingSettings = (MockSlingSettingService)bundleContext().getService(ref);
            slingSettings.setRunModes(newRunModes);
        }
    }
    
    /**
     * Create unique root paths for unit tests (and clean them up after the test run automatically).
     * @return Unique root path helper
     */
    public @NotNull UniqueRoot uniqueRoot() {
        if (uniqueRoot == null) {
            uniqueRoot = new UniqueRoot(this);
        }
        return uniqueRoot;
    }
    
    /**
     * Create a Sling AdapterFactory on the fly which can adapt from <code>adaptableClass</code>
     * to <code>adapterClass</code> and just returns the given value as result.
     * @param adaptableClass Class to adapt from
     * @param adapterClass Class to adapt to
     * @param adapter Object which is always returned for this adaption.
     * @param <T1> Adaptable type
     * @param <T2> Adapter type
     */
    public final <T1, T2> void registerAdapter(@NotNull final Class<T1> adaptableClass, @NotNull final Class<T2> adapterClass,
            @NotNull final T2 adapter) {
        registerAdapter(adaptableClass, adapterClass, new Function<T1, T2>() {
            @Override
            public T2 apply(@Nullable T1 input) {
                return adapter;
            }
        });
    }

    /**
     * Create a Sling AdapterFactory on the fly which can adapt from <code>adaptableClass</code>
     * to <code>adapterClass</code> and delegates the adapter mapping to the given <code>adaptHandler</code> function.
     * @param adaptableClass Class to adapt from
     * @param adapterClass Class to adapt to
     * @param adaptHandler Function to handle the adaption
     * @param <T1> Adaptable type
     * @param <T2> Adapter type
     */
    public final <T1, T2> void registerAdapter(@NotNull final Class<T1> adaptableClass, @NotNull final Class<T2> adapterClass,
            @NotNull final Function<T1,T2> adaptHandler) {
        AdapterFactory adapterFactory = new AdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <AdapterType> AdapterType getAdapter(@NotNull Object adaptable, @NotNull Class<AdapterType> type) {
                return (AdapterType)adaptHandler.apply((T1)adaptable);
            }
        };
        Map<String,Object> props = new HashMap<>();
        props.put(AdapterFactory.ADAPTABLE_CLASSES, new String[] {
                adaptableClass.getName()
            });
        props.put(AdapterFactory.ADAPTER_CLASSES, new String[] {
                adapterClass.getName()
            });
        // make sure this overlay has higher ranking than other adapter factories
        // normally we should use Integer.MAX_VALUE for this - but due to SLING-7194 prefers lowest-ranking services first
        props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
        registerService(AdapterFactory.class, adapterFactory, props);
    }

}

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
package org.apache.sling.testing.mock.sling.junit5;

import java.util.Map;

import org.apache.sling.testing.mock.osgi.context.ContextPlugins;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Sling Mock parameter object with resource resolver type defaulting to
 * {@link ResourceResolverType#RESOURCERESOLVER_MOCK}.
 * <p>
 * Additionally you can subclass this class and provide further parameters via
 * {@link SlingContextBuilder}.
 * </p>
 */
@ConsumerType
public final class SlingContext extends SlingContextImpl {

    private final ContextPlugins plugins;
    private boolean isSetUp;

    /**
     * Initialize Sling context.
     */
    public SlingContext() {
      this(new ContextPlugins(), null, MockSling.DEFAULT_RESOURCERESOLVER_TYPE, true);
    }

    /**
     * Initialize Sling context.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(@NotNull final ResourceResolverType resourceResolverType) {
        this(new ContextPlugins(), null, resourceResolverType, true);
    }

    /**
     * Initialize Sling context.
     * @param contextPlugins Context plugins
     * @param resourceResolverFactoryActivatorProps Resource resolver factory
     *            activator properties
     * @param registerSlingModelsFromClassPath Automatic registering of all
     *            Sling Models found in the classpath on startup.
     * @param resourceResolverType Resource resolver type.
     */
    SlingContext(@NotNull final ContextPlugins contextPlugins,
            @Nullable final Map<String, Object> resourceResolverFactoryActivatorProps,
            @Nullable final ResourceResolverType resourceResolverType,
            final boolean registerSlingModelsFromClassPath) {

        this.plugins = contextPlugins;
        setResourceResolverFactoryActivatorProps(resourceResolverFactoryActivatorProps);
        setRegisterSlingModelsFromClassPath(registerSlingModelsFromClassPath);
        setResourceResolverType(resourceResolverType);
    }

    /**
     * This is called by {@link SlingContextExtension} to set up context.
     */
    protected void setUpContext() {
        isSetUp = true;
        plugins.executeBeforeSetUpCallback(this);
        super.setUp();
    }

    /**
     * This is called by {@link SlingContextExtension} to tear down context.
     */
    protected void tearDownContext() {
        super.tearDown();
    }

    ContextPlugins getContextPlugins() {
        return plugins;
    }

    boolean isSetUp() {
        return this.isSetUp;
    }

}

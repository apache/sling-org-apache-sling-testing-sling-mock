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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.script.Bindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.LazyBindings;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Mock extension of {@link SlingBindings} that dynamically evaluates properties read from SlingBindings from the current mock context.
 * Normally the SlingBingings are set statically for each script execution, but in mocks where no script is really executed
 * it's easier to evaluate them from current context.
 */
class MockSlingBindings extends SlingBindings implements EventHandler {
    private static final long serialVersionUID = 1L;

    private static final String PROP_CURRENT_NODE = "currentNode";
    private static final String PROP_CURRENT_SESSION = "currentSession";

    /**
     * OSGi service property to set to "true" on BindingsValuesProvider implementations that should be ignored
     * when populating the "non-dynamic" bindings properties.
     */
    static final String SERVICE_PROPERTY_MOCK_SLING_BINDINGS_IGNORE = "MockSlingBindings-ignore";

    private volatile SlingContextImpl context;

    public MockSlingBindings(SlingContextImpl context) {
        this.context = context;
        populateFromBindingsValuesProvider();
    }

    @Override
    public Object get(Object key) {
        if (this.context == null) {
            return null;
        }
        if (key instanceof String) {
            Object result = context.resolveSlingBindingProperty((String) key, context.request());
            if (result != null) {
                return result;
            }
        }
        return super.get(key);
    }

    /**
     * Removes all (non-dynamic) properties from bindings and populates them from all registered {@link BindingsValuesProvider} implementations.
     */
    private void populateFromBindingsValuesProvider() {
        Bindings bindings = new LazyBindings();
        for (BindingsValuesProvider provider : context.getServices(
                BindingsValuesProvider.class, "(!(" + SERVICE_PROPERTY_MOCK_SLING_BINDINGS_IGNORE + "=true))")) {
            provider.addBindings(bindings);
        }
        this.clear();
        // if a provider added properties which are evaluated lazily, they are evaluated here.
        this.putAll(bindings);
    }

    @Override
    public void handleEvent(Event event) {
        if (this.context == null) {
            return;
        }
        // is triggered by OSGi events fired by {@link
        // org.apache.sling.scripting.core.impl.BindingsValuesProvidersByContextImpl}
        // whenever a new bindings value provider is added or removed
        populateFromBindingsValuesProvider();
    }

    static @Nullable Object resolveSlingBindingProperty(@NotNull SlingContextImpl context, @NotNull String property) {

        // -- Sling --
        if (StringUtils.equals(property, RESOLVER)) {
            return context.resourceResolver();
        }
        if (StringUtils.equals(property, RESOURCE)) {
            return context.currentResource();
        }
        if (StringUtils.equals(property, REQUEST)) {
            return context.request();
        }
        if (StringUtils.equals(property, RESPONSE)) {
            return context.response();
        }
        if (StringUtils.equals(property, SLING)) {
            return context.slingScriptHelper();
        }
        if (StringUtils.equals(property, READER)) {
            return context.request().getReader();
        }
        if (StringUtils.equals(property, OUT)) {
            return context.response().getWriter();
        }

        // -- JCR --
        // this emulates behavior of org.apache.sling.jcr.resource.internal.scripting.JcrObjectsBindingsValuesProvider
        if (StringUtils.equals(property, PROP_CURRENT_NODE)) {
            Resource resource = context.currentResource();
            if (resource != null) {
                return resource.adaptTo(Node.class);
            }
        }
        if (StringUtils.equals(property, PROP_CURRENT_SESSION)) {
            return context.resourceResolver().adaptTo(Session.class);
        }

        return null;
    }

    public void tearDown() {
        this.context = null;
    }
}

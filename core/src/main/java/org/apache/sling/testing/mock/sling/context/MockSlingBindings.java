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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock extension of {@link SlingBindings} that dynamically evaluates properties read from SlingBindings from the current mock context.
 * Normally the SlingBingings are set statically for each script execution, but in mocks where no script is really executed
 * it's easier to evaluate them from current context. 
 */
class MockSlingBindings extends SlingBindings {
    private static final long serialVersionUID = 1L;

    private static final String PROP_CURRENT_NODE = "currentNode";
    private static final String PROP_CURRENT_SESSION = "currentSession";
    
    private final SlingContextImpl context;

    public MockSlingBindings(SlingContextImpl context) {
        this.context = context;
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            Object result = context.resolveSlingBindingProperty((String)key, context.request());
            if (result != null) {
                return result;
            }
        }
        return super.get(key);
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

}

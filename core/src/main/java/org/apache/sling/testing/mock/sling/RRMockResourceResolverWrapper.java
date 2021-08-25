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

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps resource resolver form ResourceResolverMock to check for resource provider implementations
 * registered to specific path. This is a very simplified implementation to overlay the mocked
 * resource tree with resource providers, but should be sufficient for unit tests.
 */
class RRMockResourceResolverWrapper extends ResourceResolverWrapper implements ResolveContext {

    private final ConcurrentMap<String, ResourceProvider> resourceProviders;

    RRMockResourceResolverWrapper(ResourceResolver delegate, ConcurrentMap<String, ResourceProvider> resourceProviders) {
        super(delegate);
        this.resourceProviders = resourceProviders;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Resource getResource(@NotNull String path) {
        ResourceProvider resourceProvider = getMatchingResourceProvider(path);
        if (resourceProvider != null) {
            return resourceProvider.getResource(this, path, ResourceContext.EMPTY_CONTEXT, null);
        }
        return super.getResource(path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Iterator<Resource> listChildren(@NotNull Resource parent) {
        ResourceProvider resourceProvider = getMatchingResourceProvider(parent.getPath());
        if (resourceProvider != null) {
            Iterator<Resource> result = resourceProvider.listChildren(this,  parent);
            if (result == null) {
                return Collections.emptyIterator();
            }
            else {
                return result;
            }
        }
        return super.listChildren(parent);
    }

    private ResourceProvider getMatchingResourceProvider(String path) {
        if (resourceProviders.isEmpty()) {
            return null;
        }
        String normalizedPath = ResourceUtil.normalize(path);
        if (!StringUtils.startsWith(normalizedPath, "/")) {
            return null;
        }
        return getMatchingResourceProviderRecursively(normalizedPath);
    }

    private ResourceProvider getMatchingResourceProviderRecursively(String path) {
        ResourceProvider provider = resourceProviders.get(path);
        if (provider != null) {
            return provider;
        }
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath != null) {
            return getMatchingResourceProviderRecursively(parentPath);
        }
        else {
            return null;
        }
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver() {
        return this;
    }

    @Override
    public @Nullable Object getProviderState() {
        return null;
    }

    @Override
    public @Nullable ResolveContext getParentResolveContext() {
        return null;
    }

    @Override
    public @Nullable ResourceProvider getParentResourceProvider() {
        return null;
    }

}

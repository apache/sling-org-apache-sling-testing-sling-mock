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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingException;
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
        if (resourceProviders.isEmpty()) {
            return super.getResource(path);
        }
        List<String> normalizedPaths = getNormalizedPaths(path);
        for (String normalizedPath : normalizedPaths) {
            ResourceProvider resourceProvider = getMatchingResourceProvider(normalizedPath);
            if (resourceProvider != null) {
                return resourceProvider.getResource(this, normalizedPath, ResourceContext.EMPTY_CONTEXT, null);
            }
            return super.getResource(path);
        }
        return null;
    }

    @Override
    public Resource getResource(Resource base, String path) {
        if (resourceProviders.isEmpty()) {
            return super.getResource(base, path);
        }
        String normalizedPath = ResourceUtil.normalize(base.getPath() + "/" + path);
        if (normalizedPath != null) {
            ResourceProvider resourceProvider = getMatchingResourceProvider(normalizedPath);
            if (resourceProvider != null) {
                return resourceProvider.getResource(this, normalizedPath, ResourceContext.EMPTY_CONTEXT, null);
            }
            return super.getResource(path);
        }
        return null;
    }

    // duplicated method from MockResourceResolver to ensure resources from resource providers are respected as well
    @Override
    public Resource getParent(@NotNull Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(parentPath);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Iterator<Resource> listChildren(@NotNull Resource parent) {
        if (resourceProviders.isEmpty()) {
            return super.listChildren(parent);
        }
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

    @Override
    public @NotNull Iterable<Resource> getChildren(@NotNull Resource parent) {
        return () -> listChildren(parent);
    }

    @Override
    public boolean hasChildren(@NotNull Resource resource) {
        return listChildren(resource).hasNext();
    }



    private ResourceProvider getMatchingResourceProvider(String path) {
        ResourceProvider provider = resourceProviders.get(path);
        if (provider != null) {
            return provider;
        }
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath != null) {
            return getMatchingResourceProvider(parentPath);
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

    private List<String> getNormalizedPaths(String path) {
        List<String> result = new ArrayList<>();
        if (StringUtils.startsWith(path, "/")) {
            result.add(ResourceUtil.normalize(path));
        }
        else {
            for (String searchPath : getSearchPath()) {
                String combinedPath = StringUtils.removeEnd(searchPath, "/") + "/" + path;
                result.add(ResourceUtil.normalize(combinedPath));
            }
        }
        return result;
    }

    // duplicated method from MockResourceResolver to ensure resources from resource providers are respected as well
    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        if (resourceProviders.isEmpty()) {
            return super.isResourceType(resource, resourceType);
        }
        boolean result = false;
        if ( resource != null && resourceType != null ) {
             // Check if the resource is of the given type. This method first checks the
             // resource type of the resource, then its super resource type and continues
             //  to go up the resource super type hierarchy.
             if (ResourceTypeUtil.areResourceTypesEqual(resourceType, resource.getResourceType(), getSearchPath())) {
                 result = true;
             } else {
                 Set<String> superTypesChecked = new HashSet<>();
                 String superType = this.getParentResourceType(resource);
                 while (!result && superType != null) {
                     if (ResourceTypeUtil.areResourceTypesEqual(resourceType, superType, getSearchPath())) {
                         result = true;
                     } else {
                         superTypesChecked.add(superType);
                         superType = this.getParentResourceType(superType);
                         if (superType != null && superTypesChecked.contains(superType)) {
                             throw new SlingException("Cyclic dependency for resourceSuperType hierarchy detected on resource " + resource.getPath()) {
                                // anonymous class to avoid problem with null cause
                                private static final long serialVersionUID = 1L;
                             };
                         }
                     }
                 }
             }

        }
        return result;
    }

    // duplicated method from MockResourceResolver to ensure resources from resource providers are respected as well
    @Override
    public String getParentResourceType(Resource resource) {
        if (resourceProviders.isEmpty()) {
            return super.getParentResourceType(resource);
        }
        String resourceSuperType = null;
        if ( resource != null ) {
            resourceSuperType = resource.getResourceSuperType();
            if (resourceSuperType == null) {
                resourceSuperType = this.getParentResourceType(resource.getResourceType());
            }
        }
        return resourceSuperType;
    }

    // duplicated method from MockResourceResolver to ensure resources from resource providers are respected as well
    @Override
    public String getParentResourceType(String resourceType) {
        if (resourceProviders.isEmpty()) {
            return super.getParentResourceType(resourceType);
        }
        // normalize resource type to a path string
        final String rtPath = (resourceType == null ? null : ResourceUtil.resourceTypeToPath(resourceType));
        // get the resource type resource and check its super type
        String resourceSuperType = null;
        if ( rtPath != null ) {
            final Resource rtResource = getResource(rtPath);
            if (rtResource != null) {
                resourceSuperType = rtResource.getResourceSuperType();
            }
        }
        return resourceSuperType;
    }



    /**
     * Some helper methods for doing comparisons on resource types.
     * This class is private the resource resolver bundle.
     * Consumers should rely on {@link Resource#isResourceType(String)} or {@link ResourceResolver#isResourceType(Resource, String)} instead.
     *
     * <p>
     *   This is a copy of org.apache.sling.resourceresolver.impl.ResourceTypeUtil which is an internal class
     *   and its signature has changed between releases.
     * </p>
     */
    static class ResourceTypeUtil {

        /**
         * Returns <code>true</code> if the given resource type are equal.
         *
         * In case the value of any of the given resource types
         * starts with one of the resource resolver's search paths
         * it is converted to a relative resource type by stripping off
         * the resource resolver's search path before doing the comparison.
         *
         * @param resourceType A resource type
         * @param anotherResourceType Another resource type to compare with {@link resourceType}.
         * @return <code>true</code> if the resource type equals the given resource type.
         */
        public static boolean areResourceTypesEqual(@NotNull String resourceType, @NotNull String anotherResourceType, String @NotNull [] searchPath) {
            return relativizeResourceType(resourceType, searchPath).equals(relativizeResourceType(anotherResourceType, searchPath));
        }

        /**
         * Makes the given resource type relative by stripping off any prefix which equals one of the given search paths.
         * In case the given resource type does not start with any of the given search paths it is returned unmodified.
         * @param resourceType the resourceType to relativize.
         * @param searchPath the search paths to strip off from the given resource type.
         * @return the relative resource type
         */
        public static String relativizeResourceType(@NotNull String resourceType, String @NotNull [] searchPath) {
            if (resourceType.startsWith("/")) {
                for (String prefix : searchPath) {
                    if (resourceType.startsWith(prefix)) {
                        return resourceType.substring(prefix.length());
                    }
                }
            }
            return resourceType;
        }

    }

}

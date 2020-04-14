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
package org.apache.sling.testing.mock.sling.builder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for building test content in the resource hierarchy with as less
 * boilerplate code as possible.
 */
public class ContentBuilder {

    static final String DUMMY_TEMPLATE = "/apps/sample/templates/template1";

    protected final ResourceResolver resourceResolver;

    /**
     * @param resourceResolver Resource resolver
     */
    public ContentBuilder(@NotNull ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Create resource. If parent resource(s) do not exist they are created
     * automatically using <code>nt:unstructured</code> nodes.
     * @param path Page path
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull String path) {
        return resource(path, ValueMap.EMPTY);
    }

    /**
     * Create resource. If parent resource(s) do not exist they are created
     * automatically using <code>nt:unstructured</code> nodes.
     * @param path Page path
     * @param properties Properties for resource.
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull String path, @NotNull Map<String, Object> properties) {
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        
        // check if properties map contains maps representing child resources
        Map<String,Object> propertiesWihtoutChildren;
        Map<String,Map<String,Object>> children = getChildMaps(properties);
        if (!children.isEmpty()) {
            propertiesWihtoutChildren = new HashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!children.containsKey(entry.getKey())) {
                    propertiesWihtoutChildren.put(entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            propertiesWihtoutChildren = properties;
        }
        
        // create resource
        Resource parentResource = ensureResourceExists(parentPath);
        String name = ResourceUtil.getName(path);
        Resource newResource;
        try {
            newResource = resourceResolver.create(parentResource, name, propertiesWihtoutChildren);
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at " + path, ex);
        }
        
        // create child resources
        for (Map.Entry<String,Map<String,Object>> entry : children.entrySet()) {
            resource(newResource, entry.getKey(), entry.getValue());
        }
        
        return newResource;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String,Map<String,Object>> getChildMaps(Map<String,Object> properties) {
        Map<String,Map<String,Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof Map) {
                result.put(entry.getKey(), (Map)entry.getValue());
            }
        }
        return result;
    }

    /**
     * Create resource. If parent resource(s) do not exist they are created
     * automatically using <code>nt:unstructured</code> nodes.
     * @param path Page path
     * @param properties Properties for resource.
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull String path, @NotNull Object @NotNull ... properties) {
        return resource(path, MapUtil.toMap(properties));
    }

    /**
     * Create child resource below the given parent resource.
     * @param resource Parent resource
     * @param name Child resource name
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull Resource resource, @NotNull String name) {
        return resource(resource, name, ValueMap.EMPTY);
    }

    /**
     * Create child resource below the given parent resource.
     * @param resource Parent resource
     * @param name Child resource name
     * @param properties Properties for resource.
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull Resource resource, @NotNull String name, @NotNull Map<String, Object> properties) {
        String path = resource.getPath() + "/" + StringUtils.stripStart(name, "/");
        return resource(path, properties);
    }

    /**
     * Create child resource below the given parent resource.
     * @param resource Parent resource
     * @param name Child resource name
     * @param properties Properties for resource.
     * @return Resource object
     */
    public final @NotNull Resource resource(@NotNull Resource resource, @NotNull String name, @NotNull Object @NotNull ... properties) {
        return resource(resource, name, MapUtil.toMap(properties));
    }

    /**
     * Ensure that a resource exists at the given path. If not, it is created
     * using <code>nt:unstructured</code> node type.
     * @param path Resource path
     * @return Resource at path (existing or newly created)
     */
    @SuppressWarnings("null")
    protected final @NotNull Resource ensureResourceExists(@NotNull String path) {
        if (StringUtils.isEmpty(path) || StringUtils.equals(path, "/")) {
            return resourceResolver.getResource("/");
        }
        Resource resource = resourceResolver.getResource(path);
        if (resource != null) {
            return resource;
        }
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        String name = ResourceUtil.getName(path);
        Resource parentResource = ensureResourceExists(parentPath);
        try {
            resource = resourceResolver.create(parentResource, name,
                    ImmutableValueMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
            resourceResolver.commit();
            return resource;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at " + path, ex);
        }
    }

}

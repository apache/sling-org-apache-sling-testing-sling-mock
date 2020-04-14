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
package org.apache.sling.testing.mock.sling.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.ParserOptions;
import org.apache.sling.contentparser.json.JSONParserFeature;
import org.apache.sling.contentparser.json.JSONParserOptions;
import org.apache.sling.contentparser.json.internal.JSONContentParser;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ImmutableValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Imports JSON data and binary data into Sling resource hierarchy.
 * After all import operations from json or binaries {@link ResourceResolver#commit()} is called (when autocommit mode is active).
 */
public final class ContentLoader {

    private static final String CONTENTTYPE_OCTET_STREAM = "application/octet-stream";

    // set of resource or property names that are ignored for all resource resolver types
    private static final Set<String> SHARED_IGNORED_NAMES = Stream.of(
            JcrConstants.JCR_BASEVERSION,
            JcrConstants.JCR_PREDECESSORS,
            JcrConstants.JCR_SUCCESSORS,
            JcrConstants.JCR_VERSIONHISTORY,
            "jcr:checkedOut",
            "jcr:isCheckedOut",
            "rep:policy")
            .collect(Collectors.toSet());
    
    // set of resource or property names that are ignored when other resource resolver types than JCR_OAK are used
    private static final Set<String> MOCK_IGNORED_NAMES = Stream.concat(
            SHARED_IGNORED_NAMES.stream(), Stream.of(
            JcrConstants.JCR_MIXINTYPES))
            .collect(Collectors.toSet());
    
    // set of resource or property names that are ignored when JCR_OAK resource resolver type (= a real repo impl) is used
    private static final Set<String> OAK_IGNORED_NAMES = Stream.concat(
            SHARED_IGNORED_NAMES.stream(), Stream.of(
            JcrConstants.JCR_UUID,
            JcrConstants.JCR_CREATED))
            .collect(Collectors.toSet()); 
    
    private final ResourceResolver resourceResolver;
    private final BundleContext bundleContext;
    private final boolean autoCommit;
    private final Set<String> ignoredNames;
    private final ContentParser jsonParser;
    private final ParserOptions jsonParserOptions;

    /**
     * @param resourceResolver Resource resolver
     */
    public ContentLoader(@NotNull ResourceResolver resourceResolver) {
        this(resourceResolver, null);
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     */
    public ContentLoader(@NotNull ResourceResolver resourceResolver, @Nullable BundleContext bundleContext) {
        this(resourceResolver, bundleContext, true);
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     * @param autoCommit Automatically commit changes after loading content (default: true)
     */
    public ContentLoader(@NotNull ResourceResolver resourceResolver, @Nullable BundleContext bundleContext, boolean autoCommit) {
        this(resourceResolver, bundleContext, autoCommit, null);
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     * @param autoCommit Automatically commit changes after loading content (default: true)
     * @param resourceResolverType Resource resolver type.
     */
    public ContentLoader(@NotNull ResourceResolver resourceResolver, @Nullable BundleContext bundleContext, boolean autoCommit,
            @Nullable ResourceResolverType resourceResolverType) {
        this.resourceResolver = resourceResolver;
        this.bundleContext = bundleContext;
        this.autoCommit = autoCommit;
        this.ignoredNames = getIgnoredNamesForResourceResolverType(resourceResolverType);
        this.jsonParserOptions = new JSONParserOptions()
            .withFeatures(EnumSet.of(JSONParserFeature.COMMENTS, JSONParserFeature.QUOTE_TICK))
            .detectCalendarValues(true)
            .ignorePropertyNames(this.ignoredNames)
            .ignoreResourceNames(this.ignoredNames);
        // JSONContentParser is an OSGi service - for sake of simplicity in this mock environment instantiate it directly
        this.jsonParser = new JSONContentParser();
    }
    
    private final Set<String> getIgnoredNamesForResourceResolverType(ResourceResolverType resourceResolverType) {
        if (resourceResolverType == null || resourceResolverType == ResourceResolverType.JCR_OAK) {
            return OAK_IGNORED_NAMES;
        }
        else {
            return MOCK_IGNORED_NAMES;
        }
    }

    /**
     * Import content of JSON file into repository.
     * @param classpathResource Classpath resource URL for JSON content
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with JSON content
     * @return Resource
     */
    public @NotNull Resource json(@NotNull String classpathResource, @NotNull Resource parentResource, @NotNull String childName) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return json(is, parentResource, childName);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import content of JSON file into repository. Auto-creates parent
     * hierarchies as nt:unstrucured nodes if missing.
     * @param classpathResource Classpath resource URL for JSON content
     * @param destPath Path to import the JSON content to
     * @return Resource
     */
    public @NotNull Resource json(@NotNull String classpathResource, @NotNull String destPath) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return json(is, destPath);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import content of JSON file into repository.
     * @param inputStream Input stream with JSON content
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with JSON content
     * @return Resource
     */
    public @NotNull Resource json(@NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String childName) {
        return json(inputStream, parentResource.getPath() + "/" + childName);
    }

    /**
     * Import content of JSON file into repository. Auto-creates parent
     * hierarchies as nt:unstrucured nodes if missing.
     * @param inputStream Input stream with JSON content
     * @param destPath Path to import the JSON content to
     * @return Resource
     */
    @SuppressWarnings("null")
    public @NotNull Resource json(@NotNull InputStream inputStream, @NotNull String destPath) {
        try {
            String parentPath = ResourceUtil.getParent(destPath);
            String childName = ResourceUtil.getName(destPath);
            
            if (parentPath == null) {
                throw new IllegalArgumentException("Path has no parent: " + destPath);
            }

            Resource parentResource = resourceResolver.getResource(parentPath);
            if (parentResource == null) {
                parentResource = createResourceHierarchy(parentPath);
            }
            if (parentResource.getChild(childName) != null) {
                throw new IllegalArgumentException("Resource does already exist: " + destPath);
            }

            LoaderContentHandler contentHandler = new LoaderContentHandler(destPath, resourceResolver);
            jsonParser.parse(contentHandler, inputStream, jsonParserOptions);
            if (autoCommit) {
                resourceResolver.commit();
            }
            return resourceResolver.getResource(destPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private @NotNull Resource createResourceHierarchy(@NotNull String path) {
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            parentResource = createResourceHierarchy(parentPath);
        }
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        try {
            return resourceResolver.create(parentResource, ResourceUtil.getName(path), props);
        } catch (PersistenceException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing. Mime type is
     * auto-detected from either {@code classpathResource} or {@code path}.
     * @param classpathResource Classpath resource URL for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull String classpathResource, @NotNull String path) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return binaryFile(is, path, detectMimeTypeFromNames(classpathResource, path));
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing.
     * @param classpathResource Classpath resource URL for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull String classpathResource, @NotNull String path, @NotNull String mimeType) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return binaryFile(is, path, mimeType);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing. Mime type is
     * auto-detected from resource name.
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull InputStream inputStream, @NotNull String path) {
        return binaryFile(inputStream, path, detectMimeTypeFromNames(path));
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing.
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull InputStream inputStream, @NotNull String path, @NotNull String mimeType) {
        String parentPath = ResourceUtil.getParent(path, 1);
        String name = ResourceUtil.getName(path);
        if (parentPath == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            parentResource = createResourceHierarchy(parentPath);
        }
        return binaryFile(inputStream, parentResource, name, mimeType);
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing. Mime type is
     * auto-detected from resource name.
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:file
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name) {
        return binaryFile(inputStream, parentResource, name, detectMimeTypeFromNames(name));
    }

    /**
     * Import binary file as nt:file binary node into repository. Auto-creates
     * parent hierarchies as nt:unstrucured nodes if missing.
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:file
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name, @NotNull String mimeType) {
        try {
            Resource file = resourceResolver.create(parentResource, name,
                    ImmutableValueMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE));
            resourceResolver.create(file, JcrConstants.JCR_CONTENT,
                    ImmutableValueMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE,
                            JcrConstants.JCR_DATA, inputStream,
                            JcrConstants.JCR_MIMETYPE, mimeType));
            if (autoCommit) {
                resourceResolver.commit();
            }
            return file;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at " + parentResource.getPath() + "/" + name, ex);
        }
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing. Mime
     * type is auto-detected from {@code classpathResource} or {@code path}.
     * @param classpathResource Classpath resource URL for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull String classpathResource, @NotNull String path) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return binaryResource(is, path, detectMimeTypeFromNames(classpathResource, path));
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing.
     * @param classpathResource Classpath resource URL for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull String classpathResource, @NotNull String path, @NotNull String mimeType) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }
        try {
            return binaryResource(is, path, mimeType);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing. Mime
     * type is auto-detected from resource name.
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull InputStream inputStream, @NotNull String path) {
        return binaryResource(inputStream, path, detectMimeTypeFromNames(path));
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing.
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull InputStream inputStream, @NotNull String path, @NotNull String mimeType) {
        String parentPath = ResourceUtil.getParent(path, 1);
        String name = ResourceUtil.getName(path);
        if (parentPath == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            parentResource = createResourceHierarchy(parentPath);
        }
        return binaryResource(inputStream, parentResource, name, mimeType);
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing. Mime
     * type is auto-detected from resource name.
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:resource
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name) {
        return binaryResource(inputStream, parentResource, name, detectMimeTypeFromNames(name));
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * Auto-creates parent hierarchies as nt:unstrucured nodes if missing.
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:resource
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name, @NotNull String mimeType) {
        try {
            Resource resource = resourceResolver.create(parentResource, name,
                    ImmutableValueMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE,
                            JcrConstants.JCR_DATA, inputStream,
                            JcrConstants.JCR_MIMETYPE, mimeType));
            if (autoCommit) {
                resourceResolver.commit();
            }
            return resource;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at " + parentResource.getPath() + "/" + name, ex);
        }
    }

    /**
     * Detected mime type from any of the given names (evaluating the file extension) using Mime Type service.
     * Fallback to application/octet-stream.
     * @param names The names from which to derive the mime type
     * @return Mime type (never null)
     */
    private @NotNull String detectMimeTypeFromNames(@NotNull String @NotNull ... names) {
        String mimeType = null;
        for (String name : names) {
            String fileExtension = StringUtils.substringAfterLast(name, ".");
            if (bundleContext != null && StringUtils.isNotEmpty(fileExtension)) {
                ServiceReference<MimeTypeService> ref = bundleContext.getServiceReference(MimeTypeService.class);
                if (ref != null) {
                    MimeTypeService mimeTypeService = (MimeTypeService)bundleContext.getService(ref);
                    mimeType = mimeTypeService.getMimeType(fileExtension);
                    break;
                }
            }
        }
        return StringUtils.defaultString(mimeType, CONTENTTYPE_OCTET_STREAM);
    }

}

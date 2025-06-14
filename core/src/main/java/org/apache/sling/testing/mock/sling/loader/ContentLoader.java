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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.ParserOptions;
import org.apache.sling.contentparser.json.JSONParserFeature;
import org.apache.sling.contentparser.json.JSONParserOptions;
import org.apache.sling.contentparser.json.internal.JSONContentParser;
import org.apache.sling.contentparser.xml.jcr.internal.JCRXMLContentParser;
import org.apache.sling.fsprovider.internal.FsResourceProvider;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ImmutableValueMap;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
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
    private static final Set<String> MOCK_IGNORED_NAMES = SHARED_IGNORED_NAMES;

    // set of resource or property names that are ignored when JCR_OAK resource resolver type (= a real repo impl) is
    // used
    private static final Set<String> OAK_IGNORED_NAMES = Stream.concat(
                    SHARED_IGNORED_NAMES.stream(), Stream.of(JcrConstants.JCR_UUID, JcrConstants.JCR_CREATED))
            .collect(Collectors.toSet());

    private final ResourceResolver resourceResolver;
    private final BundleContext bundleContext;
    private final boolean autoCommit;
    private final Set<String> ignoredNames;

    @Nullable
    private ContentParser jsonParser;

    private final ParserOptions jsonParserOptions;

    @Nullable
    private ContentParser fileVaultXmlParser;

    private final ParserOptions fileVaultXmlParserOptions;

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
    public ContentLoader(
            @NotNull ResourceResolver resourceResolver, @Nullable BundleContext bundleContext, boolean autoCommit) {
        this(resourceResolver, bundleContext, autoCommit, null);
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     * @param autoCommit Automatically commit changes after loading content (default: true)
     * @param resourceResolverType Resource resolver type.
     */
    public ContentLoader(
            @NotNull ResourceResolver resourceResolver,
            @Nullable BundleContext bundleContext,
            boolean autoCommit,
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
        this.fileVaultXmlParserOptions = new ParserOptions()
                .detectCalendarValues(true)
                .ignorePropertyNames(this.ignoredNames)
                .ignoreResourceNames(this.ignoredNames);
    }

    private final Set<String> getIgnoredNamesForResourceResolverType(ResourceResolverType resourceResolverType) {
        if (resourceResolverType == null || resourceResolverType == ResourceResolverType.JCR_OAK) {
            return OAK_IGNORED_NAMES;
        } else {
            return MOCK_IGNORED_NAMES;
        }
    }

    /**
     * Import content of JSON file into repository.
     * <ul>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for JSON content
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with JSON content
     * @return Resource
     */
    public @NotNull Resource json(
            @NotNull String classpathResourceOrFile, @NotNull Resource parentResource, @NotNull String childName) {
        return json(classpathResourceOrFile, parentResource.getPath() + "/" + childName);
    }

    /**
     * Import content of JSON file into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for JSON content
     * @param destPath Path to import the JSON content to
     * @return Resource
     */
    public @NotNull Resource json(@NotNull String classpathResourceOrFile, @NotNull String destPath) {
        return processInputStreamFromClasspathOrFilesystem(classpathResourceOrFile, is -> json(is, destPath));
    }

    /**
     * Import content of JSON file into repository.
     * <ul>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream with JSON content
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with JSON content
     * @return Resource
     */
    public @NotNull Resource json(
            @NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String childName) {
        return json(inputStream, parentResource.getPath() + "/" + childName);
    }

    /**
     * Import content of JSON file into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream with JSON content
     * @param destPath Path to import the JSON content to
     * @return Resource
     */
    public @NotNull Resource json(@NotNull InputStream inputStream, @NotNull String destPath) {
        return mountParsedFile(inputStream, destPath, getJsonParser(), jsonParserOptions);
    }

    /**
     * Import content of FileVault XML file into repository.
     * <ul>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path to single FileVault XML file (usually <code>.content.xml</code>)
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with Filevault content
     * @return Resource
     */
    public @NotNull Resource fileVaultXml(
            @NotNull String classpathResourceOrFile, @NotNull Resource parentResource, @NotNull String childName) {
        return fileVaultXml(classpathResourceOrFile, parentResource.getPath() + "/" + childName);
    }

    /**
     * Import content of FileVault XML file into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path to single FileVault XML file (usually <code>.content.xml</code>)
     * @param destPath Path to import the Filevault content to
     * @return Resource
     */
    public @NotNull Resource fileVaultXml(@NotNull String classpathResourceOrFile, @NotNull String destPath) {
        return processInputStreamFromClasspathOrFilesystem(classpathResourceOrFile, is -> fileVaultXml(is, destPath));
    }

    /**
     * Import content of FileVault XML file into repository.
     * <ul>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream with Filevault content
     * @param parentResource Parent resource
     * @param childName Name of child resource to create with Filevault content
     * @return Resource
     */
    public @NotNull Resource fileVaultXml(
            @NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String childName) {
        return fileVaultXml(inputStream, parentResource.getPath() + "/" + childName);
    }

    /**
     * Import content of FileVault XML file into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream with Filevault content
     * @param destPath Path to import the Filevault content to
     * @return Resource
     */
    public @NotNull Resource fileVaultXml(@NotNull InputStream inputStream, @NotNull String destPath) {
        return mountParsedFile(inputStream, destPath, getFileVaultXmlParser(), fileVaultXmlParserOptions);
    }

    @NotNull
    private ContentParser getJsonParser() {
        if (jsonParser == null) {
            jsonParser = new JSONContentParser();
        }
        return jsonParser;
    }

    @NotNull
    private ContentParser getFileVaultXmlParser() {
        if (fileVaultXmlParser == null) {
            fileVaultXmlParser = new JCRXMLContentParser();
        }
        return fileVaultXmlParser;
    }

    @SuppressWarnings("null")
    private @NotNull Resource mountParsedFile(
            @NotNull InputStream inputStream,
            @NotNull String destPath,
            @NotNull ContentParser contentParser,
            @NotNull ParserOptions parserOptions) {
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
            contentParser.parse(contentHandler, inputStream, parserOptions);
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
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from either {@code classpathResourceOrFile} or {@code path}.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull String classpathResourceOrFile, @NotNull String path) {
        return binaryFile(classpathResourceOrFile, path, detectMimeTypeFromNames(classpathResourceOrFile, path));
    }

    /**
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for binary file.
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(
            @NotNull String classpathResourceOrFile, @NotNull String path, @NotNull String mimeType) {
        return processInputStreamFromClasspathOrFilesystem(
                classpathResourceOrFile, is -> binaryFile(is, path, mimeType));
    }

    /**
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from resource name.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(@NotNull InputStream inputStream, @NotNull String path) {
        return binaryFile(inputStream, path, detectMimeTypeFromNames(path));
    }

    /**
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(
            @NotNull InputStream inputStream, @NotNull String path, @NotNull String mimeType) {
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
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from resource name.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:file
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(
            @NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name) {
        return binaryFile(inputStream, parentResource, name, detectMimeTypeFromNames(name));
    }

    /**
     * Import binary file as nt:file binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:file
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryFile(
            @NotNull InputStream inputStream,
            @NotNull Resource parentResource,
            @NotNull String name,
            @NotNull String mimeType) {
        try {
            Resource file = resourceResolver.create(
                    parentResource, name, ImmutableValueMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE));
            resourceResolver.create(
                    file,
                    JcrConstants.JCR_CONTENT,
                    ImmutableValueMap.of(
                            JcrConstants.JCR_PRIMARYTYPE,
                            JcrConstants.NT_RESOURCE,
                            JcrConstants.JCR_DATA,
                            inputStream,
                            JcrConstants.JCR_MIMETYPE,
                            mimeType));
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
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from {@code classpathResourceOrFile} or {@code path}.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for binary file.
     * @param path Path to mount binary data to (parent nodes created automatically)
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(@NotNull String classpathResourceOrFile, @NotNull String path) {
        return binaryResource(classpathResourceOrFile, path, detectMimeTypeFromNames(classpathResourceOrFile, path));
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param classpathResourceOrFile Classpath resource URL or file path for binary file.
     * @param path Path to mount binary data to (parent nodes created automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(
            @NotNull String classpathResourceOrFile, @NotNull String path, @NotNull String mimeType) {
        return processInputStreamFromClasspathOrFilesystem(
                classpathResourceOrFile, is -> binaryResource(is, path, mimeType));
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from resource name.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
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
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param path Path to mount binary data to (parent nodes created
     *            automatically)
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(
            @NotNull InputStream inputStream, @NotNull String path, @NotNull String mimeType) {
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
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>Mime type is auto-detected from resource name.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:resource
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(
            @NotNull InputStream inputStream, @NotNull Resource parentResource, @NotNull String name) {
        return binaryResource(inputStream, parentResource, name, detectMimeTypeFromNames(name));
    }

    /**
     * Import binary file as nt:resource binary node into repository.
     * <ul>
     * <li>Auto-creates parent hierarchies as nt:unstrucured nodes if missing.</li>
     * <li>The imported resources support reading and writing.</li>
     * </ul>
     * @param inputStream Input stream for binary data
     * @param parentResource Parent resource
     * @param name Resource name for nt:resource
     * @param mimeType Mime type of binary data
     * @return Resource with binary data
     */
    public @NotNull Resource binaryResource(
            @NotNull InputStream inputStream,
            @NotNull Resource parentResource,
            @NotNull String name,
            @NotNull String mimeType) {
        try {
            Resource resource = resourceResolver.create(
                    parentResource,
                    name,
                    ImmutableValueMap.of(
                            JcrConstants.JCR_PRIMARYTYPE,
                            JcrConstants.NT_RESOURCE,
                            JcrConstants.JCR_DATA,
                            inputStream,
                            JcrConstants.JCR_MIMETYPE,
                            mimeType));
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
    @SuppressWarnings("null")
    private @NotNull String detectMimeTypeFromNames(@NotNull String @NotNull ... names) {
        String mimeType = null;
        for (String name : names) {
            String fileExtension = StringUtils.substringAfterLast(name, ".");
            if (bundleContext != null && StringUtils.isNotEmpty(fileExtension)) {
                ServiceReference<MimeTypeService> ref = bundleContext.getServiceReference(MimeTypeService.class);
                if (ref != null) {
                    MimeTypeService mimeTypeService = bundleContext.getService(ref);
                    mimeType = mimeTypeService.getMimeType(fileExtension);
                    break;
                }
            }
        }
        return Objects.toString(mimeType, CONTENTTYPE_OCTET_STREAM);
    }

    /**
     * Mount a folder (file system) containing content in JSON (Sling-Initial-Content) format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolderPath Root folder path to mount
     * @param parentResource Parent resource
     * @param childName Name of child resource to mount folder into
     */
    public void folderJson(
            @NotNull String mountFolderPath, @NotNull Resource parentResource, @NotNull String childName) {
        folderJson(new File(mountFolderPath), parentResource, childName);
    }

    /**
     * Mount a folder (file system) containing content in JSON (Sling-Initial-Content) format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolderPath Root folder path to mount
     * @param destPath Path to mount folder into
     */
    public void folderJson(@NotNull String mountFolderPath, @NotNull String destPath) {
        folderJson(new File(mountFolderPath), destPath);
    }

    /**
     * Mount a folder containing content in JSON (Sling-Initial-Content) format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolder Root folder to mount
     * @param parentResource Parent resource
     * @param childName Name of child resource to mount folder into
     */
    public void folderJson(@NotNull File mountFolder, @NotNull Resource parentResource, @NotNull String childName) {
        folderJson(mountFolder, parentResource.getPath() + "/" + childName);
    }

    /**
     * Mount a folder containing content in JSON (Sling-Initial-Content) format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolder Root folder to mount
     * @param destPath Path to mount folder into
     */
    public void folderJson(@NotNull File mountFolder, @NotNull String destPath) {
        registerFileSystemResourceProvider(
                "provider.file",
                mountFolder.getAbsolutePath(),
                "provider.root",
                destPath,
                "provider.fs.mode",
                "INITIAL_CONTENT",
                "provider.initial.content.import.options",
                "overwrite:=true;ignoreImportProviders:=\"xml\"",
                "provider.checkinterval",
                0);
    }

    /**
     * Mount a folder (file system) containing content in FileVault XML format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolderPath Root folder path to mount. Path needs to point to the root folder of the content package structure.
     * @param parentResource Parent resource
     * @param childName Name of child resource of subtree path that should be mounted from FileVault XML structure
     */
    public void folderFileVaultXml(
            @NotNull String mountFolderPath, @NotNull Resource parentResource, @NotNull String childName) {
        folderFileVaultXml(new File(mountFolderPath), parentResource, childName);
    }

    /**
     * Mount a folder (file system) containing content in FileVault XML format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolderPath Root folder path to mount. Path needs to point to the root folder of the content package structure.
     * @param destPath Subtree path that should be mounted from FileVault XML structure
     */
    public void folderFileVaultXml(@NotNull String mountFolderPath, @NotNull String destPath) {
        folderFileVaultXml(new File(mountFolderPath), destPath);
    }

    /**
     * Mount a folder containing content in FileVault XML format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolder Root folder to mount. Path needs to point to the root folder of the content package structure.
     * @param parentResource Parent resource
     * @param childName Name of child resource of subtree path that should be mounted from FileVault XML structure
     */
    public void folderFileVaultXml(
            @NotNull File mountFolder, @NotNull Resource parentResource, @NotNull String childName) {
        folderFileVaultXml(mountFolder, parentResource.getPath() + "/" + childName);
    }

    /**
     * Mount a folder containing content in FileVault XML format in repository.
     * <ul>
     * <li>The resources are not imported, but mounted via FS Resource Provider.</li>
     * <li>The mounted resource tree is read-only.</li>
     * </ul>
     * @param mountFolder Root folder to mount. Path needs to point to the root folder of the content package structure.
     * @param destPath Subtree path that should be mounted from FileVault XML structure
     */
    public void folderFileVaultXml(@NotNull File mountFolder, @NotNull String destPath) {
        registerFileSystemResourceProvider(
                "provider.file",
                mountFolder.getAbsolutePath(),
                "provider.root",
                destPath,
                "provider.fs.mode",
                "FILEVAULT_XML",
                "provider.checkinterval",
                0);
    }

    /**
     * Get input stream for a resource either from classpath (preferred) or from filesystem (fallback).
     * @param classpathResourceOrFile Classpath resource URL or file path
     * @param processor Processes input stream
     */
    @SuppressWarnings("null")
    private <T> @NotNull T processInputStreamFromClasspathOrFilesystem(
            @NotNull String classpathResourceOrFile, @NotNull Function<InputStream, T> processor) {
        InputStream is = ContentLoader.class.getResourceAsStream(classpathResourceOrFile);
        if (is == null) {
            try {
                is = new FileInputStream(classpathResourceOrFile);
            } catch (FileNotFoundException ex) {
                throw new IllegalArgumentException("Classpath resource or file not found: " + classpathResourceOrFile);
            }
        }
        try {
            return processor.apply(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @SuppressWarnings("null")
    private void registerFileSystemResourceProvider(Object... serviceProperties) {
        if (bundleContext == null) {
            throw new IllegalStateException("No bundle context given for content loader.");
        }
        if (isUsingMockResourceResolverFactory()) {
            throw new IllegalStateException(
                    "Loading folder content is not supported with RESOURCERESOLVER_MOCK resource resolver type. "
                            + "Use RESOURCEPROVIDER_MOCK or one of the other types.");
        }
        Dictionary<String, Object> props = MapUtil.toDictionary(serviceProperties);
        FsResourceProvider service = MockOsgi.activateInjectServices(FsResourceProvider.class, bundleContext, props);
        bundleContext.registerService(ResourceProvider.class, service, props);
    }

    private boolean isUsingMockResourceResolverFactory() {
        ServiceReference<ResourceResolverFactory> serviceReference =
                bundleContext.getServiceReference(ResourceResolverFactory.class);
        if (serviceReference == null) {
            throw new IllegalStateException("No resource resolver factory service present.");
        }
        try {
            ResourceResolverFactory resourceResolverFactory = bundleContext.getService(serviceReference);
            return resourceResolverFactory instanceof MockResourceResolverFactory;
        } finally {
            bundleContext.ungetService(serviceReference);
        }
    }
}

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

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.sling.testing.mock.osgi.ManifestScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class that fetches all node type definitions from OSGi bundle MANIFEST.MF files
 * with "Sling-Nodetypes" definitions in the classpath.
 * Additionally it support registering them to a JCR repository.
 */
public final class NodeTypeDefinitionScanner {

    private static final NodeTypeDefinitionScanner SINGLETON = new NodeTypeDefinitionScanner();

    private static final Logger log = LoggerFactory.getLogger(NodeTypeDefinitionScanner.class);

    private final List<String> nodeTypeDefinitions;

    private NodeTypeDefinitionScanner() {
        nodeTypeDefinitions = findeNodeTypeDefinitions();
    }

    /**
     * @return Node type definitions found in classpath as registered in OSGi bundle headers
     */
    public List<String> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    /**
     * Registers node types found in classpath in JCR repository.
     * @param session Session
     * @param nodeTypeMode Node type mode
     * @throws RepositoryException Repository exception
     */
    public void register(Session session, NodeTypeMode nodeTypeMode) throws RepositoryException {
        List<String> nodeTypeResources = getNodeTypeDefinitions();
        register(session, nodeTypeResources, nodeTypeMode);
    }

    /**
     * Registers node types found in classpath in JCR repository.
     * @param session Session
     * @param nodeTypeResources List of classpath resource URLs pointing to node type definitions
     * @param nodeTypeMode Node type mode
     * @throws RepositoryException Repository exception
     */
    public void register(Session session, List<String> nodeTypeResources, NodeTypeMode nodeTypeMode)
            throws RepositoryException {
        switch (nodeTypeMode) {
            case NOT_SUPPORTED:
                // do nothing
                break;
            case NAMESPACES_ONLY:
                registerNamespaces(session, nodeTypeResources);
                break;
            case NODETYPES_REQUIRED:
                registerNodeTypes(session, nodeTypeResources);
                break;
            default:
                throw new IllegalArgumentException("Node type mode not supported: " + nodeTypeMode);
        }
    }

    /**
     * Registers only the namespaces found in node type definitions in classpath in JCR repository.
     * @param session Session
     * @param nodeTypeResources List of classpath resource URLs pointing to node type definitions
     */
    private void registerNamespaces(Session session, List<String> nodeTypeResources) throws RepositoryException {
        ClassLoader classLoader = getClass().getClassLoader();
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        ValueFactory valueFactory = session.getValueFactory();

        DefinitionBuilderFactory<NodeTypeTemplate, NamespaceRegistry> factory =
                new TemplateBuilderFactory(new DummyNodeTypeManager(), valueFactory, namespaceRegistry);

        for (String nodeTypeResource : nodeTypeResources) {
            try (InputStream is = classLoader.getResourceAsStream(nodeTypeResource)) {
                if (is == null) {
                    continue;
                }
                Reader reader = new InputStreamReader(is);
                CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> cndReader = new CompactNodeTypeDefReader<
                        NodeTypeTemplate, NamespaceRegistry>(reader, nodeTypeResource, factory);
                NamespaceRegistry mapping = cndReader.getNamespaceMapping();
                for (int i = 0; i < mapping.getURIs().length; i++) {
                    String uri = mapping.getURIs()[i];
                    String prefix = mapping.getPrefix(uri);
                    try {
                        namespaceRegistry.registerNamespace(prefix, uri);
                    } catch (RepositoryException ex) {
                        // ignore
                    }
                }
            } catch (Throwable ex) {
                log.warn("Unable to parse node type definition: " + nodeTypeResource, ex);
            }
        }
    }

    /**
     * Registers node types found in classpath in JCR repository.
     * @param session Session
     * @param nodeTypeResources List of classpath resource URLs pointing to node type definitions
     */
    private void registerNodeTypes(Session session, List<String> nodeTypeResources) throws RepositoryException {
        ClassLoader classLoader = getClass().getClassLoader();
        Workspace workspace = session.getWorkspace();
        NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        ValueFactory valueFactory = session.getValueFactory();
        DefinitionBuilderFactory<NodeTypeTemplate, NamespaceRegistry> factory =
                new TemplateBuilderFactory(nodeTypeManager, valueFactory, namespaceRegistry);

        Map<String, NodeTypeTemplate> nodeTypes = new HashMap<>();
        for (String resource : nodeTypeResources) {
            nodeTypes.putAll(parseNodeTypesFromResource(resource, classLoader, factory));
        }

        nodeTypeManager.registerNodeTypes(nodeTypes.values().toArray(new NodeTypeTemplate[0]), true);
    }

    /**
     * Parses a CND file present on the classpath and returns the node types found within.
     * @param resource The resource name.
     * @param classLoader The classloader to load resources with.
     * @param factory The factory to build node type definitions with.
     * @return A mapping from node type names to node type definitions.
     */
    private Map<String, NodeTypeTemplate> parseNodeTypesFromResource(
            String resource,
            ClassLoader classLoader,
            DefinitionBuilderFactory<NodeTypeTemplate, NamespaceRegistry> factory) {
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is == null) {
                return Map.of();
            }
            CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> cndReader =
                    new CompactNodeTypeDefReader<>(new InputStreamReader(is), resource, factory);
            Map<String, NodeTypeTemplate> result = new HashMap<>();
            for (NodeTypeTemplate template : cndReader.getNodeTypeDefinitions()) {
                result.put(template.getName(), template);
            }
            return result;
        } catch (Throwable ex) {
            log.warn("Failed to parse CND resource: " + resource, ex);
            return Map.of();
        }
    }

    /**
     * Find all node type definition classpath paths by searching all MANIFEST.MF files in the classpath and reading
     * the paths from the "Sling-Nodetypes" entry.
     * The order of the paths from each entry is preserved, but the overall order when multiple bundles define such an entry
     * is not deterministic and may not be correct according to the dependencies between the node type definitions.
     * @return List of node type definition class paths
     */
    private static List<String> findeNodeTypeDefinitions() {
        return new ArrayList<String>(ManifestScanner.getValues("Sling-Nodetypes"));
    }

    /**
     * @return Instance
     */
    public static NodeTypeDefinitionScanner get() {
        return SINGLETON;
    }

    /**
     * Some dummy classes to allow usage of CompactNodeTypeDefReader with underlying JCR mock
     */
    private static class DummyNodeTypeManager implements NodeTypeManager {
        @Override
        public NodeType getNodeType(String nodeTypeName) {
            return null;
        }

        @Override
        public boolean hasNodeType(String name) {
            return false;
        }

        @Override
        public NodeTypeIterator getAllNodeTypes() {
            return null;
        }

        @Override
        public NodeTypeIterator getPrimaryNodeTypes() {
            return null;
        }

        @Override
        public NodeTypeIterator getMixinNodeTypes() {
            return null;
        }

        @Override
        public NodeTypeTemplate createNodeTypeTemplate() {
            return new DummyNodeTypeTemplate();
        }

        @Override
        public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd) {
            return new DummyNodeTypeTemplate();
        }

        @Override
        public NodeDefinitionTemplate createNodeDefinitionTemplate() {
            return new DummyNodeDefinitionTemplate();
        }

        @Override
        public PropertyDefinitionTemplate createPropertyDefinitionTemplate() {
            return new DummyPropertyDefinitionTemplate();
        }

        @Override
        public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) {
            return null;
        }

        @Override
        public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate) {
            return null;
        }

        @Override
        public void unregisterNodeType(String name) {}

        @Override
        public void unregisterNodeTypes(String[] names) {}
    }

    private static class DummyNodeTypeTemplate implements NodeTypeTemplate {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public String[] getDeclaredSupertypeNames() {
            return null;
        }

        @Override
        public boolean isAbstract() {
            return false;
        }

        @Override
        public boolean isMixin() {
            return false;
        }

        @Override
        public boolean hasOrderableChildNodes() {
            return false;
        }

        @Override
        public boolean isQueryable() {
            return false;
        }

        @Override
        public String getPrimaryItemName() {
            return null;
        }

        @Override
        public PropertyDefinition[] getDeclaredPropertyDefinitions() {
            return null;
        }

        @Override
        public NodeDefinition[] getDeclaredChildNodeDefinitions() {
            return null;
        }

        @Override
        public void setName(String name) {}

        @Override
        public void setDeclaredSuperTypeNames(String[] names) {}

        @Override
        public void setAbstract(boolean abstractStatus) {}

        @Override
        public void setMixin(boolean mixin) {}

        @Override
        public void setOrderableChildNodes(boolean orderable) {}

        @Override
        public void setPrimaryItemName(String name) {}

        @Override
        public void setQueryable(boolean queryable) {}

        @Override
        public List getPropertyDefinitionTemplates() {
            return new ArrayList();
        }

        @Override
        public List getNodeDefinitionTemplates() {
            return new ArrayList();
        }
    }

    private static class DummyNodeDefinitionTemplate implements NodeDefinitionTemplate {
        @Override
        public NodeType[] getRequiredPrimaryTypes() {
            return null;
        }

        @Override
        public String[] getRequiredPrimaryTypeNames() {
            return null;
        }

        @Override
        public NodeType getDefaultPrimaryType() {
            return null;
        }

        @Override
        public String getDefaultPrimaryTypeName() {
            return null;
        }

        @Override
        public boolean allowsSameNameSiblings() {
            return false;
        }

        @Override
        public NodeType getDeclaringNodeType() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isAutoCreated() {
            return false;
        }

        @Override
        public boolean isMandatory() {
            return false;
        }

        @Override
        public int getOnParentVersion() {
            return 0;
        }

        @Override
        public boolean isProtected() {
            return false;
        }

        @Override
        public void setName(String name) {}

        @Override
        public void setAutoCreated(boolean autoCreated) {}

        @Override
        public void setMandatory(boolean mandatory) {}

        @Override
        public void setOnParentVersion(int opv) {}

        @Override
        public void setProtected(boolean protectedStatus) {}

        @Override
        public void setRequiredPrimaryTypeNames(String[] names) {}

        @Override
        public void setDefaultPrimaryTypeName(String name) {}

        @Override
        public void setSameNameSiblings(boolean allowSameNameSiblings) {}
    }

    private static class DummyPropertyDefinitionTemplate implements PropertyDefinitionTemplate {
        @Override
        public int getRequiredType() {
            return 0;
        }

        @Override
        public String[] getValueConstraints() {
            return null;
        }

        @Override
        public Value[] getDefaultValues() {
            return null;
        }

        @Override
        public boolean isMultiple() {
            return false;
        }

        @Override
        public String[] getAvailableQueryOperators() {
            return null;
        }

        @Override
        public boolean isFullTextSearchable() {
            return false;
        }

        @Override
        public boolean isQueryOrderable() {
            return false;
        }

        @Override
        public NodeType getDeclaringNodeType() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isAutoCreated() {
            return false;
        }

        @Override
        public boolean isMandatory() {
            return false;
        }

        @Override
        public int getOnParentVersion() {
            return 0;
        }

        @Override
        public boolean isProtected() {
            return false;
        }

        @Override
        public void setName(String name) {}

        @Override
        public void setAutoCreated(boolean autoCreated) {}

        @Override
        public void setMandatory(boolean mandatory) {}

        @Override
        public void setOnParentVersion(int opv) {}

        @Override
        public void setProtected(boolean protectedStatus) {}

        @Override
        public void setRequiredType(int type) {}

        @Override
        public void setValueConstraints(String[] constraints) {}

        @Override
        public void setDefaultValues(Value[] defaultValues) {}

        @Override
        public void setMultiple(boolean multiple) {}

        @Override
        public void setAvailableQueryOperators(String[] operators) {}

        @Override
        public void setFullTextSearchable(boolean fullTextSearchable) {}

        @Override
        public void setQueryOrderable(boolean queryOrderable) {}
    }
}

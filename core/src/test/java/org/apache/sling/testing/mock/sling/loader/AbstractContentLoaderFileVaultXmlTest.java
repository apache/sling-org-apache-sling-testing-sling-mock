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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import java.util.List;
import java.util.stream.Stream;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.NodeTypeDefinitionScanner;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@SuppressWarnings("null")
public abstract class AbstractContentLoaderFileVaultXmlTest {

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    protected String path;

    @Before
    public void setUp() {
        path = context.uniqueRoot().content();

        try {
            NodeTypeDefinitionScanner.get()
                    .register(
                            context.resourceResolver().adaptTo(Session.class),
                            List.of("SLING-INF/nodetypes/app.cnd"),
                            getResourceResolverType().getNodeTypeMode());
        } catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register namespaces.", ex);
        }

        context.load()
                .fileVaultXml(
                        "src/test/resources/xml-jcr-import-samples/content/samples/en/.content.xml",
                        path + "/sample/en");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(context.resourceResolver().hasChanges());
    }

    @Test
    public void testPageResourceType() {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en");
        assertEquals("app:Page", resource.getResourceType());
    }

    @Test
    public void testPageJcrPrimaryType() throws RepositoryException {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en");
        assertPrimaryNodeType(resource, "app:Page");
    }

    @Test
    public void testPageContentResourceType() {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en/jcr:content");
        assertEquals("samples/sample-app/components/content/page/homepage", resource.getResourceType());
    }

    @Test
    public void testPageContentJcrPrimaryType() throws RepositoryException {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en/jcr:content");
        assertPrimaryNodeType(resource, "app:PageContent");
    }

    @Test
    public void testPageContentMixinTypes() throws RepositoryException {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en/jcr:content");
        assertMixinTypes(resource, "app:TestMixin");
    }

    @Test
    public void testPageContentProperties() {
        Resource resource = context.resourceResolver().getResource(path + "/sample/en/jcr:content");
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals("HOME", props.get("navTitle", String.class));
        assertEquals(true, props.get("includeAside", Boolean.class));
    }

    private void assertPrimaryNodeType(final Resource resource, final String nodeType) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            assertEquals(nodeType, node.getPrimaryNodeType().getName());
        } else {
            ValueMap props = ResourceUtil.getValueMap(resource);
            assertEquals(nodeType, props.get(JcrConstants.JCR_PRIMARYTYPE));
        }
    }

    private void assertMixinTypes(final Resource resource, String... mixinTypes) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            assertArrayEquals(
                    mixinTypes,
                    Stream.of(node.getMixinNodeTypes()).map(NodeType::getName).toArray(String[]::new));
        } else {
            ValueMap props = ResourceUtil.getValueMap(resource);
            assertArrayEquals(mixinTypes, props.get(JcrConstants.JCR_MIXINTYPES, String[].class));
        }
    }
}

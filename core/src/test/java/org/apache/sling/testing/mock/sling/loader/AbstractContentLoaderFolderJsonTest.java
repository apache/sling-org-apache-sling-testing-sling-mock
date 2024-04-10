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
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("null")
public abstract class AbstractContentLoaderFolderJsonTest {

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    @Before
    public void setUp() {
        context.load().folderJson("src/test/resources/json-import-samples", "/mount");
        context.load().folderJson("src/test/resources/json-import-samples/apps", "/apps");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(context.resourceResolver().hasChanges());
    }

    @Test
    public void testContentResourceType() {
        Resource resource = context.resourceResolver().getResource("/mount/content/jcr:content");
        assertEquals("sample/components/homepage", resource.getResourceType());
    }

    @Test
    public void testContentListChildren() {
        Resource resource = context.resourceResolver().getResource("/mount/content");
        List<Resource> result = IteratorUtils.toList(resource.listChildren());
        assertEquals("jcr:content", result.get(0).getName());
        assertEquals("toolbar", result.get(1).getName());
    }

    @Test
    public void testDamResourceType() {
        Resource resource = context.resourceResolver().getResource("/mount/dam/portraits/scott_reynolds.jpg");
        assertEquals("dam:Asset", resource.getResourceType());
    }

    @Test
    public void testBinaryResource() throws IOException {
        Resource fileResource = context.resourceResolver().getResource("/mount/binary/sample-image.gif");
        AbstractContentLoaderBinaryTest.assertSampleImageFileSize(fileResource);
    }

    @Test
    public void testAppsResource() {
        Resource resource = context.resourceResolver().getResource("/apps/app1/components/comp1");
        assertNotNull(resource);
        assertEquals("Component #1", resource.getValueMap().get(JcrConstants.JCR_TITLE, String.class));
    }

    @Test
    public void testAppsResource_SearchPath() {
        Resource resource = context.resourceResolver().getResource("app1/components/comp1");
        assertNotNull(resource);
        assertEquals("Component #1", resource.getValueMap().get(JcrConstants.JCR_TITLE, String.class));
    }

    @Test
    public void testAppsResource_ParentResourceType() {
        Resource resource = context.resourceResolver().getResource("/mount/content/jcr:content/comp1-resource");
        assertNotNull(resource);
        assertEquals("app1/components/base", context.resourceResolver().getParentResourceType(resource));
        assertTrue(context.resourceResolver().isResourceType(resource, "app1/components/comp1"));
        assertTrue(context.resourceResolver().isResourceType(resource, "/apps/app1/components/comp1"));
        assertTrue(context.resourceResolver().isResourceType(resource, "app1/components/base"));
        assertTrue(context.resourceResolver().isResourceType(resource, "core/components/superResource"));
    }

    @Test
    public void testResourceOperationsOnMountedFolder() {
        String root = context.uniqueRoot().content() + "/test-content";
        context.load().folderJson("src/test/resources/test-content", root);

        Resource parent = context.resourceResolver().getResource(root + "/parent");
        assertNotNull("Expected to resolve the 'parent' resource.", parent);
        assertNotNull("Expected to resolver the 'child' resource.", parent.getChild("child"));

        Resource uniqueRoot = context.resourceResolver().getParent(parent);
        assertNotNull("Expected to resolve the unique root.", uniqueRoot);
        assertEquals(
                "The resolved unique root is not identical to the created unique root.", root, uniqueRoot.getPath());

        assertTrue(
                "Expected to see a list of children.",
                context.resourceResolver().listChildren(parent).hasNext());
        assertTrue(
                "Expected to get a list of children.",
                context.resourceResolver().getChildren(parent).iterator().hasNext());
        assertTrue("Expected to see a list of children.", parent.hasChildren());
    }
}

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
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
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
public abstract class AbstractContentLoaderFolderFileVaultXmlTest {

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    @Before
    public void setUp() {
        context.load().folderFileVaultXml("src/test/resources/xml-jcr-import-samples", "/apps");
        context.load().folderFileVaultXml("src/test/resources/xml-jcr-import-samples", "/content");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(context.resourceResolver().hasChanges());
    }

    @Test
    public void testContentResourceType() {
        Resource resource = context.resourceResolver().getResource("/content/samples/en/jcr:content");
        assertEquals("samples/sample-app/components/content/page/homepage", resource.getResourceType());
    }

    @Test
    public void testContentListChildren() {
        Resource resource = context.resourceResolver().getResource("/content/samples/en");
        List<Resource> result = IteratorUtils.toList(resource.listChildren());
        assertEquals("jcr:content", result.get(0).getName());
        assertEquals("tools", result.get(1).getName());
    }

    @Test
    public void testDamResourceType() {
        Resource resource = context.resourceResolver().getResource("/content/dam/talk.png/jcr:content");
        assertEquals("app:AssetContent", resource.getResourceType());
    }

    @Test
    public void testBinaryResource() throws IOException {
        Resource fileResource =
                context.resourceResolver().getResource("/content/dam/talk.png/jcr:content/renditions/original");
        try (InputStream is = fileResource.adaptTo(InputStream.class)) {
            assertNotNull("InputSteam is null for " + fileResource.getPath(), is);
            byte[] binaryData = IOUtils.toByteArray(is);
            assertEquals(8668, binaryData.length);
        }
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
        Resource resource = context.resourceResolver().getResource("/content/samples/en/jcr:content/comp1-resource");
        assertNotNull(resource);
        assertEquals("app1/components/base", context.resourceResolver().getParentResourceType(resource));
        assertTrue(context.resourceResolver().isResourceType(resource, "app1/components/comp1"));
        assertTrue(context.resourceResolver().isResourceType(resource, "/apps/app1/components/comp1"));
        assertTrue(context.resourceResolver().isResourceType(resource, "app1/components/base"));
        assertTrue(context.resourceResolver().isResourceType(resource, "core/components/superResource"));
    }
}

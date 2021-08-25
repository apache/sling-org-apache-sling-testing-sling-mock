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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("null")
public abstract class AbstractContentLoaderFolderJsonTest {

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    protected String path;

    @Before
    public void setUp() {
        path = context.uniqueRoot().content();
        context.load().folderJson("src/test/resources/json-import-samples", path + "/mount");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(context.resourceResolver().hasChanges());
    }

    @Test
    public void testContentResourceType() {
        Resource resource = context.resourceResolver().getResource(path + "/mount/content/jcr:content");
        assertEquals("sample/components/homepage", resource.getResourceType());
    }

    @Test
    public void testContentListChildren() {
        Resource resource = context.resourceResolver().getResource(path + "/mount/content");
        List<Resource> result = ImmutableList.copyOf(resource.listChildren());
        assertEquals("jcr:content", result.get(0).getName());
        assertEquals("toolbar", result.get(1).getName());
    }

    @Test
    public void testDamResourceType() {
        Resource resource = context.resourceResolver().getResource(path + "/mount/dam/portraits/scott_reynolds.jpg");
        assertEquals("dam:Asset", resource.getResourceType());
    }

    @Test
    public void testBinaryResource() throws IOException {
        Resource fileResource = context.resourceResolver().getResource(path + "/mount/binary/sample-image.gif");
        AbstractContentLoaderBinaryTest.assertSampleImageFileSize(fileResource);
    }

}

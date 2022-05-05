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
package org.apache.sling.testing.mock.sling.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

public class SlingContextDefaultRRTypeTest {

    @Rule
    public SlingContext context = new SlingContext();

    @Test
    public void testRequest() {
        assertNotNull(context.request());
    }

    @Test
    public void testResourceOperationsOnMountedFolder() {
        String root = context.uniqueRoot().content() + "/test-content";
        context.load().folderJson("src/test/resources/test-content", root);

        Resource parent = context.resourceResolver().getResource(root + "/parent");
        assertNotNull( "Expected to resolve the 'parent' resource.", parent);
        assertNotNull("Expected to resolver the 'child' resource.", parent.getChild("child"));

        Resource uniqueRoot = context.resourceResolver().getParent(parent);
        assertNotNull("Expected to resolve the unique root.", uniqueRoot);
        assertEquals("The resolved unique root is not identical to the created unique root.", root, uniqueRoot.getPath());

        assertTrue("Expected to see a list of children.", context.resourceResolver().listChildren(parent).hasNext());
        assertTrue("Expected to get a list of children.", context.resourceResolver().getChildren(parent).iterator().hasNext());
        assertTrue("Expected to see a list of children.", parent.hasChildren());
    }

}

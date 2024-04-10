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

import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeIterator;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NodeTypeDefinitionScannerTest {

    @Test
    public void testGetNodeTypeDefinitions() throws Exception {
        List<String> definitions = NodeTypeDefinitionScanner.get().getNodeTypeDefinitions();

        // ensure some node types from jcr.resource exist
        assertTrue(definitions.contains("SLING-INF/nodetypes/folder.cnd"));
        assertTrue(definitions.contains("SLING-INF/nodetypes/resource.cnd"));
    }

    @Test
    public void testRegistersNodeTypes() throws Exception {
        Session session = MockJcr.newSession();
        MockJcr.loadNodeTypeDefs(
                session, new StringReader("[nt:hierarchyNode] > nt:base\n" + "[nt:folder] > nt:hierarchyNode"));

        NodeTypeDefinitionScanner.get().register(session, NodeTypeMode.NODETYPES_REQUIRED);

        Set<String> nodeTypes = new HashSet<>();
        NodeTypeIterator nodeTypeIterator =
                session.getWorkspace().getNodeTypeManager().getAllNodeTypes();
        while (nodeTypeIterator.hasNext()) {
            nodeTypes.add(nodeTypeIterator.nextNodeType().getName());
        }
        assertTrue(nodeTypes.containsAll(Set.of(
                "sling:Folder",
                "sling:HierarchyNode",
                "sling:OrderedFolder",
                "sling:Resource",
                "sling:ResourceSuperType")));
    }
}

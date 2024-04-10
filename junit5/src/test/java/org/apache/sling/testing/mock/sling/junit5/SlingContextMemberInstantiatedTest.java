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
package org.apache.sling.testing.mock.sling.junit5;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test with {@link SlingContext} as class member variable already instantiated.
 */
@ExtendWith(SlingContextExtension.class)
@SuppressWarnings("null")
class SlingContextMemberInstantiatedTest {

    SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @BeforeEach
    void setUp() {
        assertTrue(context.resourceResolver() instanceof ResourceResolverImpl);

        context.create().resource("/content/test", "prop1", "value1");
    }

    @Test
    void testResource() {
        Resource resource = context.resourceResolver().getResource("/content/test");
        assertEquals("value1", resource.getValueMap().get("prop1"));
    }
}

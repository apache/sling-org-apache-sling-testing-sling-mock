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
package org.apache.sling.testing.mock.sling.servlet;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.junit.Test.None;
import org.mockito.Mockito;

/**
 *
 */
public class MockRequestPathInfoTest {

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo#MockRequestPathInfo()}.
     * @deprecated use {@link #testMockRequestPathInfoResourceResolver()} instead
     */
    @Deprecated
    @Test(expected = None.class)
    public void testMockRequestPathInfo() {
        new MockRequestPathInfo();
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo#MockRequestPathInfo(org.apache.sling.api.resource.ResourceResolver)}.
     */
    @Test(expected = None.class)
    public void testMockRequestPathInfoResourceResolver() {
        ResourceResolver mockResourceResolver = Mockito.mock(ResourceResolver.class);
        new MockRequestPathInfo(mockResourceResolver);
    }
}

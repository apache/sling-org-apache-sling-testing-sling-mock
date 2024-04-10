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
package org.apache.sling.testing.mock.sling.rrmock.resource;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.resource.AbstractSlingCrudResourceResolverTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SlingCrudResourceResolverTest extends AbstractSlingCrudResourceResolverTest {

    @Override
    protected ResourceResolverType getResourceResolverType() {
        return ResourceResolverType.RESOURCERESOLVER_MOCK;
    }

    @Test
    @SuppressWarnings({"null", "unchecked"})
    public void testResourceResolverFactory_GetSearchPath() throws Exception {
        // ensure there is a method getSearchPaths in resource resolver factory, although it is not part of the API we
        // are compiling against (keeping backward compatibility)
        ResourceResolverFactory factory = context.getService(ResourceResolverFactory.class);
        Class clazz = factory.getClass();
        Method getSearchPathMethod = clazz.getMethod("getSearchPath");
        getSearchPathMethod.setAccessible(true);
        List<String> searchPaths = (List) getSearchPathMethod.invoke(factory);
        assertNotNull(searchPaths);
    }
}

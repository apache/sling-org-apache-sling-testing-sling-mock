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
package org.apache.sling.testing.mock.sling.spi;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 *
 */
public class ResourceResolverTypeAdapterTest {
    private ResourceResolverTypeAdapter mockAdapter = new ResourceResolverTypeAdapter() {
        @Override
        public @Nullable SlingRepository newSlingRepository() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable ResourceResolverFactory newResourceResolverFactory() {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter#snapshot(org.apache.sling.jcr.api.SlingRepository)}.
     */
    @Test
    public void testSnapshot() {
        SlingRepository mockSlingRepository = Mockito.mock(SlingRepository.class);
        assertNull(mockAdapter.snapshot(mockSlingRepository));
    }

    /**
     * Test method for {@link org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter#newSlingRepositoryFromSnapshot(java.lang.Object)}.
     */
    @Test
    public void testNewSlingRepositoryFromSnapshot() {
        Object snapshot = new Object();
        assertThrows(UnsupportedOperationException.class, () -> mockAdapter.newSlingRepositoryFromSnapshot(snapshot));
    }
}

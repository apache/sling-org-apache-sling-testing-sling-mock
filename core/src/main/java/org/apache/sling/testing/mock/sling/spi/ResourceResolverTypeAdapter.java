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

/**
 * SPI interface for resource resolver type implementations to provide a mock
 * resource resolver factory.
 * <p>Supported constructors for implementing classes:</p>
 * <ul>
 * <li>Empty constructor</li>
 * <li>Constructor with {@link org.osgi.framework.BundleContext} parameter</li>
 * </ul>
 */
public interface ResourceResolverTypeAdapter {

    /**
     * Gets resource resolver factory instance. Can be null if only a
     * SlingRepository is provided, in this case the method
     * {@link #newSlingRepository()} has to return a value.
     * @return Resource resolver factory instance or null
     */
    @Nullable
    ResourceResolverFactory newResourceResolverFactory();

    /**
     * Get SlingRepository instance. Can be null if a resource resolver factory
     * is provided, in this case the method
     * {@link #newResourceResolverFactory()} has to return a value.
     * @return Sling repository instance or null
     */
    @Nullable
    SlingRepository newSlingRepository();

    /**
     * Make a snapshot of the current state of the given repository, or return
     * null if snapshots are not supported.
     * Returning non-null implies that calling {@link #newSlingRepositoryFromSnapshot(Object)}
     * with the same object will succeed when called on objects of the same implementation type.
     * The returned object must capture the whole state of the repository, and must not reflect
     * any future changes made to the repository after this method returns.
     * @param repository The repository to snapshot.
     * @return A snapshot object.
     */
    @Nullable
    default Object snapshot(SlingRepository repository) {
        return null;
    }

    /**
     * Get a SlingRepository instance based on a snapshot taken from another repository.
     * @param snapshot A snapshot object, returned by an earlier call to {@link #snapshot(SlingRepository)}.
     * @return A Sling repository instance.
     */
    default SlingRepository newSlingRepositoryFromSnapshot(Object snapshot) {
        throw new UnsupportedOperationException("Snapshots not supported");
    }
}

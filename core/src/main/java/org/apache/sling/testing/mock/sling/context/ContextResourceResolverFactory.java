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
package org.apache.sling.testing.mock.sling.context;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create resolve resolver instance and initialize it depending on it's type.
 */
final class ContextResourceResolverFactory {

    private static final Logger log = LoggerFactory.getLogger(ContextResourceResolverFactory.class);

    private ContextResourceResolverFactory() {
        // static methods only
    }

    public static @NotNull ResourceResolverFactory get(@Nullable final ResourceResolverType resourceResolverType,
            @NotNull final BundleContext bundleContext) {
        ResourceResolverType type = resourceResolverType;
        if (type == null) {
            type = MockSling.DEFAULT_RESOURCERESOLVER_TYPE;
        }
        try {
            log.debug("Start initialize resource resolver factory, bundleContext={}", bundleContext);
            ResourceResolverFactory factory = MockSling.newResourceResolverFactory(type, bundleContext);

            switch (type) {
            case JCR_MOCK:
                initializeJcrMock(factory);
                break;
            case JCR_OAK:
                initializeJcrOak(factory);
                break;
            case RESOURCERESOLVER_MOCK:
                initializeResourceResolverMock(factory);
                break;
            case NONE:
                initializeResourceResolverNone(factory);
                break;
            default:
                throw new IllegalArgumentException("Invalid resource resolver type: " + type);
            }

            log.debug("Finished initializing resource resolver factory, bundleContext={}", bundleContext);

            return factory;
        } catch (Throwable ex) {
            log.error("Failed initializing resource resolver factory, bundleContext={}", bundleContext, ex);
            throw new RuntimeException("Unable to initialize " + type + " resource resolver factory: " + ex.getMessage(), ex);
        }
    }

    private static void initializeJcrMock(@NotNull ResourceResolverFactory factory) throws RepositoryException, LoginException {
        // nothing to do
    }

    private static void initializeJcrOak(@NotNull ResourceResolverFactory factory) {
        // register sling node types?
    }

    private static void initializeResourceResolverMock(@NotNull ResourceResolverFactory factory) {
        // nothing to do
    }

    private static void initializeResourceResolverNone(@NotNull ResourceResolverFactory factory) {
        // nothing to do
    }

}

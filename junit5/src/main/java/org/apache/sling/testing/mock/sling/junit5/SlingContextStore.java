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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Helper class managing storage of {@link SlingContext} in extension context store.
 */
final class SlingContextStore {

    private static final Namespace Sling_CONTEXT_NAMESPACE = Namespace.create(SlingContextExtension.class);

    private SlingContextStore() {
        // static methods only
    }

    /**
     * Get {@link SlingContext} from extension context store.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @return SlingContext or null
     */
    @SuppressWarnings("null")
    public static SlingContext getSlingContext(ExtensionContext extensionContext, Object testInstance) {
        return getStore(extensionContext).get(testInstance, SlingContext.class);
    }

    /**
     * Get {@link SlingContext} from extension context store - if it does not
     * exist create a new one and store it.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @return SlingContext (never null)
     */
    public static SlingContext getOrCreateSlingContext(ExtensionContext extensionContext, Object testInstance) {
        SlingContext context = getSlingContext(extensionContext, testInstance);
        if (context == null) {
            context = createSlingContext(extensionContext);
            storeSlingContext(extensionContext, testInstance, context);
        }
        return context;
    }

    /**
     * Removes {@link SlingContext} from extension context store (if it exists).
     * @param extensionContext Extension context
     * @param testInstance Test instance
     */
    public static void removeSlingContext(ExtensionContext extensionContext, Object testInstance) {
        getStore(extensionContext).remove(testInstance);
    }

    /**
     * Store {@link SlingContext} in extension context store.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @param slingContext Sling context
     */
    public static void storeSlingContext(ExtensionContext extensionContext, Object testInstance, SlingContext slingContext) {
        getStore(extensionContext).put(testInstance, slingContext);
    }

    private static Store getStore(ExtensionContext context) {
        return context.getStore(Sling_CONTEXT_NAMESPACE);
    }

    private static SlingContext createSlingContext(ExtensionContext extensionContext) {
        SlingContext slingContext = new SlingContext();
        slingContext.setUpContext();
        return slingContext;
    }

}

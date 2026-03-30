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

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

public class ThreadsafeMockAdapterManagerWrapperTest {
    @Rule
    public final SlingContext context = new SlingContextBuilder(ResourceResolverType.RESOURCEPROVIDER_MOCK)
            .registerSlingModelsFromClassPath(false)
            .build();

    @Before
    public void setUp() {
        context.registerAdapter(Resource.class, AdapterClass.class, new AdapterClass());
    }

    @Test
    public void x() {
        assertNotNull(context.create().resource("/content/test").adaptTo(AdapterClass.class));
        final MyService myService = context.registerService(new MyService());
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        executor.schedule(
                () -> myService.onChange(
                        Objects.requireNonNull(context.resourceResolver().getResource("/content/test"))),
                2,
                TimeUnit.SECONDS);
        await().until(() -> myService.getValue() != null);
    }

    private static class AdapterClass {}

    private static class MyService {
        private AdapterClass value;

        public AdapterClass getValue() {
            System.out.println("Returning value: " + this.value);
            return this.value;
        }

        public void onChange(@NotNull Resource list) {
            System.out.println("ON CHANGE");
            final AdapterClass result = list.adaptTo(AdapterClass.class);
            System.out.println("Result: " + result);
            value = result;
        }
    }
}

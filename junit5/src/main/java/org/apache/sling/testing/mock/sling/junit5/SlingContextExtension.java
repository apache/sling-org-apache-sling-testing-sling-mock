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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * JUnit 5 extension that allows to inject {@link SlingContext} (or subclasses of
 * it) parameters in test methods, and ensures that the context is set up and
 * teared down properly for each test method.
 */
public final class SlingContextExtension implements ParameterResolver, TestInstancePostProcessor, BeforeEachCallback,
        AfterEachCallback, AfterTestExecutionCallback {

    /**
     * Checks if test class has a {@link SlingContext} or derived field. If it has
     * and is not instantiated, create an new {@link SlingContext} and store it in
     * the field. If it is already instantiated reuse this instance and use it
     * for all test methods.
     */
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        Field slingContextField = getFieldFromTestInstance(testInstance, SlingContext.class);
        if (slingContextField != null) {
            SlingContext context = (SlingContext) slingContextField.get(testInstance);
            if (context != null) {
                if (!context.isSetUp()) {
                    context.setUpContext();
                }
                SlingContextStore.storeSlingContext(extensionContext, testInstance, context);
            } else {
                context = SlingContextStore.getOrCreateSlingContext(extensionContext, testInstance);
                slingContextField.set(testInstance, context);
            }
        }
    }

    /**
     * Support parameter injection for test methods of parameter type is derived
     * from {@link SlingContext}.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return SlingContext.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    /**
     * Resolve (or create) {@link SlingContext} instance for test method
     * parameter.
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return SlingContextStore.getOrCreateSlingContext(extensionContext, extensionContext.getRequiredTestInstance());
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        applySlingContext(extensionContext, slingContext -> {
            // call context plugins setup after all @BeforeEach methods were
            // called
            slingContext.getContextPlugins().executeAfterSetUpCallback(slingContext);
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        applySlingContext(extensionContext, slingContext -> {
            // call context plugins setup before any @AfterEach method is called
            slingContext.getContextPlugins().executeBeforeTearDownCallback(slingContext);
        });
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        applySlingContext(extensionContext, slingContext -> {
            // call context plugins setup after all @AfterEach methods were
            // called
            slingContext.getContextPlugins().executeAfterTearDownCallback(slingContext);

            // Tear down {@link SlingContext} after test is complete.
            slingContext.tearDownContext();
            SlingContextStore.removeSlingContext(extensionContext, extensionContext.getRequiredTestInstance());
        });
    }

    private void applySlingContext(ExtensionContext extensionContext, Consumer<SlingContext> consumer) {
        SlingContext slingContext = SlingContextStore.getSlingContext(extensionContext,
                extensionContext.getRequiredTestInstance());
        if (slingContext != null) {
            consumer.accept(slingContext);
        }
    }

    private Field getFieldFromTestInstance(Object testInstance, Class<?> type) {
        return getFieldFromTestInstance(testInstance.getClass(), type);
    }

    private Field getFieldFromTestInstance(Class<?> instanceClass, Class<?> type) {
        if (instanceClass == null) {
            return null;
        }
        Field contextField = Arrays.stream(instanceClass.getDeclaredFields())
                .filter(field -> type.isAssignableFrom(field.getType())).findFirst().orElse(null);
        if (contextField != null) {
            contextField.setAccessible(true);
        } else {
            return getFieldFromTestInstance(instanceClass.getSuperclass(), type);
        }
        return contextField;
    }

}

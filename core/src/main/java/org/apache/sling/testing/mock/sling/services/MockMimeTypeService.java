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
package org.apache.sling.testing.mock.sling.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.mime.internal.MimeTypeServiceImpl;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.log.LogService;

/**
 * Mock {@link MimeTypeService} implementation.
 */
@Component(service = MimeTypeService.class)
public final class MockMimeTypeService extends MimeTypeServiceImpl {

    private boolean initialized;

    /**
     * Do lazy initializing to avoid reading all defined mime types from disk if not required
     */
    private void lazyInitialization() {
        if (!this.initialized) {
            this.initialized = true;
            
            ComponentContext componentContext = MockOsgi.newComponentContext();

            // activate service in simulated OSGi environment (for MimeTypeService impl < 2.0.0)
            try {
                Method bindLogServiceMethod = MimeTypeServiceImpl.class.getDeclaredMethod("bindLogService", LogService.class);
                bindLogServiceMethod.invoke(this, MockOsgi.newLogService(getClass()));
            }
            catch (NoSuchMethodException ex) {
                // ignore
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException("Unableo to set log service.", ex);
            }
            
            // call activate method of MimeTypeServiceImpl
            // via reflection because the method signature changed between org.apache.sling.commons.mime 2.1.8 and 2.1.10 and 2.2.0
            try {
                Method activateMethod;
                try {
                    Class<?> mimeTypeServiceConfigClass = Class.forName(MimeTypeServiceImpl.class.getName() + "$Config");
                    activateMethod = MimeTypeServiceImpl.class.getDeclaredMethod("activate", BundleContext.class, mimeTypeServiceConfigClass);
                    Object configProxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { mimeTypeServiceConfigClass }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return null;
                        }
                    });
                    activateMethod.invoke(this, componentContext.getBundleContext(), configProxy);
                }
                catch (NoSuchMethodException | ClassNotFoundException ex1) {
                    try {
                        activateMethod = MimeTypeServiceImpl.class.getDeclaredMethod("activate", ComponentContext.class);
                        activateMethod.invoke(this, componentContext);
                    }
                    catch (NoSuchMethodException ex2) {
                        try {
                            activateMethod = MimeTypeServiceImpl.class.getDeclaredMethod("activate", BundleContext.class, Map.class);
                            activateMethod.invoke(this, componentContext.getBundleContext(), MapUtil.toMap(componentContext.getProperties()));
                        }
                        catch (NoSuchMethodException ex3) {
                            throw new RuntimeException("Did not found activate method of MimeTypeServiceImpl with any matching signature.");
                        }
                    }
                }
            }
            catch (SecurityException | InvocationTargetException | IllegalAccessException ex) {
                throw new RuntimeException("Unable to activate MimeTypeServiceImpl.", ex);
            }
        }
    }

    @Override
    public String getMimeType(final String name) {
        lazyInitialization();
        return super.getMimeType(name);
    }

    @Override
    public String getExtension(final String mimeType) {
        lazyInitialization();
        return super.getExtension(mimeType);
    }

    @Override
    public void registerMimeType(final String mimeType, final String... extensions) {
        lazyInitialization();
        super.registerMimeType(mimeType, extensions);
    }

    @Override
    public void registerMimeType(final InputStream mimeTabStream) throws IOException {
        lazyInitialization();
        super.registerMimeType(mimeTabStream);
    }

}

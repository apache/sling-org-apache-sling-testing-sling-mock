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

import static org.junit.Assert.assertNull;

import org.apache.sling.testing.mock.sling.context.modelsautoreg.ClasspathRegisteredModel;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("null")
public class NoSlingModelsRegistrationTest {

    @Rule
    public SlingContext context = new SlingContextBuilder()
            .registerSlingModelsFromClassPath(false)
            .build();

    @Test
    public void testSlnigModelClasspathRegistered() {
        context.request().setAttribute("prop1", "myValue");
        ClasspathRegisteredModel model = context.request().adaptTo(ClasspathRegisteredModel.class);
        // expect null because ClasspathRegisteredModel should not be registered automatically from classpath
        assertNull(model);
    }

}

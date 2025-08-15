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
package org.apache.sling.testing.mock.sling.context.models;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

@Model(adaptables = SlingJakartaHttpServletRequest.class)
public interface SlingBindingsModel {

    // -- Sling --
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    ResourceResolver getResolver();

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    Resource getResource();

    /**
     * @deprecated use {@link #getJakartaRequest()} instead
     */
    @Deprecated(since = "4.0.0")
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    org.apache.sling.api.SlingHttpServletRequest getRequest();

    /**
     * @deprecated use {@link #getJakartaResponse()} instead
     */
    @Deprecated(since = "4.0.0")
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    org.apache.sling.api.SlingHttpServletResponse getResponse();

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    SlingJakartaHttpServletRequest getJakartaRequest();

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    SlingJakartaHttpServletResponse getJakartaResponse();

    // -- JCR --
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    Node getCurrentNode();

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    Session getCurrentSession();

    // -- Custom --
    @ScriptVariable(name = "custom-param-1", injectionStrategy = InjectionStrategy.OPTIONAL)
    String getCustomParam1();

    @ScriptVariable(name = "custom-param-2", injectionStrategy = InjectionStrategy.OPTIONAL)
    String getCustomParam2();

    @ScriptVariable(name = "custom-param-3", injectionStrategy = InjectionStrategy.OPTIONAL)
    String getCustomParam3();
}

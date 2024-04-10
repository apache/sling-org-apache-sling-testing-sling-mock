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

import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSFilter;
import org.osgi.service.component.annotations.Component;

/**
 * Mock implementation of {@link XSSFilter} that just accepts anything.
 */
@Component(service = XSSFilter.class)
public final class MockXSSFilter implements XSSFilter {

    @Override
    public boolean check(ProtectionContext context, String src) {
        return true;
    }

    @Override
    public String filter(String src) {
        return src != null ? src : "";
    }

    @Override
    public String filter(ProtectionContext context, String src) {
        return src != null ? src : "";
    }

    @Override
    public boolean isValidHref(String url) {
        return true;
    }
}

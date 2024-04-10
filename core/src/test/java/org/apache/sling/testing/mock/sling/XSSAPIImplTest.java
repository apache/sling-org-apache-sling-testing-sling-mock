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

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class XSSAPIImplTest {

    @Rule
    public SlingContext context = new SlingContext();

    private XSSAPI underTest;

    @Before
    public void setUp() {
        underTest = context.getService(XSSAPI.class);
    }

    @Test
    public void testGetValidInteger() {
        assertEquals((Integer) 123, underTest.getValidInteger("123", -1));
        assertEquals((Integer) (-123), underTest.getValidInteger("-123", -1));
        assertEquals((Integer) (-1), underTest.getValidInteger("invalid", -1));
        assertEquals((Integer) (-1), underTest.getValidInteger("", -1));
        assertEquals((Integer) (-1), underTest.getValidInteger(null, -1));
    }

    @Test
    public void testGetValidLong() throws Exception {
        assertEquals((Long) 123L, underTest.getValidLong("123", -1L));
        assertEquals((Long) (-123L), underTest.getValidLong("-123", -1L));
        assertEquals((Long) (-1L), underTest.getValidLong("invalid", -1L));
        assertEquals((Long) (-1L), underTest.getValidLong("", -1L));
        assertEquals((Long) (-1L), underTest.getValidLong(null, -1L));
    }

    @Test
    public void testGetValidDouble() throws Exception {
        assertEquals((Double) 1.23d, underTest.getValidDouble("1.23", -1d));
        assertEquals((Double) (-1d), underTest.getValidDouble("invalid", -1d));
        assertEquals((Double) (-1.5d), underTest.getValidDouble("", -1.5d));
        assertEquals((Double) (-1d), underTest.getValidDouble(null, -1d));
    }

    @Test
    public void testGetValidDimension() throws Exception {
        assertEquals("123", underTest.getValidDimension("123", "-1"));
        assertEquals("-123", underTest.getValidDimension("-123", "-1"));
        assertEquals("-1", underTest.getValidDimension("invalid", "-1"));
        assertEquals("-1", underTest.getValidDimension("", "-1"));
        assertEquals("-1", underTest.getValidDimension(null, "-1"));
        assertEquals("\"auto\"", underTest.getValidDimension("\"auto\"", "-1"));
        assertEquals("\"auto\"", underTest.getValidDimension("'auto'", "-1"));
    }

    @Test
    public void testGetValidHref() throws Exception {
        assertEquals("val", underTest.getValidHref("val"));
        assertEquals("", underTest.getValidHref(null));
    }

    @Test
    public void testGetValidJSToken() throws Exception {
        assertEquals("val", underTest.getValidJSToken("val", "def"));
        assertEquals("def", underTest.getValidJSToken("", "def"));
        assertEquals("def", underTest.getValidJSToken(null, "def"));
    }

    @Test
    public void testGetValidStyleToken() throws Exception {
        assertEquals("val", underTest.getValidStyleToken("val", "def"));
        assertEquals("def", underTest.getValidStyleToken("", "def"));
        assertEquals("def", underTest.getValidStyleToken(null, "def"));
    }

    @Test
    public void testGetValidCSSColor() throws Exception {
        assertEquals("val", underTest.getValidCSSColor("val", "def"));
        assertEquals("def", underTest.getValidCSSColor("", "def"));
        assertEquals("def", underTest.getValidCSSColor(null, "def"));
    }

    @Test
    public void testGetValidMultiLineComment() throws Exception {
        assertEquals("val", underTest.getValidMultiLineComment("val", "def"));
        assertEquals("", underTest.getValidMultiLineComment("", "def"));
        assertEquals("def", underTest.getValidMultiLineComment(null, "def"));
    }

    @Test
    public void testGetValidJSON() throws Exception {
        assertEquals("{\"valid\":true}", underTest.getValidJSON("{\"valid\":true}", "{}"));
        assertEquals("", underTest.getValidJSON("", "{}"));
        assertEquals("{}", underTest.getValidJSON("{invalid", "{}"));
        assertEquals("{}", underTest.getValidJSON(null, "{}"));
    }

    @Test
    public void testGetValidXML() throws Exception {
        assertEquals("<valid/>", underTest.getValidXML("<valid/>", "<default/>"));
        assertEquals("", underTest.getValidXML("", "<default/>"));
        assertEquals("<default/>", underTest.getValidXML("<invalid", "<default/>"));
        assertEquals("<default/>", underTest.getValidXML(null, "<default/>"));
    }

    @Test
    public void testEncodeForHTML() throws Exception {
        assertEquals("val", underTest.encodeForHTML("val"));
        assertNull(underTest.encodeForHTML(null));
    }

    @Test
    public void testEncodeForHTMLAttr() throws Exception {
        assertEquals("val", underTest.encodeForHTMLAttr("val"));
        assertNull(underTest.encodeForHTMLAttr(null));
    }

    @Test
    public void testEncodeForXML() throws Exception {
        assertEquals("val", underTest.encodeForXML("val"));
        assertNull(underTest.encodeForXML(null));
    }

    @Test
    public void testEncodeForXMLAttr() throws Exception {
        assertEquals("val", underTest.encodeForXMLAttr("val"));
        assertNull(underTest.encodeForXMLAttr(null));
    }

    @Test
    public void testEncodeForJSString() throws Exception {
        assertEquals("val", underTest.encodeForJSString("val"));
        assertNull(underTest.encodeForJSString(null));
    }

    @Test
    public void testEncodeForCSSString() throws Exception {
        assertEquals("val", underTest.encodeForCSSString("val"));
        assertNull(underTest.encodeForCSSString(null));
    }

    @Test
    public void testFilterHTML() throws Exception {
        assertEquals("val", underTest.filterHTML("val"));
        assertEquals("", underTest.filterHTML(null));
    }
}

/**
 * Copyright (C) 2013 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.test

import org.dom4j.{Document ⇒ JDocument, Element ⇒ JElement}
import org.orbeon.oxf.resources.URLFactory
import org.orbeon.oxf.util.{XPath, ScalaUtils}
import org.orbeon.oxf.xml.{TransformerUtils, Dom4j}
import org.orbeon.oxf.xml.TransformerUtils._
import org.orbeon.oxf.xml.dom4j.Dom4jUtils
import org.orbeon.saxon.om.DocumentInfo
import org.scalatest.junit.AssertionsForJUnit

trait XMLSupport extends AssertionsForJUnit {

    def readURLAsImmutableXMLDocument(url: String) =
        ScalaUtils.useAndClose(URLFactory.createURL(url).openStream()) { is ⇒
            TransformerUtils.readTinyTree(XPath.GlobalConfiguration, is, null, false, false)
        }

    def assertXMLDocumentsIgnoreNamespacesInScope(left: DocumentInfo, right: DocumentInfo): Unit =
        assertXMLDocumentsIgnoreNamespacesInScope(tinyTreeToDom4j(left), tinyTreeToDom4j(right))

    def assertXMLDocumentsIgnoreNamespacesInScope(left: JDocument, right: JDocument): Unit = {

        val result = Dom4j.compareDocumentsIgnoreNamespacesInScope(left, right)

        // Produce a nicer message
        if (! result) {
            assert(Dom4jUtils.domToPrettyString(left) === Dom4jUtils.domToPrettyString(right))
            assert(false)
        }
    }

    def assertXMLElementsIgnoreNamespacesInScopeCollapse(left: JElement, right: JElement): Unit = {

        val result = Dom4j.compareElementsIgnoreNamespacesInScopeCollapse(left, right)

        // Produce a nicer message
        if (! result) {
            assert(Dom4jUtils.domToPrettyString(left) === Dom4jUtils.domToPrettyString(right))
            assert(false)
        }
    }
}

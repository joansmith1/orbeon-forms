/**
 * Copyright (C) 2012 Orbeon, Inc.
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
package org.orbeon.oxf.xforms.function

import org.orbeon.oxf.xforms.XFormsObject
import org.orbeon.oxf.xforms.control.XFormsControl
import org.orbeon.saxon.expr.{ExpressionTool, Expression, XPathContext}
import org.orbeon.saxon.om._
import org.orbeon.saxon.value.{Int64Value, IntegerValue, BooleanValue, StringValue}
import collection.JavaConverters._
import org.orbeon.scaxon.XML._

protected trait FunctionSupport extends XFormsFunction {

    import XFormsFunction._

    def stringArgument(i: Int)(implicit xpathContext: XPathContext) =
        arguments(i).evaluateAsString(xpathContext).toString

    def stringArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        arguments.lift(i) map (_.evaluateAsString(xpathContext).toString)

    def stringValueArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        itemsArgumentOpt(i) map (_.getStringValue)

    def stringArgumentOrContextOpt(i: Int)(implicit xpathContext: XPathContext) =
        stringArgumentOpt(i) orElse (Option(xpathContext.getContextItem) map (_.getStringValue))

    def longArgument(i: Int, default: Long)(implicit xpathContext: XPathContext) =
        longArgumentOpt(i) getOrElse default

    def longArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        arguments.lift(i) flatMap evaluateAsLong

    def booleanArgument(i: Int, default: Boolean)(implicit xpathContext: XPathContext) =
        booleanArgumentOpt(i) getOrElse default

    def booleanArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        arguments.lift(i) map effectiveBooleanValue

    def itemsArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        arguments.lift(i) map (_.iterate(xpathContext))

    def itemArgumentOpt(i: Int)(implicit xpathContext: XPathContext) =
        itemsArgumentOpt(i) map (_.next())

    def itemArgumentOrContextOpt(i: Int)(implicit xpathContext: XPathContext) =
        Option(itemArgumentOpt(i) getOrElse xpathContext.getContextItem)

    def itemsArgumentOrContextOpt(i: Int)(implicit xpathContext: XPathContext) =
        itemsArgumentOpt(i) getOrElse SingletonIterator.makeIterator(xpathContext.getContextItem)

    // Resolve the relevant control by argument expression
    def relevantControl(i: Int)(implicit xpathContext: XPathContext): Option[XFormsControl] =
        relevantControl(arguments(i).evaluateAsString(xpathContext).toString)

    // Resolve a relevant control by id
    def relevantControl(staticOrAbsoluteId: String)(implicit xpathContext: XPathContext): Option[XFormsControl] =
        resolveOrFindByStaticOrAbsoluteId(staticOrAbsoluteId) collect
            { case control: XFormsControl if control.isRelevant ⇒ control }

    // Resolve an object by id
    def resolveOrFindByStaticOrAbsoluteId(staticOrAbsoluteId: String)(implicit xpathContext: XPathContext): Option[XFormsObject] =
        context.container.resolveObjectByIdInScope(getSourceEffectiveId, staticOrAbsoluteId)

    def resolveStaticOrAbsoluteId(staticIdExpr: Option[Expression])(implicit xpathContext: XPathContext): Option[String] =
        staticIdExpr match {
            case None ⇒
                // If no argument is supplied, return the closest id (source id)
                Option(getSourceEffectiveId)
            case Some(expr) ⇒
                // Otherwise resolve the id passed against the source id
                val staticOrAbsoluteId = expr.evaluateAsString(xpathContext).toString
                resolveOrFindByStaticOrAbsoluteId(staticOrAbsoluteId) map
                    (_.getEffectiveId)
        }

    def effectiveBooleanValue(e: Expression)(implicit xpathContext: XPathContext) =
        ExpressionTool.effectiveBooleanValue(e.iterate(xpathContext))

    def evaluateAsLong(e: Expression)(implicit xpathContext: XPathContext) =
        Option(e.evaluateItem(xpathContext)) flatMap {
            case v: Int64Value   ⇒ Some(v.longValue)
            case v: IntegerValue ⇒ throw new IllegalArgumentException("integer value out of range for Long")
            case v               ⇒ None
        }

    def asIterator(v: Array[String]) = new ArrayIterator(v map StringValue.makeStringValue)
    def asIterator(v: Seq[String])   = new ListIterator (v map StringValue.makeStringValue asJava)

    implicit def stringIteratorToSequenceIterator(i: Iterator[String]) : SequenceIterator = i map stringToStringValue

    implicit def itemSeqOptToSequenceIterator(v: Option[Seq[Item]])    : SequenceIterator = v map (s ⇒ new ListIterator(s.asJava)) getOrElse EmptyIterator.getInstance
    implicit def stringSeqOptToSequenceIterator(v: Option[Seq[String]]): SequenceIterator = v map asIterator getOrElse EmptyIterator.getInstance

    implicit def stringToStringValue(v: String)                        : StringValue      = StringValue.makeStringValue(v)
    implicit def booleanToBooleanValue(v: Boolean)                     : BooleanValue     = BooleanValue.get(v)

    implicit def stringOptToStringValue(v: Option[String])             : StringValue      = v map stringToStringValue orNull
    implicit def booleanOptToBooleanValue(v: Option[Boolean])          : BooleanValue     = v map booleanToBooleanValue orNull
}

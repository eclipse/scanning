/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Richard Kennard
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Richard Kennard - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an arbitrary Metawidget attribute.
 * <p>
 * This annotation can be used when no other Inspector is available for the given attribute, and as
 * an alternative to using XmlInspector.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiAttribute {

	/**
	 * Attribute to set.
	 * <p>
	 * Multiple attributes can be specified if you need to set multiple attributes to the same
	 * value.
	 */

	String[] name();

	/**
	 * Value to set the attribute to.
	 * <p>
	 * This can be an EL expression if using an <code>InspectionResultProcessor</code> such as
	 * <code>FacesInspectionResultProcessor</code> or <code>JexlInspectionResultProcessor</code>.
	 */

	String value();
}

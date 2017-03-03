/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Richard Kennard.
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
 * Annotates the field comes after the other given field(s) in the UI.
 * <p>
 * Controlling field ordering by annotating fields is an alternative to using one of the XML-based
 * <code>Inspectors</code> (XML nodes are inherently ordered), or using
 * <code>JavassistPropertyStyle</code>.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiComesAfter {

	/**
	 * Array of property names which the annotated property must come after.
	 * <p>
	 * Specifying multiple names can be useful if the annotated property is intermingled with other
	 * properties in subclasses.
	 * <p>
	 * If no names are specified, the annotated property will come after all other properties.
	 */

	String[] value() default {};
}

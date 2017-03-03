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
 * Annotates the field should be preceding by a section heading.
 * <p>
 * The 'camel-cased' version of the section name will first be looked up in any relevant UI
 * <code>ResourceBundle</code>. If no match is found, the section name will be output 'as is'.
 * <p>
 * Once a section heading has been declared, subsequent fields are assumed to belong to the same
 * section until a different section heading is encountered. Sections can be cancelled using a
 * section heading with an empty String. Sections can be nested by specifying multiple section
 * names.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiSection {

	String[] value();
}

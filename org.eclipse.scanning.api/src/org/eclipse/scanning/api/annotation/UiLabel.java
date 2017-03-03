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
 * Annotates the field should have the given label in the UI.
 * <p>
 * If no UI <code>ResourceBundle</code> is in use, the label will be output 'as is'. If a resource
 * bundle <em>has</em> been specified, the 'camel-cased' version of the label will be looked up in
 * the bundle. This means developers can initially build their UIs without worrying about resource
 * bundles, then turn on localization support later.
 * <p>
 * To remove the label entirely (including its column) specify an empty String. To render a blank
 * label (preserving its column) specify a value that gets looked up in a
 * <code>ResourceBundle</code>, and have the <code>ResourceBundle</code> return an empty String.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiLabel {

	String value();
}

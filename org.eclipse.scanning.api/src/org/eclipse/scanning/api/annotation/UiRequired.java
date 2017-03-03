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
 * Annotates the field should be 'required' in the UI. For example, should be displayed with a star
 * after it and, depending on the target UI platform, have 'required' validators attached to it.
 * <p>
 * Note: Metawidget is designed to use <em>existing</em> annotations as much as possible. Clients
 * should use something like <code>javax.persistence.Column(nullable = false)</code> or
 * <code>org.hibernate.validator.NotNull</code> in preference to <code>UiRequired</code>.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiRequired {
	// Just a marker annotation
}

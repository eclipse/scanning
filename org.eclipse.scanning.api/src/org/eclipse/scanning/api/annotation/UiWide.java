/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Richard Kennard.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Illya Yalovyy - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.scanning.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates the field should be 'wide' in the UI, spanning all columns in a multi-column layout.
 * <p>
 * UiWide is different to UiLarge, because 'large' implies a data size (ie. BLOB or CLOB) whereas
 * 'wide' refers purely to spanning columns. Generally all 'large' fields are implicitly 'wide', but
 * not all 'wide' fields are 'large'. For example, you may want a normal text field (not a textarea)
 * to span all columns.
 *
 * @author Illya Yalovyy
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD } )
@Deprecated
public @interface UiWide {
	// Just a marker annotation
}

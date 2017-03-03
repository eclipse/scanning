/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.device.ui.model;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

class EnableIfColumnLabelProvider extends ColumnLabelProvider {

	private Font  italic;

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getForeground(Object ofield) {
		if (ofield == null) return null;

		FieldValue field  = (FieldValue)ofield;
		if (isEnabled(field)) {
			return null;
		} else {
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
		}
	}
	public Font getFont(Object ofield) {
		if (ofield == null) return null;

		FieldValue field  = (FieldValue)ofield;
		if (isEnabled(field)) {
			return null;
		} else {
			if (italic == null) {
				try {
					final FontData shellFd = Display.getDefault().getActiveShell().getFont().getFontData()[0];
					FontData fd = new FontData(shellFd.getName(), shellFd.getHeight(), SWT.ITALIC);
					italic = new Font(null, fd);
				} catch (Exception ne) {
					return null; // Otherwise a dialog is shown to the user!
				}
			}
			return italic;
		}
	}

	private static boolean isEnabled(FieldValue field) {
    	final FieldDescriptor anot  = field.getAnnotation();
    	final Object      model = field.getModel();
    	return ModelFieldEditorFactory.isEnabled(model, anot);
	}


	public void dispose() {
		if (italic!=null)   italic.dispose();
        super.dispose();
	}
}

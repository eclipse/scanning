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
package org.eclipse.scanning.device.ui.device;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This is a useful label provider for formatting the value of an IScannable in
 * the standard way in a table.
 * 
 * @author Matthew Gerring
 *
 */
public class ScannableValueLabelProvider extends ColumnLabelProvider implements IStyledLabelProvider {
	
	private static final Logger logger = LoggerFactory.getLogger(ScannableValueLabelProvider.class);
		
	public ScannableValueLabelProvider() {
		Activator.getStore().setDefault(DevicePreferenceConstants.NUMBER_FORMAT, "##########0.0###");
	}

	@Override
	public StyledString getStyledText(Object element) {

		if(!(element instanceof IScannable<?>)) return new StyledString();

		final String       text       = getText(element);
		final StyledString styledText = new StyledString(text!=null?text:"");
		
		IScannable<?> scannable = (IScannable<?>)element;
		try {

			if (scannable.getUnit() != null) {
				styledText.append("    ");
				styledText.append(scannable.getUnit(), StyledString.DECORATIONS_STYLER);
						
			} else {
		
				// Intentionally do nothing!
			}
		} catch (Exception ne) {
			String message = ne.getMessage() == null ? "" : ne.getMessage();
			return styledText.append(message, StyledString.QUALIFIER_STYLER);
		}
		
		return styledText;
	}

	@Override
	public String getText(Object element) {

		if(!(element instanceof IScannable<?>)) return super.getText(element);

		IScannable<?> scannable = (IScannable<?>)element;		
		try {

			Object value = scannable.getPosition();

			if (value == null) return "!VALUE";
			if (value instanceof Number) {
				try {
					final DecimalFormat format = new DecimalFormat(Activator.getStore().getString(DevicePreferenceConstants.NUMBER_FORMAT));
					return format.format(value); 
				} catch (Exception ne) {
					logger.warn("Could not format value as a decimal: ", value);
					return value.toString();
				}
			}
			if (value instanceof Object[]) {
				return Arrays.toString((Object[]) value);
			} else if (value instanceof double[]) {
				return Arrays.toString((double[]) value);
			}

			return value.toString();

		} catch (Exception ne) {
			logger.error("Error with value for "+scannable, ne);
			return ne.toString();
		}

	}

}

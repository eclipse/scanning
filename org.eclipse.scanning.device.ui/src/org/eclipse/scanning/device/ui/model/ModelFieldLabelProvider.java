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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.annotation.ui.EditType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FieldUtils;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModelFieldLabelProvider extends EnableIfColumnLabelProvider {

	private Image ticked;
	private Image unticked;
	
	private static final Logger logger = LoggerFactory.getLogger(ModelFieldLabelProvider.class);
	
	private IScannableDeviceService cservice;
	private final ModelViewer       viewer;
	
	public ModelFieldLabelProvider(ModelViewer viewer) {
		this.viewer = viewer;
		try {
			cservice = ServiceHolder.getEventService().createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IScannableDeviceService.class);
		} catch (Exception e) {
			logger.error("Unable to make a remote connection to "+IScannableDeviceService.class.getSimpleName());
		}
	}
	
	public void dispose() {
		if (ticked!=null)   ticked.dispose();
		if (unticked!=null) unticked.dispose();
		super.dispose();
	}
	
	public Color getForeground(Object ofield) {
		Color ret = super.getForeground(ofield);
		if (ret!=null) return ret;
		if (ofield instanceof FieldValue && viewer.isValidationError((FieldValue)ofield)) {
			return Display.getDefault().getSystemColor(SWT.COLOR_RED);
		} else {
			return null;
		}
	}


	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	public Image getImage(Object ofield) {
		
		if (ofield == null) return null;
		
		FieldValue field  = (FieldValue)ofield;
		Object   element  = field.get();
		if (element instanceof Boolean) {
			if (ticked==null)   ticked   = Activator.getImageDescriptor("icons/ticked.png").createImage();
			if (unticked==null) unticked = Activator.getImageDescriptor("icons/unticked.gif").createImage();
			Boolean val = (Boolean)element;
			return val ? ticked : unticked;
		}
		return null;
	}

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 * 
	 * This renderer is called by the table and some cell editors.
	 * It does not always get asked to render a FieldValue
	 */
	public String getText(Object ofield) {
		
		if (ofield == null)            return "";
		
		StringBuilder buf = new StringBuilder();
		try {
			if (ofield instanceof FieldValue) {
				appendFieldText(buf, (FieldValue)ofield);
			} else {
				appendCompoundText(buf, null, ofield);
			}
		} catch (Exception ne) {
			// Do not keep logging this exception, it's a table render action and
			// would repeat nausiously in the log file for no benefit.
			buf.append(ne.getMessage());
		}
		return buf.toString();
	}
	
	private void appendFieldText(StringBuilder buf, FieldValue ofield) throws Exception {
		FieldValue field  = (FieldValue)ofield;
		Object   element  = field.get();
		if (element == null)    {
			buf.append(field.getAnnotation().edit()==EditType.COMPOUND ? "..." :  "");
			return;
		}
		if (element instanceof Boolean) return;
		
		if (element.getClass()!=null &&element.getClass().isArray()) {
			buf.append( StringUtils.toString(element) );
		} else {
		    appendLabel(buf, field, element);//$NON-NLS-1$
		}
	}

	private void appendLabel(StringBuilder buf, FieldValue field, Object element) throws Exception {

		if (field!=null&&field.getAnnotation()!=null && field.getAnnotation().edit()==EditType.COMPOUND) {
			appendCompoundText(buf, field.getAnnotation().compoundLabel(), element);
		} else {
			buf.append(element.toString());//$NON-NLS-1$
			buf.append(getUnit(field));
		}
	}
	
	private String getLabel(FieldValue field, Object element) throws Exception {
		StringBuilder buf = new StringBuilder();
		appendLabel(buf, field, element);
		return buf.toString();
	}

	private void appendCompoundText(StringBuilder buf, final String compoundLabel, Object element) throws Exception {
		
		try {
		    Method ts = element.getClass().getMethod("toString");
		    if (ts.getDeclaringClass()==element.getClass()) {
		        buf.append(ts.invoke(element)); // They made a special impl of toString for us to use
		        return;
		    }
		} catch (Exception ignored) {
		    // We continue to the model's fields.
		}

		Collection<FieldValue> fields = FieldUtils.getModelFields( element );
		if (compoundLabel!=null && compoundLabel.length()>0) {
			String replace = compoundLabel;
			for (FieldValue fieldValue : fields) {
				String with = "${"+fieldValue.getName()+"}";
				if (replace.contains(with)) {
					String value = getLabel(fieldValue, fieldValue.get());
				    replace = replace.replace(with, value);
				};
			}
			buf.append(replace);
			
		} else {
			buf.append("[");
			for (FieldValue fieldValue : fields) {
				buf.append(fieldValue.getDisplayName().trim());
				buf.append("=");
				appendLabel(buf, fieldValue, fieldValue.get());
				buf.append(", ");
			}	
			buf.append("]");
		}
	}

	
	private String getUnit(FieldValue field) {
		
		FieldDescriptor anot = field.getAnnotation();
		if (anot!=null) {
			if (anot.scannable().length()>0 && cservice !=null) {
				try {
				    String scannableName = (String)FieldValue.get(field.getModel(), anot.scannable());
				    
				    if (scannableName!=null && scannableName.length()>0) {
					    IScannable<?> scannable = cservice.getScannable(scannableName);
					    
					    String unit = scannable.getUnit();
					    if (unit!=null && unit.length()>0) {
					        return " "+scannable.getUnit();
					    }
				    }
				    
				} catch (Exception ne) {
					ne.printStackTrace();
				}
			}
			
			if (anot.unit().length()>0) return " "+anot.unit();
		}
		return "";
	}

}

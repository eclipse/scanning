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
package org.eclipse.scanning.api;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.annotation.ui.FieldValue;

public class ModelValidationException extends ValidationException {
	// Use an unchecked exception because IPointGenerator.iterator() cannot
	// throw a checked exception. (TODO: Why can't we change signature of
	// IPointGenerator.iterator())?

	private static final long serialVersionUID = -2818058720202899355L;
	private Object model;
	private String[] fieldNames;

	public ModelValidationException(String message, Object model, String... fieldNames) {
		super(message);
		this.model      = model;
		this.fieldNames = fieldNames;
		
		boolean fieldFound = false;
		List<String> fields = Arrays.asList(fieldNames);
		if (fieldNames!=null && fieldNames.length>0) for (Field method : getFields(model)) {
			if (fields.contains(method.getName())) {
				fieldFound = true;
				break;
			}
		}
		if (!fieldFound) throw new RuntimeException("No field(s) '"+Arrays.toString(fieldNames)+"' has been found in Class "+model.getClass().getSimpleName());
	}
	
	public ModelValidationException(Exception e) {
		super(e);
	}

	private List<Field> getFields(Object model) {
		List<Field> fields = new ArrayList<>();
		Class<? extends Object> cls = model.getClass();

		while (!cls.equals(Object.class)) { 
			fields.addAll(Arrays.asList(cls.getDeclaredFields()));
			cls = cls.getSuperclass();
		}
		return fields;
	}

	public ModelValidationException(RuntimeException e) {
		super(e);
	}

	public Object getModel() {
		return model;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public boolean isField(FieldValue field) {
		if (fieldNames==null) return false;
		return Arrays.asList(fieldNames).contains(field.getName());
	}

}

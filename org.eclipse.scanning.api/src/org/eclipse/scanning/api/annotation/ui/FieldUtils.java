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

package org.eclipse.scanning.api.annotation.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldUtils {

	
	public static boolean isFileType(Class<? extends Object> clazz) {
		if (File.class.isAssignableFrom(clazz))       return true;
		if (Path.class.isAssignableFrom(clazz))       return true;
		if (clazz.getName().equals("org.eclipse.core.resources.IResource"))  return true;
		return false;
	}

	public static FieldDescriptor getAnnotation(Object model, String fieldName) throws NoSuchFieldException, SecurityException{

		Field field = getField(model, fieldName);
		if (field!=null) {
			FieldDescriptor anot = field.getAnnotation(FieldDescriptor.class);
			if (anot!=null) {
				return anot;
			}
		}
		return null;

	}
	
	public static Field getField(Object model, String fieldName) throws NoSuchFieldException, SecurityException {
		
    	Field field;
    	Class<? extends Object> cls = model.getClass();
    	while (!cls.equals(Object.class)) {
			try {
				field = cls.getDeclaredField(fieldName);
				return field;
			} catch (Exception ne) {
				cls = cls.getSuperclass();
			}
    	}
    	throw new NoSuchFieldException(fieldName);
	}
	
	/**
	 * Get a collection of the fields of the model that should be edited in the User interface
	 * for editing the model.
	 * 
	 * @return collection of fields.
	 * @throws Exception
	 */
	public static Collection<FieldValue> getModelFields(Object model) throws Exception {
		
		// Decided not to use the obvious BeanMap here because class problems with
		// GDA and we have to read annotations anyway.
		final List<Field> allFields = new ArrayList<Field>(31);
		Class<? extends Object> cls = model.getClass();

		while (!cls.equals(Object.class)) { 
			allFields.addAll(Arrays.asList(cls.getDeclaredFields()));
			cls = cls.getSuperclass();
		}		
		
		// The returned descriptor
		final Map<String, FieldValue> map = new HashMap<>();
		
		// fields
		for (Field field : allFields) {
			if (map.containsKey(field.getName())) continue; // We do not overwrite repeats from the super.
			
			// If there is a getter/isser for the field we assume it is a model field.
			if (FieldValue.isModelField(model, field.getName())) {			
				map.put(field.getName(), new FieldValue(model, field.getName()));
			}
		}
		
		List<FieldValue> ret = new ArrayList<>(map.values());
		Collections.sort(ret, new Comparator<FieldValue>() {
			@Override
			public int compare(FieldValue o1, FieldValue o2) {
				FieldDescriptor an1, an2;
				try {
					an1 = FieldUtils.getAnnotation(o1.getModel(), o1.getName());
					an2 = FieldUtils.getAnnotation(o2.getModel(), o2.getName());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new RuntimeException("Cannot get field from model, "+o1.getName()+", "+o2.getName(), e);
				}
				if (an1!=null && an2 !=null) {
					if (an1.fieldPosition() != Integer.MAX_VALUE && an2.fieldPosition() != Integer.MAX_VALUE) {
						return (an1.fieldPosition() - an2.fieldPosition());
					}
					else if (an1.fieldPosition() != Integer.MAX_VALUE) {
						return -1;
					}
					else if (an2.fieldPosition() != Integer.MAX_VALUE) {
						return 1;
					}
				}
				return o1.getDisplayName().toLowerCase().compareTo(o2.getDisplayName().toLowerCase());
			}
		});
		
		return ret;
	}

}

package org.eclipse.scanning.api.device.models;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.scanning.api.INameable;

/**
 * 
 * @author Matthew Gerring
 *
 */
public class ModelReflection {


	/**
	 * 
	 * @param model
	 * @return
	 */
	public static final String getName(Object model) {
		
		if (model instanceof INameable) {
			return ((INameable)model).getName();
		}
		try {
			Method getName = model.getClass().getMethod("getName");
			return (String)getName.invoke(model);
		} catch (Exception ne) {
			return null;
		}
	}
	
	/**
	 * 
	 * @param model
	 * @return
	 */
	public static final double getTime(Object model) {
		
		if (model instanceof IDetectorModel) {
			return ((IDetectorModel)model).getExposureTime();
		}
		try {
			Method getName = model.getClass().getMethod("getExposureTime");
			return (Double)getName.invoke(model);
		} catch (Exception ne) {
			return -1;
		}
	}

	/**
	 * 
	 * @param model
	 * @param name
	 * @return
	 */
	public static Object getValue(Object model, String name) {

		boolean isAccessible = false;
		try {
	        Field field = model.getClass().getDeclaredField(name);
	        isAccessible = field.isAccessible();
	        try {
	        	field.setAccessible(true);
		        return field.get(model);
	        } finally {
	        	field.setAccessible(isAccessible);
	        }
		} catch (Exception ne) {
			return null;
		}
	}

	public static Object stringify(Object value) {
		if (value instanceof String) return "'"+value+"'";
		return value;
	}

}

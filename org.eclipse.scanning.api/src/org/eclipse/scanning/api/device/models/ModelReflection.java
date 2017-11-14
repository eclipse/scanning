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
	 * Returns the value of the field of the given object with the given name, or <code>null</code>
	 * if no such field exists or the field has a <code>null</code> value
	 * @param model
	 * @param fieldName
	 * @return
	 */
	public static Object getValue(Object model, String fieldName) {

		boolean isAccessible = false;
		try {
	        Field field = model.getClass().getDeclaredField(fieldName);
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

	public static String stringify(Object value) {
		if (value instanceof String)  return "'"+value+"'";
		if (value instanceof Boolean) { // Python booleans start with an upper case
			String svalue = value.toString();
			svalue = svalue.substring(0, 1).toUpperCase()+svalue.substring(1);
			return svalue;
		}
		return value.toString();
	}

	public static String stringifyField(Object model, String fieldName) {
		Object value = getValue(model, fieldName);
		if (value != null) {
			StringBuilder buf = new StringBuilder();
			buf.append(fieldName);
			buf.append("=");
			buf.append(stringify(value));
			return buf.toString();
		}

		return null;
	}

}

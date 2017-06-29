package org.eclipse.scanning.api.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.eclipse.scanning.api.annotation.ui.FieldUtils;
import org.eclipse.scanning.api.annotation.ui.FieldValue;

/**
 * 
 * This class links to a database bean using reflection.
 * It means that an arbitrary bean may be used outside the {@link IExperimentDatabaseService}
 * which is not in the classpath at the time it is used. The Bean instance
 * is then transmitted to the IExperimentDatabaseService whose classloader is
 * able to load the class referenced and set the fields using reflection.
 * 
 * @author Matthew Gerring
 *
 */
public class Bean {
	
	private String             beanClass;
    private Map<String,Object> data;
	
	public Bean() {
		this(null);
	}
	
	public Bean(String beanClass) {
		this.beanClass = beanClass;
		this.data      = new HashMap<>();
	}
	
	/**
	 * Shortcut method used for when you already have an instance of the bean.
	 * @param theBean
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public Bean(Object theBean) throws Exception {
		
		this.beanClass = theBean.getClass().getName();
		this.data      = new HashMap<>();
		init(theBean);
	}

	/**
	 * Set a field value of the mapped beans.
	 * @param fieldName
	 * @param value
	 */
	public <T> void set(String fieldName, T value) {
		data.put(fieldName, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String fieldName) {
		return (T)data.get(fieldName);
	}
	
	/**
	 * 
	 * @return a Set of the field names
	 */
	public Collection<String> names() {
		return new HashSet<String>(data.keySet());
	}

	public String getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(String beanClass) {
		this.beanClass = beanClass;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanClass == null) ? 0 : beanClass.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bean other = (Bean) obj;
		if (beanClass == null) {
			if (other.beanClass != null)
				return false;
		} else if (!beanClass.equals(other.beanClass))
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}
	
    /**
     * Method reflects the fields into the map of field values.
     * @param theBean
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
	private void init(Object theBean) throws Exception {
		
		Collection<FieldValue> fields = FieldUtils.getModelFields(theBean);
		for (FieldValue field : fields) {
			String fieldName = field.getName();
			try {
				String getterName = getGetterName(fieldName);
				Method getter     = theBean.getClass().getMethod(getterName);
				Object value      = getter.invoke(theBean);
				if (value!=null) {
					data.put(fieldName, value);
				}
			} catch (NoSuchMethodException nsme) {
				continue; // It is legal to have a field with no getter value.
			}
		}
	}
	private static String getGetterName(final String fieldName) {
		if (fieldName == null) return null;
		return getName("get", fieldName);
	}
	private static String getName(final String prefix, final String fieldName) {
		return prefix + getFieldWithUpperCaseFirstLetter(fieldName);
	}
	public static String getFieldWithUpperCaseFirstLetter(final String fieldName) {
		return fieldName.substring(0, 1).toUpperCase(Locale.US) + fieldName.substring(1);
	}

}

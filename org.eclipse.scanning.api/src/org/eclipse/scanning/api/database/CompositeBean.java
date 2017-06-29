package org.eclipse.scanning.api.database;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is a composite of serveral beans which
 * should be upserted/updated/inserted using the 
 * database service. It is used to create single requests
 * which do multiple actions. This allows queues of 
 * composite operations to be created with each one being
 * a single action in terms of what the ISPyB Client submits
 * to the database.
 * 
 * @author Matthew Gerring
 *
 */
public class CompositeBean {
	
	private static final Logger logger = LoggerFactory.getLogger(CompositeBean.class);

	/**
	 * IMPORTANT: Do not use generics here, the data
	 * is almost always heterogeneous.
	 */
	private Map<Operation, Collection<Bean>> data;
    
    public CompositeBean() {
    	data = new LinkedHashMap<>(); // Order matters in the collection of items
    }
    
    /**
     * 
     * @param type
     * @param originalObjectsOrBeans may be some Bean objects or the actual database objects (which will be wrapped with Bean)
     */
    public CompositeBean(Operation type, Object... originalObjectsOrBeans) {
    	this();
    	assertType(type);
  
    	List<Bean> wrapped = new ArrayList<Bean>(originalObjectsOrBeans.length);
    	Arrays.asList(originalObjectsOrBeans).forEach(object->wrapped.add(toBean(object)));
     	data.put(type, wrapped);
    }
   
    private Bean toBean(Object object) {
    	try {
			return object instanceof Bean ? (Bean)object : new Bean(object);
		} catch (NoSuchMethodException 
					| SecurityException 
					| IllegalAccessException 
					| IllegalArgumentException
					| InvocationTargetException e) {
			
			logger.error("Unexpected object tried to be added to "+getClass().getSimpleName(), e);
			return null;
		}
    }

	private void assertType(Operation type) {
       	if (!Operation.ends().contains(type)) {
       		throw new IllegalArgumentException("Each operation set added to the composite must not be a composite. Allowed types are "+Operation.ends());
       	}
	}

	public void add(Operation type, Bean bean) {
    	
    	assertType(type);
    	Collection<Bean> beans = data.get(type);
    	if (beans==null) {
    		beans = new ArrayList<>(7); // Order matters in the collection of items
    		data.put(type, beans);
    	}
    	beans.add(bean);
    }
    
	/**
	 * 
	 * @param type
     * @param originalObjectsOrBeans may be some Bean objects or the actual database objects (which will be wrapped with Bean)
	 * @return
	 */
    public Collection<Bean> set(Operation type, Object... originalObjectsOrBeans) {
       	assertType(type);
    	List<Bean> wrapped = new ArrayList<Bean>(originalObjectsOrBeans.length);
    	Arrays.asList(originalObjectsOrBeans).forEach(object->wrapped.add(toBean(object)));
    	return data.put(type, wrapped);
    }

	public Collection<Bean> get(Operation type) {
		return data.getOrDefault(type, Collections.emptyList());
	}
	
	public Map<Operation, Collection<Bean>> getData() {
		return data;
	}

	public void setData(Map<Operation, Collection<Bean>> data) {
		this.data = data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		CompositeBean other = (CompositeBean) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}


}

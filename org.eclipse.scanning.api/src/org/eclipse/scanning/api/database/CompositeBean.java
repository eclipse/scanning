package org.eclipse.scanning.api.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

	/**
	 * IMPORTANT: Do not use generics here, the data
	 * is almost always heterogeneous.
	 */
	private Map<Operation, Collection<Object>> data;
    
    public CompositeBean() {
    	data = new LinkedHashMap<>(); // Order matters in the collection of items
    }
    
    public CompositeBean(Operation type, Object... beans) {
    	this();
    	assertType(type);
     	data.put(type, new ArrayList<>(Arrays.asList(beans)));
    }
   
    private void assertType(Operation type) {
       	if (!Operation.ends().contains(type)) {
       		throw new IllegalArgumentException("Each operation set added to the composite must not be a composite. Allowed types are "+Operation.ends());
       	}
	}

	public void add(Operation type, Object bean) {
    	
    	assertType(type);
    	Collection<Object> beans = data.get(type);
    	if (beans==null) {
    		beans = new ArrayList<>(7); // Order matters in the collection of items
    		data.put(type, beans);
    	}
    	beans.add(bean);
    }
    
    public Collection<Object> set(Operation type, Object... beans) {
       	assertType(type);
    	return data.put(type, new ArrayList<>(Arrays.asList(beans)));
    }

	public Collection<Object> get(Operation type) {
		return data.getOrDefault(type, Collections.emptyList());
	}
	

	public Map<Operation, Collection<Object>> getData() {
		return data;
	}

	public void setData(Map<Operation, Collection<Object>> data) {
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

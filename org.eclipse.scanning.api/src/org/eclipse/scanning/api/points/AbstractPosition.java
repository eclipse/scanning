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
package org.eclipse.scanning.api.points;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public abstract class AbstractPosition implements IPosition, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8555957478192358365L;
	
	private int stepIndex = -1;
	protected List<Collection<String>> dimensionNames; // Dimension->Names@dimension
	
	public int getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(int stepIndex) {
		this.stepIndex = stepIndex;
	}

	public final IPosition compound(IPosition parent) {
		if (parent==null) return this; // this+null = this
		final MapPosition ret = new MapPosition();
		ret.putAll(parent);
		ret.putAll(this);
		ret.putAllIndices(parent);
		ret.putAllIndices(this);
		ret.setStepIndex(getStepIndex());
		
		List<Collection<String>> dimensionNames = new ArrayList<>();
		dimensionNames.addAll(((AbstractPosition) parent).getDimensionNames());
		dimensionNames.addAll(getDimensionNames());
		ret.setDimensionNames(dimensionNames);
		
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		final Collection<String> names   = getNames();
		for (String name : names) {
			Object val = get(name);
			if (val instanceof Number) {
			    temp = Double.doubleToLongBits(((Number)val).doubleValue());
			} else {
				temp = val.hashCode();
			}
			result = prime * result + (int) (temp ^ (temp >>> 32));
		}
  	    return result+stepIndex;
	}

	/**
	 * Do not override equals. 
	 * 
	 * MapPostion("x:1,y:1") should equal Point("x", 1, "y",1)
	 * because they represent the same motor values.
	 * 
	 * @param obj
	 * @param checkStep
	 * @return
	 */
	public final boolean equals(Object obj, boolean checkStep) {
		
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (checkStep) {
			if (stepIndex != ((IPosition)obj).getStepIndex())
				return false;
		}

		final Collection<String> ours   = getNames();
		final Collection<String> theirs = ((IPosition)obj).getNames();
		if (!equals(ours, theirs)) return false;		
		for (String name : ours) {
			Object val1 = get(name);
			Object val2 = ((IPosition)obj).get(name);
			if (val1==null && val2!=null) return false;
			if (val1!=null && val2==null) return false;
			if (!val1.equals(val2)) return false;
		}
		
		final Map<String, Integer> iours   = getIndices();
		final Map<String, Integer> itheirs = getIndices((IPosition)obj);
		if (!iours.equals(itheirs)) return false;		
	
		if (!equals(getDimensionNames(), getDimensionNames((IPosition)obj))) return false;		

		return true;
	}

	private Map<String, Integer> getIndices(IPosition pos) {
		if (pos instanceof AbstractPosition) return ((AbstractPosition)pos).getIndices(); // Might be null
		Map<String, Integer> ret = new LinkedHashMap<String, Integer>(pos.getNames().size());
		for (String name : pos.getNames()) ret.put(name,  pos.getIndex(name));
		return ret;
	}

	/**
	 * This equals does an equals on two collections
	 * as if they were two lists because order matters with the names.
	 * @param o
	 * @param t
	 * @return
	 */
    private boolean equals(Collection<?> o, Collection<?> t) {
        
    	if (o == t)
            return true;
    	if (o == null && t == null)
            return true;
    	if (o == null || t == null)
            return false;
 
        Iterator<?> e1 = o.iterator();
        Iterator<?> e2 = t.iterator();
        while (e1.hasNext() && e2.hasNext()) {
            Object o1 = e1.next();
            Object o2 = e2.next();
            
            // Collections go down to the same equals.
            if (o1 instanceof Collection && o2 instanceof Collection) {
            	boolean collectionsEqual = equals((Collection<?>)o1,(Collection<?>)o2);
            	if (!collectionsEqual) {
            		return false;
            	} else {
            		continue;
            	}
            }
            
            // Otherwise we use object equals.
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

	@Override
	public boolean equals(Object obj) {
		return equals(obj, true);
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder("[");
		final Collection<String> names   = getNames();
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
			String name = it.next();
        	buf.append(name);
        	int index = getIndex(name);
        	if(index>-1) {
	        	buf.append("(");
	        	buf.append(index);
	        	buf.append(")");
        	}
         	buf.append("=");
        	buf.append(get(name));
        	if (it.hasNext()) buf.append(", ");
		}
        buf.append(", step=");
        buf.append(getStepIndex());
        
    	buf.append("]");
    	return buf.toString();
	}
	
	public Collection<String> getDimensionNames(int dimension) {
		if (dimensionNames==null && dimension==0) return getNames();
		if (dimensionNames==null)                 return null;
		if (dimension>=dimensionNames.size())     return null;
		return dimensionNames.get(dimension);
	}
	private List<Collection<String>> getDimensionNames(IPosition pos) {
		if (pos instanceof AbstractPosition) return ((AbstractPosition)pos).getDimensionNames();
		return null; // Do not have to support dimension names
	}

	/**
	 * This method makes dimensionNames if they are null.
	 * It must be synchronized because getDimensionNames() 
	 * is called within the thread pool, for instance when
	 * neXus writing positions.
	 * 
	 * Quite a lot of tests were intermittently failing the
	 * tests because of this issue. Be careful when creating
	 * member data in this class that things are thread safe.
	 * 
	 * @return
	 */
	public synchronized List<Collection<String>> getDimensionNames() {
		if (dimensionNames==null||dimensionNames.isEmpty())  {
			dimensionNames = new ArrayList<>();
			dimensionNames.add(new ArrayList<>(getNames())); // List adding a collection, we copy the keys here run SerializationTest to see why
		}
		return dimensionNames;
	}
	public void setDimensionNames(List<Collection<String>> dNames) {
		this.dimensionNames = dNames;
	}

	@Override
	public int getScanRank() {
		return getDimensionNames().size();
	}

	@Override
	public int getIndex(int dimension) {
		final String name = getDimensionNames(dimension).iterator().next();
		return getIndex(name);
	}
}

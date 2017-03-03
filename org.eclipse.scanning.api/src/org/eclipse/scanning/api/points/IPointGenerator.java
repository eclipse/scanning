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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scanning.api.IModelProvider;
import org.eclipse.scanning.api.IValidator;


/**
 * Generator for a give type, T (for instance LissajousModel).
 * 
 * The generator is an iterator used in the scan and a controller object 
 * for the user interface which provides naming information about the
 * type of scan.
 * 
 * @see IDeviceDependentIterable which allows a point generator to be created
 * which instructs the scanning never to look at the points until they are in a scan.
 * Useful for positions that interact with hardware as they are generated.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IPointGenerator<T> extends Iterable<IPosition>, IValidator<T>, IModelProvider<T> {
	
	/**
	 * The model for the generator.
	 * @return
	 */
	T getModel();
	void setModel(T model) throws GeneratorException;
	
	/**
	 * The class which contains points, may be null.
	 * @return
	 */
	List<IPointContainer> getContainers();
	void setContainers(List<IPointContainer> container) throws GeneratorException;
	
	/**
	 * The regions for the generator.
	 * @return
	 */
	Collection<Object> getRegions();
	void setRegions(Collection<Object> region) throws GeneratorException;

	/**
	 * The size of the points iterator. This call will be as fast as possible
	 * but can be as slow as iterating all points.
	 * @return
	 */
	int size() throws GeneratorException;
	
	/**
	 * Iterator over the points, fast because does not evaluate
	 * all points straight away, does it on the fly.
	 * 
	 * @return
	 */
	Iterator<IPosition> iterator();
	
	/**
	 * Relatively slow because all the points have to exist in memory.
	 * Points are lightweight and it is normally ok to have them all in memory.
	 * However if it can be avoided for a given scan, the scan will start faster.
	 * 
	 * @return
	 */
	List<IPosition> createPoints() throws GeneratorException;
	
	/**
	 * The id for this generator. Generators defined by extension must set an it.
	 * Those defined by 
	 * 
	 * @return
	 */
	public String getId();
	public void setId(String id);
	
	/**
	 * The short label shown to the user for this generator.
	 * @return
	 */
	public String getLabel();
	public void setLabel(String label);
	
	/**
	 * The long description shown to the user for this generator.
	 */
	public String getDescription();
	public void setDescription(String description);
	
	/**
	 * 
	 * @return true if the user should be able to use this generator in the user interface.
	 */
	public boolean isVisible();
	public void setVisible(boolean vis);
	
	/**
	 * 
	 * @return false if the user has disabled this generator from the compound scan but does not want to delete it.
	 */
	public boolean isEnabled();
	public void setEnabled(boolean enabled);
	
	/**
	 * The relative icon path to provide a custom icon for the generator.
	 * 
	 * If using extension points, no need to set the icon path, the extension point
	 * will read the icon for the generator.
	 * 
	 * @return
	 */
	public String getIconPath();
	public void setIconPath(String path);
	
	/**
	 * Most generators can be used with Jython/CPython and are interoperable with
	 * the malcolm layer. Others are java only because they are not needed to be
	 * operated with the malcolm layer.
	 * 
	 * @return
	 */
	default boolean isScanPointGeneratorFactory() {
		return true;
	}
	
	/**
	 * By default this gets the first point from the iterator.
	 * Override to define a more efficient first point, for instance
	 * if the iterator does a sleep this method can be overridden to avoid
	 * the sleep.
	 * 
	 * @return
	 */
	default IPosition getFirstPoint() {
		return iterator().next();
	}
}

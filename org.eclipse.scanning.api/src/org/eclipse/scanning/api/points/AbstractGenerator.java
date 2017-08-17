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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;

/**
 *
 * @author Matthew Gerring
 *
 * @param <T>
 * @param <P>
 */
public abstract class AbstractGenerator<T> implements IPointGenerator<T>, Iterable<IPosition> {

	protected volatile T model; // Because of the validateModel() method

	protected List<IPointContainer> containers;
	protected Collection<Object> regions = new ArrayList<Object>();
	private String id;
	private String label;
	private String description;
	private String iconPath;
	private boolean visible=true;
	private boolean enabled=true;
	private int[] shape = null;

	protected AbstractGenerator() {
		super();
		this.id = getClass().getName();
	}

	protected AbstractGenerator(String id) {
		this.id = id;
	}

	@Override
	public T getModel() {
		return model;
	}

	@Override
	public void setModel(T model) {
		this.model = model;
		this.shape = null; // clear cached shape
	}

	@Override
	final public Iterator<IPosition> iterator() {
		validateModel();
		return iteratorFromValidModel();
	};

	/**
	 * Creates and returns an iterator for this model. If possible subclasses should aim to
	 * return an instance of {@link ScanPointIterator}.
	 * @return
	 */
	protected abstract Iterator<IPosition> iteratorFromValidModel();


	/**
	 * If the given model is considered "invalid", this method throws a
	 * ModelValidationException explaining why it is considered invalid.
	 * Otherwise, just returns. A model should be considered invalid if its
	 * parameters would cause the generator implementation to hang or crash.
	 *
	 * @throw exception if model invalid
	 */
	protected void validateModel() throws ValidationException {
		T model = getModel();

		if (model instanceof IBoundingBoxModel) {
			IBoundingBoxModel bmodel = (IBoundingBoxModel)model;
			// As implemented, model width and/or height can be negative,
			// and this flips the slow and/or fast point order.
			if (bmodel.getBoundingBox() == null) throw new ModelValidationException("The model must have a Bounding Box!", model, "boundingBox");
	        if (bmodel.getBoundingBox().getFastAxisLength()==0)  throw new ModelValidationException("The length must not be 0!", bmodel, "boundingBox");
	        if (bmodel.getBoundingBox().getSlowAxisLength()==0)  throw new ModelValidationException("The length must not be 0!", bmodel, "boundingBox");
		}
	}

	/**
	 * The AbstractGenerator has a no argument validateModel() method which
	 * generators have implemented to validate models.
	 * Therefore the model is temporarily set in order to check it.
	 * In order to make that thread safe, model is marked as volatile.
	 */
	@Override
	public void validate(T model) throws ModelValidationException {
		T orig = this.getModel();
		try {
			setModel(model);
			validateModel();

		} catch (SecurityException | IllegalArgumentException e) {
			throw new ModelValidationException(e);
		} finally {
			setModel(orig);
		}
	}

	/**
	 * Final use sizeOfValidModel() to calculate size!
	 */
	@Override
	public final int size() throws GeneratorException {
		validateModel();
		return sizeOfValidModel();
	}

	@Override
	public int getRank() throws GeneratorException {
		return getShape().length;
	}

	@Override
	public int[] getShape() throws GeneratorException {
		if (shape == null) {
			shape = calculateShape();
		}

		return shape;
	}

	/**
	 * Calculates the shape of the scan. This method is called when
	 * {@link #iteratorFromValidModel()} does not return a {@link ScanPointIterator}.
	 * Subclasses should override if a more efficient way of calculating the
	 * scan shape can be provided.
	 *
	 * @param iterator
	 * @return
	 * @throws GeneratorException
	 */
	protected int[] calculateShape() throws GeneratorException {
		Iterator<IPosition> iterator = iteratorFromValidModel();

		// if the iterator is an ScanPointIterator we can ask it for the shape
		if (iterator instanceof ScanPointIterator) {
			return ((ScanPointIterator) iterator).getShape();
		}

		if (!iterator.hasNext()) {
			// empty iterator
			return new int[0];
		}

		IPosition first = iterator.next();
		final int scanRank = first.getScanRank();

		if (scanRank == 0) {
			// scan of rank 0, e.g. static generator for a single empty point
			return new int[0];
		}

		// we fall back on iterating through all the points in the
		// scan to get the dimensions of the last one
		int pointNum = 1;
		IPosition last = first;
		int lastInnerIndex = last.getIndex(scanRank - 1);
		int maxInnerIndex = -1;
		long lastTime = System.currentTimeMillis();
		while (iterator.hasNext()) {
			last = iterator.next(); // Could be large...
			pointNum++;
			if (maxInnerIndex == -1) {
				// check for the max index of the inner most dimension - we assume that when it
				// first decreases the previous value was the max. This special treatment
				int innerIndex = last.getIndex(scanRank - 1);
				if (innerIndex <= lastInnerIndex) {
					maxInnerIndex = lastInnerIndex;
				}
			}

			if (pointNum % 10000 == 0) {
				long newTime = System.currentTimeMillis();
				System.err.println("Point number " + pointNum++ + ", took " + (newTime - lastTime) + "ms");
			}
		}

		// special case for scans of rank 0 - i.e. acquire scans
		if (scanRank == 0) return new int[0];

		// the shape is created from the indices for each dimension for the final scan point
		int[] shape = new int[scanRank];
		for (int i = 0; i < shape.length - 1; i++) {
			shape[i] = last.getIndex(i) + 1;
		}
		// except for the last index, which is the maximum last index found
		// this is due to the special case of snake scans
		shape[shape.length - 1 ] = maxInnerIndex;

		return shape;
	}

	/**
	 * Please override this method, the default creates all points and
	 * returns their size
	 */
	protected int sizeOfValidModel() throws GeneratorException {
		// For those generators which implement an iterator,
		// doing this loop is *much* faster for large arrays
		// because memory does not have to be allocated.
		Iterator<IPosition> it = iterator();

		// Always ask the iterator for size because it is
		// much faster than actual iteration.
		if (it instanceof ScanPointIterator) {
			return ((ScanPointIterator)it).size();
		}
		int index = -1;
		while(it.hasNext()) {
			it.next();
			index++;
		}
		return index+1;
	}

	@Override
	public List<IPosition> createPoints() throws GeneratorException {
		final List<IPosition> points = new ArrayList<IPosition>(89);
		Iterator<IPosition> it = iterator();
		while(it.hasNext()) points.add(it.next());
		return points;
	}

	@Override
	public List<IPointContainer> getContainers() {
		if (containers!=null) return containers;
		return null;
	}

	@Override
	public void setContainers(List<IPointContainer> containers) throws GeneratorException {
		this.containers = containers;
	}

	/**
	 * If there are no containers, the point is considered contained.
	 *
	 * @param x
	 * @param y
	 */
	public boolean containsPoint(IPosition point) {
		if (containers==null)    return true;
		if (containers.size()<1) return true;
		for (IPointContainer container : containers) {
			if (container.containsPoint(point)) return true;
		}
		return false;
	}

	@Override
	public Collection<Object> getRegions() {
		return regions;
	}

	@Override
	public void setRegions(Collection<Object> regions) throws GeneratorException {
		this.regions = regions == null ? new ArrayList<Object>() : regions;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String getIconPath() {
		return iconPath;
	}

	@Override
	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containers == null) ? 0 : containers.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((iconPath == null) ? 0 : iconPath.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + (visible ? 1231 : 1237);
		result = prime * result + ((regions == null) ? 0 : regions.hashCode());
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
		AbstractGenerator<?> other = (AbstractGenerator<?>) obj;
		if (containers == null) {
			if (other.containers != null)
				return false;
		} else if (!containers.equals(other.containers))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (enabled != other.enabled)
			return false;
		if (iconPath == null) {
			if (other.iconPath != null)
				return false;
		} else if (!iconPath.equals(other.iconPath))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (visible != other.visible)
			return false;
		if (regions == null) {
			if (other.regions != null)
				return false;
		} else if (!regions.equals(other.regions))
			return false;
		return true;
	}

}

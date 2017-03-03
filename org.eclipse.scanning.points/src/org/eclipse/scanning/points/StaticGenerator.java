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
package org.eclipse.scanning.points;

import java.util.Iterator;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.StaticPosition;
import org.eclipse.scanning.api.points.models.StaticModel;

public class StaticGenerator extends AbstractGenerator<StaticModel> {

	private static class CountIterator<T> implements Iterator<T> {

		private final T item;
		private int remaining = 0;
		
		public CountIterator(T item, final int size) {
			this.item = item;
			this.remaining = size;
		}
		
		@Override
		public boolean hasNext() {
			return remaining > 0;
		}

		@Override
		public T next() {
			remaining--;
			return item; 
		}
	};

	StaticGenerator() {
		setLabel("Empty");
		setDescription("Empty generator used when wrapping malcolm scans with no CPU steps.");
		setVisible(false);
	}

	@Override
	protected void validateModel() {
		if (model.getSize() < 1) throw new ModelValidationException("Size must be greater than zero!", model, "size");
	}

	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {
		return new CountIterator<IPosition>(new StaticPosition(), model.getSize());
	}
	
	// Users to not edit the StaticGenerator
	public boolean isVisible() {
		return false;
	}
	public boolean isScanPointGeneratorFactory() {
		return false;
	}
}

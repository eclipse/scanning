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

import java.util.NoSuchElementException;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.StaticPosition;
import org.eclipse.scanning.api.points.models.StaticModel;

/**
 * A software generator that generates a static (i.e. empty) point one or more times.
 *
 * @author Matthew Dickie
 */
class StaticGenerator extends AbstractGenerator<StaticModel> {

	private static class StaticPointIterator implements ScanPointIterator {

		private final int size;
		private int remaining = 0;

		public StaticPointIterator(final int size) {
			this.size = size;
			this.remaining = size;
		}

		@Override
		public boolean hasNext() {
			return remaining > 0;
		}

		@Override
		public IPosition next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			remaining--;
			return STATIC_POSITION;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public int[] getShape() {
			return size == 1 ? EMPTY_SHAPE : new int[] { size };
		}

		@Override
		public int getRank() {
			return size == 1 ? 0 : 1;
		}

	}

	private static final int[] EMPTY_SHAPE = new int[0];

	private static final IPosition STATIC_POSITION = new StaticPosition();

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
	protected ScanPointIterator iteratorFromValidModel() {
		return new StaticPointIterator(model.getSize());
	}

	// Users to not edit the StaticGenerator
	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public boolean isScanPointGeneratorFactory() {
		return false;
	}

	@Override
	public int[] getShape() throws GeneratorException {
		return model.getSize() == 1 ? EMPTY_SHAPE : new int[] { model.getSize() };
	}
}

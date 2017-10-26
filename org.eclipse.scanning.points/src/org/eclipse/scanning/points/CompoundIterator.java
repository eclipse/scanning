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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;

/**
 * We are trying to make it super efficient to iterate
 * compound generators by doing this. Otherwise the createPoints(...)
 * would do.
 *
 * TODO Matt D. 2017-10-26 should we do all points generation in the jython layer? If so,
 * we should remove this class.
 *
 * @author Matthew Gerring
 *
 */
public class CompoundIterator implements Iterator<IPosition> {

	private CompoundGenerator     gen;
	private IPosition             lastPosition;
	private Iterator<? extends IPosition>[] iterators;
	private int index;

	public CompoundIterator(CompoundGenerator gen) {
		this.gen = gen;
		this.iterators = initIterators();
		this.lastPosition = createFirstPosition();
		this.index = 0;
	}

	@SuppressWarnings("unchecked")
	private Iterator<? extends IPosition>[] initIterators() {
		return Arrays.stream(gen.getGenerators()).map(IPointGenerator::iterator).toArray(Iterator[]::new);
	}

	private IPosition createFirstPosition() {
		// Before next() is called for the first time, we need to create a compound position that
		// includes the positions of all iterators not just the inner most one. At each call to next()
		// on this iterator this is updated with the new positions of any iterators for which next() are called
	    IPosition position = new MapPosition();
		for (int i = 0; i < iterators.length-1; i++) {
			IPosition with = gen.getGenerators()[i].getFirstPoint();
			if (with==null) with = iterators[i].next();
			position = with.compound(position);
		}
		return position;
	}

	@Override
	public boolean hasNext() {
		return Arrays.stream(iterators).anyMatch(Iterator::hasNext);
	}

	@Override
	public IPosition next() {
		// iterate through iterators starting at the inner most one
		for (int i = iterators.length - 1; i > -1; i--) {
			if (iterators[i].hasNext()) {
				// once we find an iterator for which hasNext() returns true
				// call next() once, update lastPosition with that position
				IPosition next = iterators[i].next();
				lastPosition = next.compound(lastPosition);
				((AbstractPosition) lastPosition).setDimensionNames(gen.getDimensionNames());

				// update the step index and return pos
				lastPosition.setStepIndex(index);
				index++;
				return lastPosition;
			} else if (i > 0) { // for all but the outer most iterator
				// replace this inner iterator with a fresh one for the next outer iteration
				iterators[i] = gen.getGenerators()[i].iterator();
				// call next() once and update lastPosition with the first position of this inner iterator
				IPosition first = iterators[i].next();
				lastPosition = first.compound(lastPosition);
				((AbstractPosition) lastPosition).setDimensionNames(gen.getDimensionNames());
			}
		}

		// hasNext() returned false for all iterators, no more points left
		throw new NoSuchElementException();
	}

}

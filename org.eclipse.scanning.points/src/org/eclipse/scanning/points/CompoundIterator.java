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

import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;

/**
 * We are trying to make it super efficient to iterate
 * compound generators by doing this. Otherwise the createPoints(...) 
 * would do.
 * 
 * @author Matthew Gerring
 *
 */
public class CompoundIterator implements Iterator<IPosition> {

	private CompoundGenerator     gen;
	private IPosition             pos;
	private Iterator<? extends IPosition>[] iterators;
	private int index;

	public CompoundIterator(CompoundGenerator gen) throws GeneratorException {
		this.gen       = gen;
		this.iterators = initIterators();
		this.pos       = createFirstPosition();
		this.index     = -1;
	}

	private IPosition createFirstPosition() throws GeneratorException {
		
	    IPosition pos = new MapPosition();
		for (int i = 0; i < iterators.length-1; i++) {
			IPosition with=null;
			if (gen.getGenerators()[i] instanceof IPointGenerator) with = ((IPointGenerator)gen.getGenerators()[i]).getFirstPoint();
			if (with==null) with = iterators[i].next();
			pos = with.compound(pos);
		}
		return pos;
	}
	
	private IPosition next;
    private boolean justDidNext = false; // This attempts to deal with the case where someone does .next() without .hasNext();
    
	@Override
	public boolean hasNext() {
        next = getNext(); 
        index++;
        if (next!=null) next.setStepIndex(index);
        
        justDidNext = true;
        return next!=null;
	}

	@Override
	public IPosition next() {
		if (!justDidNext) next = getNext(); 
		justDidNext = false;
		return next;
	}
	
	public IPosition getNext() {
		
		for (int i = iterators.length-1; i > -1; i--) {
			if (iterators[i].hasNext()) {
				IPosition next = iterators[i].next();
				pos = next.compound(pos);
				((AbstractPosition)pos).setDimensionNames(gen.getDimensionNames());
				return pos;
			} else if (i>0) {
				iterators[i]    = gen.getGenerators()[i].iterator();
				IPosition first = iterators[i].next();
				pos = first.compound(pos);
				((AbstractPosition)pos).setDimensionNames(gen.getDimensionNames());
			}
		}
		return null;
	}


	private Iterator<? extends IPosition>[] initIterators() {
		final IPointGenerator<?>[] gs = gen.getGenerators();
		@SuppressWarnings("unchecked")
		Iterator<? extends IPosition>[] ret = new Iterator[gs.length];
		for (int i = 0; i < gs.length; i++) {
			ret[i] = gs[i].iterator();
		}
		return ret;
	}

	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}

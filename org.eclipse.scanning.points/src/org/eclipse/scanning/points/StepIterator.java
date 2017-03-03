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

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.models.CollatedStepModel;
import org.eclipse.scanning.api.points.models.StepModel;

class StepIterator implements Iterator<IPosition> {

	private StepModel     model;
	private double        value;
	
	public StepIterator(StepGenerator gen) {
		this(gen.getModel());
	}
	
	public StepIterator(StepModel model) {
		this.model = model;
		value = model.getStart()-model.getStep();
	}

	@Override
	public boolean hasNext() {
		double next = increment();
		double dir = Math.signum(model.getStop() - next);
		return dir == 0 || dir == Math.signum(model.getStop() - model.getStart()); 
	}

	private double increment() {
		return value+model.getStep();
	}

	int index = -1;
	@Override
	public IPosition next() {
		value = increment();
        ++index;
        if (model instanceof CollatedStepModel) {
        	final MapPosition mp = new MapPosition();
        	CollatedStepModel cmodel = (CollatedStepModel)model;
        	if (cmodel.getNames()!=null) {
	        	for (String name : cmodel.getNames()) {
	           		mp.put(name, value);
	           		mp.putIndex(name, index);
				}
	        	return mp;
        	} else {
    		    return new Scalar(model.getName(), index, value);
        	}
        } else {
		    return new Scalar(model.getName(), index, value);
        }
	}

	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}

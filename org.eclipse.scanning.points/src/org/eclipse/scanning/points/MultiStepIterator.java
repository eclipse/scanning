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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

/**
 * An iterator over multiple step ranges. Acts essentially as a sequence of
 * step iterators chained together. In the special case where one step iterator begins
 * with the last value of the previous iterator it is not repeated.
 * 
 * @author Matthew Dickie
 */
public class MultiStepIterator extends AbstractScanPointIterator {
	
	private final MultiStepModel model;
	private int                  index;
	private double[]             points; 
	private double[]             times; 
	
	public MultiStepIterator(MultiStepModel model) {
		this.model = model;
		
		JythonObjectFactory<ScanPointIterator> arrayGeneratorFactory = ScanPointGeneratorFactory.JArrayGeneratorFactory();

		createPositions();

		ScanPointIterator iterator = arrayGeneratorFactory.createObject(model.getName(), "mm", points);
		pyIterator = iterator;
	}
	
	private void createPositions() {
		
		int totalSize = 0;
		boolean finalPosWasEnd = false;
		List<double[]> positionArrays = new ArrayList<>(model.getStepModels().size());
		double previousEnd = 0;
		for (StepModel stepModel : model.getStepModels()) {
			int size = getSize(stepModel);
			double pos = stepModel.getStart();

			// if the start of this model is the end of the previous one, and the end of the
			// previous was was its final point, skip the first point
			if (finalPosWasEnd && 
					Math.abs(stepModel.getStart() - previousEnd) < Math.abs(stepModel.getStep() / 100)) {
				pos = pos += stepModel.getStep();
				size--;
			}
			double[] positions = new double[size];
			
			for (int i = 0; i < size; i++) {
				positions[i] = pos;
				pos += stepModel.getStep();
			}
			positionArrays.add(positions);
			totalSize += size;
			
			// record if the final position of this model is its end position (within a tolerance of step/100)
			// this is not always the case, e.g. if start=0, stop=10 and step=3
			double finalPos = positions[positions.length - 1];
			finalPosWasEnd = Math.abs(stepModel.getStop() - finalPos) < Math.abs(stepModel.getStep() / 100);
			previousEnd = stepModel.getStop();
		}
		
		this.points = new double[totalSize];
		this.times  = new double[totalSize];
		this.index  = 0;
		
		int pos        = 0;
		int sindex     = 0;
		for (double[] positions : positionArrays) {
			System.arraycopy(positions, 0, points, pos, positions.length);
			double time = model.getStepModels().get(sindex).getExposureTime();
			Arrays.fill(times, pos, pos+positions.length, time);
			pos += positions.length;
			sindex+=1;
		}
	}
	
	private static int getSize(StepModel stepModel) {
		// copied from StepGenerator.sizeOfValidModel
		double div = ((stepModel.getStop()-stepModel.getStart())/stepModel.getStep());
		div += (Math.abs(stepModel.getStep()) / 100); // add tolerance of 1% of step value
		return (int)Math.floor(div+1);
	}
	
	@Override
	public boolean hasNext() {
		return pyIterator.hasNext();
	}

	@Override
	public IPosition next() {
		IPosition next = pyIterator.next();
        next.setExposureTime(times[index]);
        next.setStepIndex(index);
        index++;
		return next;
	}

}

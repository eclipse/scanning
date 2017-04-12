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

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;

public class RepeatedPointIterator implements ScanPointIterator {

	private RepeatedPointModel   model;
	private int count = 0;
	
	public RepeatedPointIterator(RepeatedPointGenerator gen) {
		this.model= gen.getModel();
	}

	@Override
	public boolean hasNext() {
		return count<model.getCount();
	}
	
	private static boolean countSleeps;
	private static int     sleepCount;
	   /**
     * For testing we may count the sleeps of an interation
     * @param b
     */
	public static void _setCountSleeps(boolean count) {
		countSleeps = count;
		sleepCount  = 0;
	}

	public static int _getSleepCount() {
		return sleepCount;
	}

	@Override
	public IPosition next() {
		
		if (model.getSleep()>0) {
			try {
				Thread.sleep(model.getSleep());
				if (countSleeps) sleepCount++;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		Scalar<Double> point = new Scalar<>(model.getName(), count, model.getValue());
		count++;
		return point;
	}

	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

	@Override
	public int size() {
		return count;
	}

	@Override
	public int[] getShape() {
		return new int[] { count };
	}

	@Override
	public int getRank() {
		return 1;
	}

}

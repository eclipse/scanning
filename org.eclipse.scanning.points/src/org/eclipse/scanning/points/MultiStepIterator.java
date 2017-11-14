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
import org.eclipse.scanning.api.points.ScanPointIterator;

/**
 * An iterator over multiple step ranges. Acts essentially as a sequence of
 * step iterators chained together. In the special case where one step iterator begins
 * with the last value of the previous iterator it is not repeated.
 *
 * TODO Matt D. 2017-10-26: why do we need to set exposure time? Isn't there some way
 * we can give this to the jython point generator? Adding the exposure time here
 * means this won't work with Malcolm. See JIRA ticket DAQ-888. When this is fixed
 * this class should be removed.
 *
 * @author Matthew Dickie
 */
public class MultiStepIterator extends SpgIterator {

	private final double[] times;

	public MultiStepIterator(ScanPointIterator pyIterator, double[] times) {
		super(pyIterator);
		this.times = times;
	}

	@Override
	public IPosition next() {
		IPosition next = super.next();
        next.setExposureTime(times[next.getStepIndex()]);
		return next;
	}

}

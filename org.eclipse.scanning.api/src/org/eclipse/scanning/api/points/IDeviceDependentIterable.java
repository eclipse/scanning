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

import java.util.List;

/**
 * 
 * Use this interface to return the size of the scan without having to look at all the points.
 * Used whenever the scan positions have to look at hardware to run. Avoids calling
 * the iterator until needed.
 * 
 * 
 * This is a good idea when the iterator checks an moves hardware for each point.
 * Ideally the hardware should all be moved by the scanning and the iterator return the
 * list of precalculated point but in some instances it is desirable to calculate the
 * next position based on where the last position got to. In this case all points
 * cannot be interated until they are run and you must implement this interface to 
 * provide extra scan information such that no points have to be looked at until the scan
 * runs.
 * 
 * @author Matthew Gerring
 *
 */
public interface IDeviceDependentIterable extends Iterable<IPosition> {
	/**
	 * The size of the points iterator without asking for a next call on the iterator. 
	 * This call will be as fast as possible, it may estimate the size, in this case 
	 * its estimation should be >= the actual point iterated.
	 * 
	 * @return
	 */
	int size() throws GeneratorException;

	/**
	 * Provide the names of the scannables at each position without making
	 * an iterator.
	 * 
	 * @return
	 */
	List<String> getScannableNames();

	/**
	 * For scan ranks greater than one please override this
	 * default method to return the real rank of the scan.
	 * @return
	 */
	default int getScanRank() {
		return 1;
	}
	
	/**
	 * Generating the first point is better to do not using the iterator
	 * if each point generation takes time. For instance if there is a 
	 * sleep on the next() method of the iterator, implementing this method
	 * means that the sleep is avoided.
	 * 
	 * @return
	 */
	default IPosition getFirstPoint() {
		return iterator().next();
	}
}

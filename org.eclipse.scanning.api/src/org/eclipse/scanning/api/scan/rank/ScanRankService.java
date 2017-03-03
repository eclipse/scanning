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
package org.eclipse.scanning.api.scan.rank;

import org.eclipse.scanning.api.points.IPosition;

/**
 * 
 * Please use this class to figure out the correct slice
 * from a position in a scan.
 * 
 * @author Matthew Gerring
 *
 */
class ScanRankService implements IScanRankService {

	
	public IScanSlice createScanSlice(IPosition position, int... datashape) {
		
       
		final int scanRank = position.getScanRank();
		final int[] start = new int[scanRank+datashape.length];
		final int[] stop  = new int[scanRank+datashape.length];
		
		for (int dim = 0; dim < scanRank; dim++) {
			start[dim] = position.getIndex(dim);
			stop[dim]  = position.getIndex(dim)+1;
		}

		int index = 0;
		for (int i = datashape.length; i>0; i--) {
			start[start.length-i] = 0;
			stop[stop.length-i]  = datashape[index];
			index++;
		}
	  
		return new ScanSlice(start, stop, null);
	}

}

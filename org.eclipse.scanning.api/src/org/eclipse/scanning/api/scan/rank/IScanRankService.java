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
public interface IScanRankService {

	/**
	 * 
	 * @param context
	 * @param position
	 * @param datashape
	 * @return
	 */
	IScanSlice createScanSlice(IPosition position, int... datashape);
	
	/**
	 * Currently it is possible to implemnent the IScanRankService
	 * directly in the API bundle. It may be necessary in future to 
	 * move this to a bundle with dependencies. In this case this static
	 * method will pick up the implementation of the service via 
	 * osgi services.
	 * 
	 * @return
	 */
    public static IScanRankService getScanRankService() {
    	return scanRankService;
    }
    
    /**
     * Do not use this variable, use getScanRankService();
     */
	static final IScanRankService scanRankService = new ScanRankService();
}

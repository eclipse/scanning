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
package org.eclipse.scanning.test.points;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanEstimator;

/**
 * @author Matthew Gerring
 *
 */
class GeneratorUtil {

	/**
	 * Checks the points list vs the iterator
	 * @param gen
	 * @param expectedShape 
	 * @throws Exception 
	 */
	public static void testGeneratorPoints(IPointGenerator<?> gen, int... expectedShape) throws Exception {
		
		final List points = gen.createPoints();
		final List its   = new ArrayList<>(gen.size());
		final Iterator it = gen.iterator();
		while(it.hasNext()) its.add(it.next());
		
		IPosition[] pnts1 = array(points);
		IPosition[] pnts2 = array(its);
		
		if (pnts2.length!=pnts1.length) throw new Exception("Not the same size! Iterator size is "+its.size()+" full list size is "+points.size());
        for (int i = 0; i < pnts1.length; i++) {
			if (!pnts1[i].equals(pnts2[i])) {
				throw new Exception(pnts1[i]+" does not equal "+pnts2[i]);
			}
		}
		
		// Check the estimator. In this case it is not doing anything
		// that we don't already know, so we can test it.
		final ScanEstimator estimator = new ScanEstimator(gen, (Map) null, 100);
		if (points.size()!=estimator.getSize()) throw new Exception("Different size from shape estimator!");

		if (expectedShape!=null && expectedShape.length>0) {// They set one
		    int[] shape = estimator.getShape();
		    assertArrayEquals(expectedShape, shape);
		}
 	}

	private static IPosition[] array(List<IPosition> p) {
		return p.toArray(new IPosition[p.size()]);
	}
}

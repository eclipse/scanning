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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.eclipse.scanning.points.CompoundSpgIterator;
import org.eclipse.scanning.points.ScanPointGeneratorFactory;
import org.junit.Test;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;

public class ScanPointGeneratorFactoryTest {
	
    @Test
    public void testJLineGeneratorFactory1D() {
        JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				"x", "mm", 1.0, 5.0, 5);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Scalar("x", 0, 1.0));
	    expected_points.add(new Scalar("x", 1, 2.0));
	    expected_points.add(new Scalar("x", 2, 3.0));
	    expected_points.add(new Scalar("x", 3, 4.0));
	    expected_points.add(new Scalar("x", 4, 5.0));
	    
	    int index = 0;
        while (iterator.hasNext()){

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(5, index);
    }
    
    @Test
    public void testJLineGeneratorFactory2D() {
        JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

        PyList names = new PyList(Arrays.asList(new String[] {"X", "Y"}));
        PyList units = new PyList(Arrays.asList(new String[] {"mm", "mm"}));
        double[] start = {1.0, 2.0};
        double[] stop = {5.0, 10.0};
        
        @SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>) lineGeneratorFactory.createObject(
				names, units, start, stop, 5);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Point("X", 0, 1.0, "Y", 0, 2.0, false));
	    expected_points.add(new Point("X", 1, 2.0, "Y", 1, 4.0, false));
	    expected_points.add(new Point("X", 2, 3.0, "Y", 2, 6.0, false));
	    expected_points.add(new Point("X", 3, 4.0, "Y", 3, 8.0, false));
	    expected_points.add(new Point("X", 4, 5.0, "Y", 4, 10.0, false));
	    
	    int index = 0;
        while (iterator.hasNext()){  // Just get first few points

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(5, index);
    }
    
    @Test
    public void testJArrayGeneratorFactory() {
        JythonObjectFactory arrayGeneratorFactory = ScanPointGeneratorFactory.JArrayGeneratorFactory();
        
        double[] array_points = new double[] {1.0, 2.0, 3.0, 4.0, 5.0};

        @SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>) arrayGeneratorFactory.createObject(
				"x", "mm", array_points);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Scalar("x", 0, 1.0));
	    expected_points.add(new Scalar("x", 1, 2.0));
	    expected_points.add(new Scalar("x", 2, 3.0));
	    expected_points.add(new Scalar("x", 3, 4.0));
	    expected_points.add(new Scalar("x", 4, 5.0));
	    
	    int index = 0;
        while (iterator.hasNext()){

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(5, index);
    }
    
    @Test
    public void testJSpiralGeneratorFactory() {
        JythonObjectFactory spiralGeneratorFactory = ScanPointGeneratorFactory.JSpiralGeneratorFactory();

        PyList names = new PyList(Arrays.asList(new String[] {"X", "Y"}));
        PyList centre = new PyList(Arrays.asList(new Double[] {0.0, 0.0}));
        double radius = 1.5;
        double scale = 1.0;
        boolean alternate = false;
        
        @SuppressWarnings("unchecked")
        Iterator<IPosition> iterator = (Iterator<IPosition>)  spiralGeneratorFactory.createObject(
				names, "mm", centre, radius, scale, alternate);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Point("X", 0, 0.23663214944574582, "Y", 0, -0.3211855677650875, false));
	    expected_points.add(new Point("X", 1, -0.6440318266552169, "Y", 1, -0.25037538922751695, false));
	    expected_points.add(new Point("X", 2, -0.5596688286164636, "Y", 2, 0.6946549630820702, false));
	    expected_points.add(new Point("X", 3, 0.36066957248394327, "Y", 3, 0.9919687803189761, false));
	    expected_points.add(new Point("X", 4, 1.130650533568409, "Y", 4, 0.3924587351155914, false));
	    expected_points.add(new Point("X", 5, 1.18586065489788, "Y", 5, -0.5868891557832875, false));
	    
	    int index = 0;
	    while (iterator.hasNext() && index < 6){  // Just get first few points

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(6, index);
    }
    
    @Test
    public void testJLissajousGeneratorFactory() {
        JythonObjectFactory lissajousGeneratorFactory = ScanPointGeneratorFactory.JLissajousGeneratorFactory();

        PyDictionary box = new PyDictionary();
        box.put("width", 1.5);
        box.put("height", 1.5);
        box.put("centre", new double[] {0.0, 0.0});

        PyList names = new PyList(Arrays.asList(new String[] {"X", "Y"}));
        int numLobes = 2;
        int numPoints = 500;
        
        @SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>) lissajousGeneratorFactory.createObject(
				names, "mm", box, numLobes, numPoints);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Point("X", 0, 0.0, "Y", 0, 0.0, false));
	    expected_points.add(new Point("X", 1, 0.01884757158250311, "Y", 1, 0.028267637002450906, false));
	    expected_points.add(new Point("X", 2, 0.03768323863482717, "Y", 2, 0.05649510414594954, false));
	    expected_points.add(new Point("X", 3, 0.05649510414594954, "Y", 3, 0.08464228865511125, false));
	    expected_points.add(new Point("X", 4, 0.07527128613841116, "Y", 4, 0.1126691918405678, false));
	    expected_points.add(new Point("X", 5, 0.0939999251732282, "Y", 5, 0.14053598593929345, false));
	    
	    int index = 0;
        while (iterator.hasNext() && index < 6){  // Just get first few points

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(6, index);
    }
    
    @Test
    public void testJCompoundGeneratorFactoryWithRaster() {
		
		JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> outerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				"y", "mm", 0.0, 5.0, 2);
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> innerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				"x", "mm", 1.0, 3.0, 3, true);
		
		JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
        
        Object[] generators = {outerLine, innerLine};
        Object[] excluders = CompoundSpgIterator.getExcluders(Arrays.asList(new ScanRegion(new RectangularROI(0,0,5,5,0), Arrays.asList("x", "y"))));
        Object[] mutators = {};
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  compoundGeneratorFactory.createObject(
				generators, excluders, mutators);
        
        List<Object> expected_points = new ArrayList<Object>();
	    expected_points.add(new Point(0, 1.0, 0, 0.0));
	    expected_points.add(new Point(1, 2.0, 0, 0.0));
	    expected_points.add(new Point(2, 3.0, 0, 0.0));
	    expected_points.add(new Point(2, 3.0, 1, 5.0));
	    expected_points.add(new Point(1, 2.0, 1, 5.0));
	    expected_points.add(new Point(0, 1.0, 1, 5.0));
	    
	    int index = 0;
        while (iterator.hasNext()){

            Object point = iterator.next();
            assertEquals(expected_points.get(index), point);

        	index++;
        }
        assertEquals(6, index);
    }
    
    @Test
    public void testJCompoundGeneratorFactoryWithMutatedRaster() {
        JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> line1 = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				"y", "mm", 2.0, 10.0, 5);
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> line2 = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				"x", "mm", 1.0, 5.0, 5);
		
        JythonObjectFactory randomOffsetMutatorFactory = ScanPointGeneratorFactory.JRandomOffsetMutatorFactory();
		
        int seed = 10;
        
        PyList axes = new PyList(Arrays.asList(new String[] {"y", "x"}));

        PyDictionary maxOffset = new PyDictionary();
        maxOffset.put("x", 0.5);
        maxOffset.put("y", 0.5);
        
		PyObject randomOffset = (PyObject) randomOffsetMutatorFactory.createObject(seed, axes, maxOffset);
        
        JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
        
        Object[] generators = {line1, line2};
        Object[] excluders = CompoundSpgIterator.getExcluders(Arrays.asList(new ScanRegion(new RectangularROI(0,0,5,10,0), Arrays.asList("x", "y"))));
        Object[] mutators = {randomOffset};
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  compoundGeneratorFactory.createObject(
				generators, excluders, mutators);

		List<IPosition> vanilla_points = new ArrayList<IPosition>(10);
		vanilla_points.add(new Point("x", 0, 1.0, "y", 0, 2.0));
		vanilla_points.add(new Point("x", 1, 2.0, "y", 0, 2.0));
		vanilla_points.add(new Point("x", 2, 3.0, "y", 0, 2.0));
		vanilla_points.add(new Point("x", 3, 4.0, "y", 0, 2.0));
		vanilla_points.add(new Point("x", 4, 5.0, "y", 0, 2.0));
		vanilla_points.add(new Point("x", 0, 1.0, "y", 1, 4.0));
		vanilla_points.add(new Point("x", 1, 2.0, "y", 1, 4.0));
		vanilla_points.add(new Point("x", 2, 3.0, "y", 1, 4.0));
		vanilla_points.add(new Point("x", 3, 4.0, "y", 1, 4.0));
		vanilla_points.add(new Point("x", 4, 5.0, "y", 1, 4.0));
		Iterator<IPosition> itV = vanilla_points.iterator();
		while (iterator.hasNext() && itV.hasNext()) {
			IPosition point = iterator.next();
			IPosition vp = itV.next();
			assertEquals(vp.getIndices(), point.getIndices());
			assertEquals(vp.getNames(), point.getNames());
			for (String axis : vp.getNames()) {
				assertEquals(vp.getValue(axis), point.getValue(axis), (double) maxOffset.get(axis));
			}
		}
		assertFalse(itV.hasNext());
	}
}

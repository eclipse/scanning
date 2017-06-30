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
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.JythonArgument;
import org.eclipse.scanning.api.points.models.JythonArgument.JythonArgumentType;
import org.eclipse.scanning.api.points.models.JythonGeneratorModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;
import org.python.core.PyException;

/**
 * Tests the Jython point iterator by loading its scan points and 
 * 
 * @author Matthew Gerring
 *
 */
public class JythonGeneratorTest {
	
	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test
	public void generatorExists() throws Exception {

        JythonGeneratorModel model = new JythonGeneratorModel();
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        assertNotNull(gen);
	}
	
	@Test(expected=ValidationException.class)
	public void emptyModel() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}

	@Test(expected=ValidationException.class)
	public void modulelessModel() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        model.setPath("src/org/eclipse/scanning/test/points");
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}
	
	@Test(expected=ValidationException.class)
	public void classlessModel() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        model.setPath("src/org/eclipse/scanning/test/points");
        model.setModuleName("JythonGeneratorTest");
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}

	@Test(expected=PyException.class)
	public void badModule() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        model.setPath("src/org/eclipse/scanning/test/points");
        model.setModuleName("fred");
        model.setClassName("FixedValueGenerator");
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}
	
	@Test(expected=PyException.class)
	public void badClass() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        model.setPath("src/org/eclipse/scanning/test/points");
        model.setModuleName("JythonGeneratorTest");
        model.setClassName("fred");
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}

	@Test(expected=PyException.class)
	public void exceptionInGenerator() throws Exception {
		
        JythonGeneratorModel model = new JythonGeneratorModel();
        model.setPath("src/org/eclipse/scanning/test/points");
        model.setModuleName("JythonGeneratorTest");
        model.setClassName("ExceptionGenerator");
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        gen.size();
	}

	@Test
	public void testSize() throws Exception {

        JythonGeneratorModel model = createFixedValueModel("x", 10, Math.PI);
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        assertEquals(10, gen.size());
	}
	
	@Test
	public void testValue1() throws Exception {

        JythonGeneratorModel model = createFixedValueModel("p", 3, Math.PI);
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        List<IPosition> points = gen.createPoints();
        assertEquals(3, points.size());
        assertEquals(Math.PI, points.get(1).getValue("p"), 0.000001);
       
	}

	@Test
	public void testValue2() throws Exception {

        JythonGeneratorModel model = createMultipliedValueModel("m", 5, 10);
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        List<IPosition> points = gen.createPoints();
        assertEquals(5, points.size());
        assertEquals(0, points.get(0).getValue("m"), 0.000001);
        assertEquals(10, points.get(1).getValue("m"), 0.000001);
        assertEquals(20, points.get(2).getValue("m"), 0.000001);
        assertEquals(40, points.get(4).getValue("m"), 0.000001);
      
	}

	@Test
	public void mapPositionValue() throws Exception {

        JythonGeneratorModel model = createMapPositionModel("x", 5, 10, 3);
        IPointGenerator<JythonGeneratorModel> gen = service.createGenerator(model);
        List<IPosition> points = gen.createPoints();
        assertEquals(5, points.size());
        assertEquals(0, points.get(0).getValue("x0"), 0.000001);
        assertEquals(0, points.get(0).getValue("x1"), 0.000001);
        assertEquals(0, points.get(0).getValue("x2"), 0.000001);
      
        assertEquals(40, points.get(4).getValue("x0"), 0.000001);
        assertEquals(40, points.get(4).getValue("x1"), 0.000001);
        assertEquals(40, points.get(4).getValue("x2"), 0.000001);
        
        assertEquals(null, points.get(4).get("x5")); // No such scannable created

	}


	private JythonGeneratorModel createFixedValueModel(String scannableName, int size, double value) {
		JythonGeneratorModel model = new JythonGeneratorModel();
        model.setModuleName("JythonGeneratorTest");
        model.setClassName("FixedValueGenerator");
        model.setPath("src/org/eclipse/scanning/test/points");
        model.addArgument(new JythonArgument(scannableName,  JythonArgumentType.STRING));
        model.addArgument(new JythonArgument(String.valueOf(size), JythonArgumentType.INTEGER));
        model.addArgument(new JythonArgument(String.valueOf(value), JythonArgumentType.FLOAT));
        return model;
	}

	private JythonGeneratorModel createMultipliedValueModel(String scannableName, int size, double value) {
		JythonGeneratorModel model = new JythonGeneratorModel();
        model.setModuleName("JythonGeneratorTest");
        model.setClassName("MultipliedValueGenerator");
        model.setPath("src/org/eclipse/scanning/test/points");
        model.addArgument(new JythonArgument(scannableName,  JythonArgumentType.STRING));
        model.addArgument(new JythonArgument(String.valueOf(size), JythonArgumentType.INTEGER));
        model.addArgument(new JythonArgument(String.valueOf(value), JythonArgumentType.FLOAT));
        return model;
	}
	private JythonGeneratorModel createMapPositionModel(String scannableName, int size, double value, int nScannables) {
		JythonGeneratorModel model = new JythonGeneratorModel();
        model.setModuleName("JythonGeneratorTest");
        model.setClassName("MappedPositionGenerator");
        model.setPath("src/org/eclipse/scanning/test/points");
        model.addArgument(new JythonArgument(scannableName,  JythonArgumentType.STRING));
        model.addArgument(new JythonArgument(String.valueOf(size), JythonArgumentType.INTEGER));
        model.addArgument(new JythonArgument(String.valueOf(value), JythonArgumentType.FLOAT));
        model.addArgument(new JythonArgument(String.valueOf(nScannables), JythonArgumentType.INTEGER));
               return model;
	}

}

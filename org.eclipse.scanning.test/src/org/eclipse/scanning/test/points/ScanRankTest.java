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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

/**
 * Test different scan ranks after compounds are created.
 *
 * @author Matthew Gerring
 *
 */
public class ScanRankTest {

	private IPointGeneratorService service;

	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}

	@Test
	public void testRankLine1D() throws Exception {
		lineTest(0);
	}

	@Test
	public void testRankLine2D() throws Exception {
		lineTest(1);
	}

	@Test
	public void testRankLine3D() throws Exception {
		lineTest(2);
	}

	@Test
	public void testRankLine4D() throws Exception {
		lineTest(3);
	}

	@Test
	public void testRankLine5D() throws Exception {
		lineTest(4);
	}

	@Test
	public void testRankLine6D() throws Exception {
		lineTest(5);
	}

	@Test
	public void testRankLine7D() throws Exception {
		lineTest(6);
	}

	@Test
	public void testRankLine8D() throws Exception {
		lineTest(7);
	}

	@Test
	public void testRankLine9D() throws Exception {
		lineTest(8);
	}

	private void lineTest(int nestCount) throws Exception {

		LinearROI roi = new LinearROI(new double[]{0,0}, new double[]{3,3});

        OneDEqualSpacingModel model = new OneDEqualSpacingModel();
        model.setPoints(10);
        model.setFastAxisName("x");
        model.setSlowAxisName("y");

		// Get the point list
		IPointGenerator<?> gen = service.createGenerator(model, roi);

		IPointGenerator<?>[] gens = new IPointGenerator<?>[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			gens[i] = service.createGenerator(new StepModel("T"+(nestCount - 1 - i), 290, 300, 1));
		}
		gens[nestCount] = gen;
		gen = service.createCompoundGenerator(gens);

        checkOneGenerator(nestCount, gen);
	}

	@Test
	public void testRankSpiral1D() throws Exception {
		spiralTest(0);
	}

	@Test
	public void testRankSpiral2D() throws Exception {
		spiralTest(1);
	}

	@Test
	public void testRankSpiral3D() throws Exception {
		spiralTest(2);
	}

	@Test
	public void testRankSpiral4D() throws Exception {
		spiralTest(3);
	}

	@Test
	public void testRankSpiral5D() throws Exception {
		spiralTest(4);
	}

	@Test
	public void testRankSpiral6D() throws Exception {
		spiralTest(5);
	}

	@Test
	public void testRankSpiral7D() throws Exception {
		spiralTest(6);
	}

	@Test
	public void testRankSpiral8D() throws Exception {
		spiralTest(7);
	}

	@Test(expected=org.python.core.PyException.class)
	public void testScanLengthOver32BitRaisesPyException() throws Exception {
		spiralTest(8);
	}

	private void spiralTest(int nestCount) throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		SpiralModel model = new SpiralModel("x", "y");
		model.setBoundingBox(box);

		// Get the point list
		IPointGenerator<?> gen = service.createGenerator(model);

		IPointGenerator<?>[] gens = new IPointGenerator<?>[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			gens[i] = service.createGenerator(new StepModel("T"+(nestCount -1 -i), 290, 300, 1));
		}
		gens[nestCount] = gen;
		gen = service.createCompoundGenerator(gens);

        checkOneGenerator(nestCount, gen);

	}

	private void checkOneGenerator(int nestCount, IPointGenerator<?> gen)  throws Exception {

		int expectedScanRank = nestCount+1;

		int count=0;
        for (IPosition pos : gen) {
		    assertTrue("The ranks should be "+expectedScanRank+" but was "+pos.getScanRank()+" for "+pos, pos.getScanRank()==expectedScanRank);
		    for (int i = 0; i < nestCount; i++) {
			final Collection<String> names = ((AbstractPosition)pos).getDimensionNames(i);
			final Collection<String> expected = Arrays.asList("T"+(nestCount-1-i));
				assertTrue("Names are: "+names+" expected was: "+expected, expected.containsAll(names));
			}
		    if (nestCount>0) {
			    assertTrue(Arrays.asList("x", "y").containsAll(((AbstractPosition)pos).getDimensionNames(expectedScanRank-1)));
		    }

		    ++count;
		    if (count>100) break; // We just check the first few.
        }
	}

	@Test
	public void testRankGrid2D() throws Exception {
		gridTest(0);
	}
	@Test
	public void testRankGrid3D() throws Exception {
		gridTest(1);
	}
	@Test
	public void testRankGrid4D() throws Exception {
		gridTest(2);
	}
	@Test
	public void testRankGrid5D() throws Exception {
		gridTest(3);
	}
	@Test
	public void testRankGrid6D() throws Exception {
		gridTest(4);
	}
	@Test
	public void testRankGrid7D() throws Exception {
		gridTest(5);
	}
	@Test
	public void testRankGrid8D() throws Exception {
		gridTest(6);
	}

	@Test
	public void testRankGridWithCircularRegion2D() throws Exception {
		gridWithCircularRegionTest(0);
	}

	@Test
	public void testRankGridWithCircularRegion3D() throws Exception {
		gridWithCircularRegionTest(1);
	}

	@Test
	public void testRankGridWithCircularRegion4D() throws Exception {
		gridWithCircularRegionTest(2);
	}

	@Test
	public void testRankGridWithCircularRegion5D() throws Exception {
		gridWithCircularRegionTest(3);
	}

	@Test
	public void testRankGridWithCircularRegion6D() throws Exception {
		gridWithCircularRegionTest(4);
	}

	@Test
	public void testRankGridWithCircularRegion7D() throws Exception {
		gridWithCircularRegionTest(5);
	}

	@Test
	public void testRankGridWithCircularRegion8D() throws Exception {
		gridWithCircularRegionTest(6);
	}

	@Test
	public void testRankGridWithPolygonRegion2D() throws Exception {
		gridWithPolygonRegionTest(0);
	}

	@Test
	public void testRankGridWithPolygonRegion3D() throws Exception {
		gridWithPolygonRegionTest(1);
	}

	@Test
	public void testRankGridWithPolygonRegion4D() throws Exception {
		gridWithPolygonRegionTest(2);
	}

	@Test
	public void testRankGridWithPolygonRegion5D() throws Exception {
		gridWithPolygonRegionTest(3);
	}

	@Test
	public void testRankGridWithPolygonRegion6D() throws Exception {
		gridWithPolygonRegionTest(4);
	}

	@Test
	public void testRankGridWithPolygonRegion7D() throws Exception {
		gridWithPolygonRegionTest(5);
	}

	@Test
	public void testRankGridWithPolygonRegion8D() throws Exception {
		gridWithPolygonRegionTest(6);
	}

	private <R> IPointGenerator<?> createGridGenerator(int nestCount, R region) throws Exception {
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setBoundingBox(box);

		// Get the point list
		IPointGenerator<?> grid = service.createGenerator(model,
				region == null ? Collections.emptyList() : Arrays.asList(region));

		IPointGenerator<?>[] gens = new IPointGenerator<?>[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			gens[i] = service.createGenerator(new StepModel("T"+(nestCount -1 -i), 290, 300, 1));
		}
		gens[nestCount] = grid;

		return service.createCompoundGenerator(gens);
	}

	private void gridTest(int nestCount) throws Exception {
		IPointGenerator<?> gen = createGridGenerator(nestCount, null);
		final int[] gridShape = new int[] { 20, 20 };
		final int expectedSize = gridShape[0] * gridShape[1] * (int) Math.pow(11, nestCount);
		final int expectedScanRank = nestCount + 2;
		assertEquals(expectedSize, gen.size());
		assertEquals(expectedScanRank, gen.getRank());
		int[] expectedShape = Stream.concat(Collections.nCopies(nestCount, 11).stream(),
				Arrays.stream(gridShape).mapToObj(x -> new Integer(x))).mapToInt(Integer::valueOf).toArray();
		assertArrayEquals(expectedShape, gen.getShape());

		int count=0;
		for (IPosition pos : gen) {
			assertTrue("The ranks should be "+expectedScanRank+" but was "+pos.getScanRank()+" for "+pos, pos.getScanRank()==expectedScanRank);
			for (int i = 0; i < nestCount; i++) {
				final Collection<String> names = ((AbstractPosition)pos).getDimensionNames(i);
				final Collection<String> expected = Arrays.asList("T"+(nestCount-1-i));
				assertTrue("Names are: "+names+" expected was: "+expected, expected.containsAll(names));
			}
			if (nestCount>0) {
			assertEquals(Arrays.asList("y"), ((AbstractPosition) pos).getDimensionNames(expectedScanRank - 2));
			assertEquals(Arrays.asList("x"), ((AbstractPosition) pos).getDimensionNames(expectedScanRank - 1));
			}

			++count;
			if (count>100) break; // We just check the first few.
		}
	}

	private void gridWithCircularRegionTest(int nestCount) throws Exception {
		IROI region = new CircularROI(2, 1, 1);

		IPointGenerator<?> gen = createGridGenerator(nestCount, region);
		final int innerScanSize = 316;
		final int expectedSize = innerScanSize * (int) Math.pow(11, nestCount);
		final int expectedScanRank = nestCount+1;
		assertEquals(expectedSize, gen.size());
		assertEquals(expectedScanRank, gen.getRank());
		int[] expectedShape = Stream.concat(Collections.nCopies(nestCount, 11).stream(),
				Arrays.stream(new int[] { innerScanSize }).mapToObj(x -> new Integer(x)))
				.mapToInt(Integer::valueOf).toArray();
		assertArrayEquals(expectedShape, gen.getShape());

		int count=0;
	    for (IPosition pos : gen) {
		    assertTrue("The ranks should be "+expectedScanRank+" but was "+pos.getScanRank()+" for "+pos, pos.getScanRank()==expectedScanRank);
		    for (int i = 0; i < nestCount; i++) {
			final Collection<String> names = ((AbstractPosition)pos).getDimensionNames(i);
			final Collection<String> expected = Arrays.asList("T"+(nestCount-1-i));
				assertTrue("Names are: "+names+" expected was: "+expected, expected.containsAll(names));
			}
		    if (nestCount>0) {
			assertEquals(Arrays.asList("y", "x"), ((AbstractPosition) pos).getDimensionNames(expectedScanRank - 1));
		    }

		    ++count;
		    if (count>100) break; // We just check the first few.
	    }
	}

	private void gridWithPolygonRegionTest(int nestCount) throws Exception {
		PolygonalROI diamond = new PolygonalROI(new double[] { 1.5, 0 });
		diamond.insertPoint(new double[] { 3, 1.5 });
		diamond.insertPoint(new double[] { 1.5, 3 });
		diamond.insertPoint(new double[] { 0, 1.5 });

		IPointGenerator<?> gen = createGridGenerator(nestCount, diamond);
		final int innerScanSize = 194;
		final int expectedSize = innerScanSize * (int) Math.pow(11, nestCount);
		final int expectedScanRank = nestCount+1;
		assertEquals(expectedSize, gen.size());
		assertEquals(expectedScanRank, gen.getRank());
		int[] expectedShape = Stream.concat(Collections.nCopies(nestCount, 11).stream(),
				Arrays.stream(new int[] { innerScanSize }).mapToObj(x -> new Integer(x)))
				.mapToInt(Integer::valueOf).toArray();
		assertArrayEquals(expectedShape, gen.getShape());

		int count=0;
	    for (IPosition pos : gen) {
		    assertTrue("The ranks should be "+expectedScanRank+" but was "+pos.getScanRank()+" for "+pos, pos.getScanRank()==expectedScanRank);
		    for (int i = 0; i < nestCount; i++) {
			final Collection<String> names = ((AbstractPosition)pos).getDimensionNames(i);
			final Collection<String> expected = Arrays.asList("T"+(nestCount-1-i));
				assertTrue("Names are: "+names+" expected was: "+expected, expected.containsAll(names));
			}
		    if (nestCount>0) {
			assertEquals(Arrays.asList("y", "x"), ((AbstractPosition) pos).getDimensionNames(expectedScanRank - 1));
		    }

		    ++count;
		    if (count>100) break; // We just check the first few.
	    }
	}

}

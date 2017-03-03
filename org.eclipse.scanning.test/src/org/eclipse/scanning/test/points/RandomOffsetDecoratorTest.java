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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.points.RandomOffsetDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RandomOffsetDecoratorTest {

	private class MockIterator implements Iterator<IPosition> {

		IPosition next;

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			return false;
		}

		@Override
		public IPosition next() {
			if (next != null) {
				return next;
			}
			throw new NoSuchElementException();
		}
	}

	private static final long RANDOM_SEED = 0L;
	private static final double STD_DEV = 4.5;

	private MockIterator mockIterator;
	private RandomOffsetDecorator randomOffsetDecorator;

	@Before
	public void setUp() throws Exception {
		mockIterator = new MockIterator();
		randomOffsetDecorator = new RandomOffsetDecorator(mockIterator, STD_DEV);
		randomOffsetDecorator.setRandomSeed(RANDOM_SEED);
	}

	@After
	public void tearDown() throws Exception {
		randomOffsetDecorator = null;
		mockIterator = null;
	}

	@Test(expected = IllegalStateException.class)
	public void nonNumericPositionShouldThrowIllegalStateException() {
		mockIterator.next = new MapPosition("test_axis", 0, "String value");
		randomOffsetDecorator.next();
	}

	@Test
	public void testPointAtOrigin() {
		mockIterator.next = new Point(0, 0.0, 0, 0.0);

		Random random = new Random(RANDOM_SEED);
		double expectedY = random.nextGaussian() * STD_DEV;
		double expectedX = random.nextGaussian() * STD_DEV;
		IPosition expected = new Point(0, expectedX, 0, expectedY);

		IPosition actual = randomOffsetDecorator.next();
		assertEquals(expected, actual);
	}

	@Test
	public void testPointAwayFromOrigin() {
		final double x = -79.0;
		final double y = 43501.3;
		mockIterator.next = new Point(0, x, 0, y);

		Random random = new Random(RANDOM_SEED);
		double expectedY = y + random.nextGaussian() * STD_DEV;
		double expectedX = x + random.nextGaussian() * STD_DEV;
		IPosition expected = new Point(0, expectedX, 0, expectedY);

		IPosition actual = randomOffsetDecorator.next();
		assertEquals(expected, actual);
	}

	@Test
	public void testIndicesArePreserved() {
		final int xIndex = 5;
		final int yIndex = 28;
		mockIterator.next = new Point(xIndex, 0.0, yIndex, 0.0);

		Random random = new Random(RANDOM_SEED);
		double expectedY = random.nextGaussian() * STD_DEV;
		double expectedX = random.nextGaussian() * STD_DEV;
		IPosition expected = new Point(xIndex, expectedX, yIndex, expectedY);

		IPosition actual = randomOffsetDecorator.next();
		assertEquals(expected, actual);
	}

	@Test
	public void testOneDimensionalPosition() {
		final String name = "pos";
		final int index = 4;
		final double pos = -79.0;
		mockIterator.next = new MapPosition(name, index, pos);

		Random random = new Random(RANDOM_SEED);
		double expectedPos = pos + random.nextGaussian() * STD_DEV;
		IPosition expected = new MapPosition(name, index, expectedPos);

		IPosition actual = randomOffsetDecorator.next();
		assertEquals(expected, actual);
	}

	@Test
	public void testThreeDimensionalPosition() {
		final String name = "temp";
		final int tempIndex = 4;
		final double tempPosition = -79.0;
		final int xIndex = 5;
		final double xPosition = 0.0;
		final int yIndex = 28;
		final double yPosition = 541433.56234;
		final MapPosition position = new MapPosition(name, tempIndex, tempPosition);
		position.putAll(new Point(xIndex, xPosition, yIndex, yPosition));
		mockIterator.next = position;

		Random random = new Random(RANDOM_SEED);
		double expectedTemp = tempPosition + random.nextGaussian() * STD_DEV;
		double expectedY = yPosition + random.nextGaussian() * STD_DEV;
		double expectedX = xPosition + random.nextGaussian() * STD_DEV;
		MapPosition expected = new MapPosition(name, tempIndex, expectedTemp);
		expected.putAll(new Point(xIndex, expectedX, yIndex, expectedY));

		IPosition actual = randomOffsetDecorator.next();
		assertEquals(expected, actual);
	}
}
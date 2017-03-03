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
package org.eclipse.scanning.test.util;

import java.text.DecimalFormat;

public class DoubleUtils {

	/**
	 * Test if two numbers are equal.
	 * @param foo
	 * @param bar
	 * @param tolerance
	 * @return true if foo equals bar within tolerance
	 */
	public static boolean equalsWithinTolerance(Number foo, Number bar, Number tolerance) {
		final double a = foo.doubleValue();
		final double b = bar.doubleValue();
		final double t = tolerance.doubleValue();	
		return t>=Math.abs(a-b);
	}

	/**
	 * Test if two numbers are equal within an absolute or relative tolerance whichever is larger.
	 * The relative tolerance is given by a percentage and calculated from the absolute maximum of the input numbers.
	 * @param foo
	 * @param bar
	 * @param tolerance
	 * @param percentage
	 * @return true if foo equals bar within tolerance
	 */
	public static boolean equalsWithinTolerances(Number foo, Number bar, Number tolerance, Number percentage) {
		final double a = foo.doubleValue();
		final double b = bar.doubleValue();
		final double t = tolerance.doubleValue();
		final double p = percentage.doubleValue();

		double r = p * Math.max(Math.abs(a), Math.abs(b)) / 100.; // relative tolerance
		if (r > t)
			return r >= Math.abs(a - b);
			return t >= Math.abs(a - b);
	}

	/**
	 * Returns a formatted Double value given a specific DecimalFormat<br>
	 * If more than 4 integer, then we display the value in scientific notation
	 * @param value
	 * @param precision
	 * @return a double value formatted as a String
	 */
	public static String formatDouble(double value, int precision){
		String result;
		if(((int)value) > 9999 || ((int)value) < -9999){
			result = new DecimalFormat("0.######E0").format(value);
		}
		else
			result = String.valueOf(roundDouble(value, precision));
		return result == null ? "-" : result;
	}

	/**
	 * Method that rounds a value to the n precision decimals
	 * @param value
	 * @param precision
	 * @return a double value rounded to n precision
	 */
	public static double roundDouble(double value, int precision){
		int rounder = (int)Math.pow(10, precision);
		return (double)Math.round(value * rounder) / rounder;
	}

	public static void main(String[] args) {
		System.out.println(DoubleUtils.equalsWithinTolerance(10,11,2));
		System.out.println(DoubleUtils.equalsWithinTolerance(10,11,1));
		System.out.println(DoubleUtils.equalsWithinTolerance(10,10.9,1));
		System.out.println(DoubleUtils.equalsWithinTolerance(10.99,10.98,0.02));
		System.out.println(DoubleUtils.equalsWithinTolerance(10.99,10.97,0.02));
		System.out.println(DoubleUtils.equalsWithinTolerance(10.99,10.96,0.02));

		System.out.println(DoubleUtils.equalsWithinTolerances(10.99, 10.96, 0.02, 2.));
		System.out.println(DoubleUtils.equalsWithinTolerances(10.99, 10.96, 0.02, 0.1));

	}
}

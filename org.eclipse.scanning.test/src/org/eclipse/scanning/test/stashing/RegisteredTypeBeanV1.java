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
package org.eclipse.scanning.test.stashing;

public class RegisteredTypeBeanV1 {

	private int value1;
	private String value2;
	private double value3;
	
	public RegisteredTypeBeanV1() {
		value1 = 1;
		value2 = "2";
		value3 = 3.0d;
	}
	
	public int getValue1() {
		return value1;
	}
	public void setValue1(int value1) {
		this.value1 = value1;
	}
	public String getValue2() {
		return value2;
	}
	public void setValue2(String value2) {
		this.value2 = value2;
	}
	public double getValue3() {
		return value3;
	}
	public void setValue3(double value3) {
		this.value3 = value3;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value1;
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
		long temp;
		temp = Double.doubleToLongBits(value3);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegisteredTypeBeanV1 other = (RegisteredTypeBeanV1) obj;
		if (value1 != other.value1)
			return false;
		if (value2 == null) {
			if (other.value2 != null)
				return false;
		} else if (!value2.equals(other.value2))
			return false;
		if (Double.doubleToLongBits(value3) != Double.doubleToLongBits(other.value3))
			return false;
		return true;
	}
	
	
}

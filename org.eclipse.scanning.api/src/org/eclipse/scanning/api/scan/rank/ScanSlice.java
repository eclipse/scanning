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

import java.util.Arrays;

import org.eclipse.scanning.api.scan.rank.IScanSlice;

class ScanSlice implements IScanSlice {

	private int[] start;
	private int[] stop;
	private int[] step;
	
	public ScanSlice() {
		
	}
	public ScanSlice(int[] start, int[] stop, int[] step) {
		this.start = start;
		this.stop  = stop;
		this.step  = step;
	}
	public int[] getStart() {
		return start;
	}
	public void setStart(int[] start) {
		this.start = start;
	}
	public int[] getStop() {
		return stop;
	}
	public void setStop(int[] stop) {
		this.stop = stop;
	}
	public int[] getStep() {
		return step;
	}
	public void setStep(int[] step) {
		this.step = step;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(start);
		result = prime * result + Arrays.hashCode(step);
		result = prime * result + Arrays.hashCode(stop);
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
		ScanSlice other = (ScanSlice) obj;
		if (!Arrays.equals(start, other.start))
			return false;
		if (!Arrays.equals(step, other.step))
			return false;
		if (!Arrays.equals(stop, other.stop))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ScanSlice [start=" + Arrays.toString(start) + ", stop=" + Arrays.toString(stop) + ", step="
				+ Arrays.toString(step) + "]";
	}
}

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
package org.eclipse.scanning.test.scan.mock;

import org.eclipse.scanning.api.device.models.IDetectorModel;


public class MockDetectorModel implements IDetectorModel {

	public MockDetectorModel() {
	
	}
	
	public MockDetectorModel(double exposureTime) {
		super();
		this.exposureTime = exposureTime;
	}

	private double exposureTime;
	
	private int ran=0;
	private int written=0;
	private int abortCount=-1;
	private String name;
    private boolean createImage = true;
    private int[] imageSize = new int[]{64, 64};
	
	public double getExposureTime() {
		return exposureTime;
	}

	public void setExposureTime(double exposureTime) {
		this.exposureTime = exposureTime;
	}

	public int getRan() {
		return ran;
	}

	public void setRan(int ran) {
		this.ran = ran;
	}

	public int getWritten() {
		return written;
	}

	public void setWritten(int read) {
		this.written = read;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + abortCount;
		result = prime * result + (createImage ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(exposureTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ran;
		result = prime * result + written;
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
		MockDetectorModel other = (MockDetectorModel) obj;
		if (abortCount != other.abortCount)
			return false;
		if (createImage != other.createImage)
			return false;
		if (Double.doubleToLongBits(exposureTime) != Double.doubleToLongBits(other.exposureTime))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (ran != other.ran)
			return false;
		if (written != other.written)
			return false;
		return true;
	}

	public int getAbortCount() {
		return abortCount;
	}

	public void setAbortCount(int abortCount) {
		this.abortCount = abortCount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isCreateImage() {
		return createImage;
	}

	public void setCreateImage(boolean createImage) {
		this.createImage = createImage;
	}

	public int[] getImageSize() {
		return imageSize;
	}

	public void setImageSize(int[] imageSize) {
		this.imageSize = imageSize;
	}

}

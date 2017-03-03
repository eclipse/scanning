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
package org.eclipse.scanning.api.scan;

import java.io.Serializable;

import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

public class AxisConfiguration implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3907146006857647918L;

	@FieldDescriptor(file=FileType.EXISTING_FILE, 
			         hint="The microscope image to load into the scan as a background.", 
			         fieldPosition=-2)
	private String microscopeImage;
	
	@FieldDescriptor(label="Random", hint="If there is no image, create some random noise for one instead.", fieldPosition=-1, enableif="microscopeImage==null")
	private boolean randomNoise = false;

	public boolean isRandomNoise() {
		return randomNoise;
	}
	public void setRandomNoise(boolean randomNoise) {
		this.randomNoise = randomNoise;
	}
	public String getMicroscopeImage() {
		return microscopeImage;
	}
	public void setMicroscopeImage(String microscopeImage) {
		this.microscopeImage = microscopeImage;
	}
	@FieldDescriptor(device=DeviceType.SCANNABLE, hint="The name of a fast motor, for instance that used for the x-stage.", fieldPosition=0)
	private String fastAxisName;
	
	@FieldDescriptor(scannable="fastAxisName", fieldPosition=1)
	private double fastAxisStart;
	
	@FieldDescriptor(scannable="fastAxisName", fieldPosition=2)
	private double fastAxisEnd;
	
	@FieldDescriptor(device=DeviceType.SCANNABLE, hint="The name of a slow or other motor, for instance that used for the y-stage.", fieldPosition=3)
	private String slowAxisName;
	
	@FieldDescriptor(scannable="slowAxisName", fieldPosition=4)
	private double slowAxisStart;
	
	@FieldDescriptor(scannable="slowAxisName", fieldPosition=5)
	private double slowAxisEnd;
	
	@FieldDescriptor(label="Apply to Regions", hint="Find any scan regions and set their axis names to those on this form.", fieldPosition=6)
	private boolean applyRegions = false;

	@FieldDescriptor(label="Apply to Scan", hint="Find any scan models and set their axis names to those on this form.", fieldPosition=7)
	private boolean applyModels = false;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (applyModels ? 1231 : 1237);
		result = prime * result + (applyRegions ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(fastAxisEnd);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((fastAxisName == null) ? 0 : fastAxisName.hashCode());
		temp = Double.doubleToLongBits(fastAxisStart);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((microscopeImage == null) ? 0 : microscopeImage.hashCode());
		result = prime * result + (randomNoise ? 1231 : 1237);
		temp = Double.doubleToLongBits(slowAxisEnd);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((slowAxisName == null) ? 0 : slowAxisName.hashCode());
		temp = Double.doubleToLongBits(slowAxisStart);
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
		AxisConfiguration other = (AxisConfiguration) obj;
		if (applyModels != other.applyModels)
			return false;
		if (applyRegions != other.applyRegions)
			return false;
		if (Double.doubleToLongBits(fastAxisEnd) != Double.doubleToLongBits(other.fastAxisEnd))
			return false;
		if (fastAxisName == null) {
			if (other.fastAxisName != null)
				return false;
		} else if (!fastAxisName.equals(other.fastAxisName))
			return false;
		if (Double.doubleToLongBits(fastAxisStart) != Double.doubleToLongBits(other.fastAxisStart))
			return false;
		if (microscopeImage == null) {
			if (other.microscopeImage != null)
				return false;
		} else if (!microscopeImage.equals(other.microscopeImage))
			return false;
		if (randomNoise != other.randomNoise)
			return false;
		if (Double.doubleToLongBits(slowAxisEnd) != Double.doubleToLongBits(other.slowAxisEnd))
			return false;
		if (slowAxisName == null) {
			if (other.slowAxisName != null)
				return false;
		} else if (!slowAxisName.equals(other.slowAxisName))
			return false;
		if (Double.doubleToLongBits(slowAxisStart) != Double.doubleToLongBits(other.slowAxisStart))
			return false;
		return true;
	}
	public String getFastAxisName() {
		return fastAxisName;
	}
	public void setFastAxisName(String fastAxisName) {
		this.fastAxisName = fastAxisName;
	}
	public double getFastAxisStart() {
		return fastAxisStart;
	}
	public void setFastAxisStart(double fastAxisStart) {
		this.fastAxisStart = fastAxisStart;
	}
	public double getFastAxisEnd() {
		return fastAxisEnd;
	}
	public void setFastAxisEnd(double fastAxisEnd) {
		this.fastAxisEnd = fastAxisEnd;
	}
	public String getSlowAxisName() {
		return slowAxisName;
	}
	public void setSlowAxisName(String slowAxisName) {
		this.slowAxisName = slowAxisName;
	}
	public double getSlowAxisStart() {
		return slowAxisStart;
	}
	public void setSlowAxisStart(double slowAxisStart) {
		this.slowAxisStart = slowAxisStart;
	}
	public double getSlowAxisEnd() {
		return slowAxisEnd;
	}
	public void setSlowAxisEnd(double slowAxisEnd) {
		this.slowAxisEnd = slowAxisEnd;
	}
	
	public boolean isApplyRegions() {
		return applyRegions;
	}
	public void setApplyRegions(boolean applyRegions) {
		this.applyRegions = applyRegions;
	}
	public boolean isApplyModels() {
		return applyModels;
	}
	public void setApplyModels(boolean applyModels) {
		this.applyModels = applyModels;
	}

}

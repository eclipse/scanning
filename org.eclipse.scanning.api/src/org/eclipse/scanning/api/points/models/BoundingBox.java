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
package org.eclipse.scanning.api.points.models;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

/**
 * A model defining a box in two dimensional space, which can be used to confine and give scale to a {@link
 * IBoundingBoxModel}.
 * <p>
 * The two axes of the box are abstracted as "fast" and "slow". Often these will be the X and Y stage motors, but other
 * axes could be used depending on the beamline configuration or the required experiment. The axis names to be used are
 * defined in AbstractBoundingBoxModel.
 *
 * Important difference between BoundingBox and IRectangularROI - rois are in data coordinates and bounding boxes are
 * in axis coordinates i.e. locations of the motors rather than the selection of the data.
 *
 * @author Colin Palmer
 * @author Matthew Gerring
 *
 * @Deprecated Replaced by ScanRegion which is provided with the CompoundModel
 */
public class BoundingBox  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3775847793520017725L;

	@FieldDescriptor(editable=false, fieldPosition=0)
	private String fastAxisName="stage_x";
	
	@FieldDescriptor(scannable="fastAxisName", fieldPosition=1)
	private double fastAxisStart;
	
	@FieldDescriptor(scannable="fastAxisName", validif="fastAxisLength!=0", fieldPosition=2)
	private double fastAxisLength;
	
	@FieldDescriptor(editable=false, fieldPosition=3)
	private String slowAxisName="stage_y";

	@FieldDescriptor(scannable="slowAxisName", fieldPosition=4)
	private double slowAxisStart;
	
	@FieldDescriptor(scannable="slowAxisName", validif="slowAxisLength!=0", fieldPosition=5)
	private double slowAxisLength;
	
	@FieldDescriptor(visible=false, hint="Provides information about the visible region we are linked to.")
	private String regionName;


	public BoundingBox() {
		
	}

	public BoundingBox(String fName, String sName) {
		this.fastAxisName = fName;
		this.slowAxisName = sName;
	}
	
	public BoundingBox(double fastAxisStart, double slowAxisStart, double fastAxisLength, double slowAxisLength) {
		super();
		this.fastAxisStart = fastAxisStart;
		this.slowAxisStart = slowAxisStart;
		this.fastAxisLength = fastAxisLength;
		this.slowAxisLength = slowAxisLength;
	}

	/**
	 * 
	 * @param spt [fastStart, slowStart]
	 * @param ept [fastEnd, slowEnd]
	 */
	public BoundingBox(double[] spt, double[] ept) {
		
		double[] len = new double[2];
		double lx = ept[0] - spt[0];
		double ly = ept[1] - spt[1];
		@SuppressWarnings("unused")
		double ang = 0d; // TODO should be used?
		if (lx > 0) {
			if (ly > 0) {
				len[0] = lx;
				len[1] = ly;
				ang = 0;
			} else {
				len[0] = lx;
				len[1] = -ly;
				ang = Math.PI * 1.5;
			}
		} else {
			if (ly > 0) {
				len[0] = -lx;
				len[1] = ly;
				ang = Math.PI * 0.5;
			} else {
				len[0] = -lx;
				len[1] = -ly;
				ang = Math.PI;
			}
		}

		fastAxisStart  = spt[0];
		fastAxisLength = len[0];
		slowAxisStart  = spt[1];
		slowAxisLength = len[1];
	}

	public double getFastAxisStart() {
		return fastAxisStart;
	}
	public void setFastAxisStart(double fastAxisStart) {
		this.fastAxisStart = fastAxisStart;
	}
	public double getSlowAxisStart() {
		return slowAxisStart;
	}
	public void setSlowAxisStart(double yStart) {
		this.slowAxisStart = yStart;
	}
	public double getFastAxisLength() {
		return fastAxisLength;
	}
	public void setFastAxisLength(double fastAxisLength) {
		this.fastAxisLength = fastAxisLength;
	}
	public double getSlowAxisLength() {
		return slowAxisLength;
	}
	public void setSlowAxisLength(double slowAxisLength) {
		this.slowAxisLength = slowAxisLength;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(slowAxisLength);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(fastAxisLength);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(fastAxisStart);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		BoundingBox other = (BoundingBox) obj;
		if (Double.doubleToLongBits(slowAxisLength) != Double
				.doubleToLongBits(other.slowAxisLength))
			return false;
		if (Double.doubleToLongBits(fastAxisLength) != Double
				.doubleToLongBits(other.fastAxisLength))
			return false;
		if (Double.doubleToLongBits(fastAxisStart) != Double
				.doubleToLongBits(other.fastAxisStart))
			return false;
		if (Double.doubleToLongBits(slowAxisStart) != Double
				.doubleToLongBits(other.slowAxisStart))
			return false;
		return true;
	}

	public String getFastAxisName() {
		return fastAxisName;
	}

	public void setFastAxisName(String fastAxisName) {
		this.fastAxisName = fastAxisName;
	}

	public String getSlowAxisName() {
		return slowAxisName;
	}

	public void setSlowAxisName(String slowAxisName) {
		this.slowAxisName = slowAxisName;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public double getSlowAxisEnd() {
		return getSlowAxisStart()+getSlowAxisLength();
	}
	
	public double getFastAxisEnd() {
		return getFastAxisStart()+getFastAxisLength();
	}

	@Override
	public String toString() {
		return "Start="+toString(getStart())+" length="+toString(getLength());
	}

	private double[] getStart() {
		return new double[]{fastAxisStart, slowAxisStart};
	}
	private double[] getLength() {
		return new double[]{fastAxisLength, slowAxisLength};
	}
	
    private String toString(double[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(format.format(a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }
    
    private DecimalFormat format = new DecimalFormat("##########0.0###");
    public void setNumberFormat(String sformat) {
    	format = new DecimalFormat(sformat);
    }

}

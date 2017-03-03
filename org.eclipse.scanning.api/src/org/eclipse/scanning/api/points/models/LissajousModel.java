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

public class LissajousModel extends AbstractBoundingBoxModel {

	private double a = 1;
	private double b = 0.25;
	private double delta = 0;
	private double thetaStep = 0.05;
	private int points = 503; // this gives a closed path with the other default values

	public LissajousModel() {
		setName("Lissajous Curve");
	}
	
	public double getA() {
		return a;
	}
	public void setA(double a) {
		double oldValue = this.a;
		this.a = a;
		this.pcs.firePropertyChange("a", oldValue, a);
	}
	public double getB() {
		return b;
	}
	public void setB(double b) {
		double oldValue = this.b;
		this.b = b;
		this.pcs.firePropertyChange("b", oldValue, b);
	}
	public double getDelta() {
		return delta;
	}
	public void setDelta(double delta) {
		double oldValue = this.delta;
		this.delta = delta;
		this.pcs.firePropertyChange("delta", oldValue, delta);
	}
	public double getThetaStep() {
		return thetaStep;
	}
	public void setThetaStep(double thetaStep) {
		double oldValue = this.thetaStep;
		this.thetaStep = thetaStep;
		this.pcs.firePropertyChange("thetaStep", oldValue, thetaStep);
	}
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		int oldValue = this.points;
		this.points = points;
		this.pcs.firePropertyChange("points", oldValue, points);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(a);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(b);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(delta);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + points;
		temp = Double.doubleToLongBits(thetaStep);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LissajousModel other = (LissajousModel) obj;
		if (Double.doubleToLongBits(a) != Double.doubleToLongBits(other.a))
			return false;
		if (Double.doubleToLongBits(b) != Double.doubleToLongBits(other.b))
			return false;
		if (Double.doubleToLongBits(delta) != Double.doubleToLongBits(other.delta))
			return false;
		if (points != other.points)
			return false;
		if (Double.doubleToLongBits(thetaStep) != Double.doubleToLongBits(other.thetaStep))
			return false;
		return true;
	}
}

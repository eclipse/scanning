/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.example.xcen.beans;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.dataset.roi.json.GridROIBean;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * Bean to serialise with JSON and be sent to the server.
 * 
 * JSON is used rather than the direct object because we may want to have
 * a python server.
 * 
 * @author Matthew Gerring
 *
 */
public class XcenBean extends StatusBean {

	// TODO More fields for doing the centering.
	private String      beamline;  // visitId, same as ISPyB definition
	private String      visit;  // visitId, same as ISPyB definition
	private String      collection; // DatacollectionId, same as ISPyB definition
	private double      x,y,z;
	private GridROIBean[] grids;
	
	@Override
	public void merge(StatusBean with) {
        super.merge(with);
        XcenBean db     = (XcenBean)with;
        this.beamline   = db.beamline;
        this.visit      = db.visit;
        this.collection = db.collection;
        this.grids      = db.grids;
        this.x          = db.x;
        this.y          = db.y;
        this.z          = db.z;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((beamline == null) ? 0 : beamline.hashCode());
		result = prime * result
				+ ((collection == null) ? 0 : collection.hashCode());
		result = prime * result + Arrays.hashCode(grids);
		result = prime * result + ((visit == null) ? 0 : visit.hashCode());
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
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
		XcenBean other = (XcenBean) obj;
		if (beamline == null) {
			if (other.beamline != null)
				return false;
		} else if (!beamline.equals(other.beamline))
			return false;
		if (collection == null) {
			if (other.collection != null)
				return false;
		} else if (!collection.equals(other.collection))
			return false;
		if (!Arrays.equals(grids, other.grids))
			return false;
		if (visit == null) {
			if (other.visit != null)
				return false;
		} else if (!visit.equals(other.visit))
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public GridROIBean[] getGrids() {
		return grids;
	}

	public void setGrids(GridROIBean... grids) {
		this.grids = grids;
	}
	
	public void addGrid(GridROIBean grid) {
		if (grid== null) throw new NullPointerException("Cannot add null grid!");
		if (grids==null) {
			grids = new GridROIBean[]{grid};
			return;
		}
		GridROIBean[] tmp = new GridROIBean[grids.length+1];
		System.arraycopy(grids, 0, tmp, 0, grids.length);
		tmp[tmp.length-1] = grid;
		grids = tmp;
	}

	public String getVisit() {
		return visit;
	}

	public void setVisit(String visit) {
		this.visit = visit;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getBeamline() {
		return beamline;
	}

	public void setBeamline(String beamline) {
		this.beamline = beamline;
	}

}

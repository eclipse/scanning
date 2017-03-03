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
package org.eclipse.scanning.example.malcolm;

import java.util.Arrays;

/**
 * A model describing a dataset that should be written by a {@link DummyMalcolmDevice}.
 * 
 * @author Matthew Dickie
 */
public class DummyMalcolmDatasetModel {
	
	private String name;
	
	private Class<?> dtype; // type of element in the dataset, e.g. String or Double
	
	private int rank;
	
	private int[] shape = null;
	
	public DummyMalcolmDatasetModel() {
		// no args constructor for spring instantiation
	}
	
	public DummyMalcolmDatasetModel(String name, int rank, Class<?> dtype) {
		this.name = name;
		this.rank = rank;
		this.dtype = dtype;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The rank of the data for this dataset at each point in the scan, e.g. 2 for images.
	 * In the Nexus file this dataset will have a rank of scan rank plus this value. 
	 * @return rank of data at each scan point
	 */
	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public Class<?> getDtype() {
		return dtype;
	}

	public void setDtype(Class<?> dtype) {
		this.dtype = dtype;
	}
	
	public void setShape(int[] shape) {
		if (shape.length != rank) {
			throw new IllegalArgumentException("size of shape array must equal rank " + rank);
		}
		
		this.shape = shape;
	}
	
	public int[] getShape() {
		return shape;
	}

	@Override
	public String toString() {
		return "DummyMalcolmDatasetModel [name=" + name + ", dtype=" + dtype + ", rank=" + rank + ", shape="
				+ Arrays.toString(shape) + "]";
	}

}

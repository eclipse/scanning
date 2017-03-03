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
package org.eclipse.scanning.api.device.models;

import java.util.List;

import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

/**
 * The model for a malcolm device that writes h5 files.
 */
public class MalcolmModel extends AbstractDetectorModel implements IMalcolmModel {

	public MalcolmModel() {
		setTimeout(60*24); // 1 Day
	}
	/**
	 * The folder for malcolm to create its HDF5 files in. This is set by the scan, any value
	 * set by the user will be overwritten. 
	 */
	@FieldDescriptor(visible=false, file=FileType.NEW_FOLDER)
	private String fileDir;
	
	@FieldDescriptor(editable=false)
	private List<String> axesToMove;
	
	public String getFileDir() {
		return fileDir;
	}

	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	@Override
	public List<String> getAxesToMove() {
		return axesToMove;
	}
	
	public void setAxesToMove(List<String> axesToMove) {
		this.axesToMove = axesToMove;
	}
}

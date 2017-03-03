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

import java.util.List;

import org.eclipse.dawnsci.analysis.api.processing.IOperationBean;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * An implementation of {@link IOperationBean} for testing purposes.
 * @author Matthew Dickie
 */
public class DummyOperationBean extends StatusBean implements IOperationBean {

	private static final long serialVersionUID = 1L;
	
	private String dataKey;
	private String filePath;
	private String outputFilePath;
	private String datasetPath;
	private String slicing;
	private String processingPath;
	private List<String>[] axesNames;
	private boolean deleteProcessingFile;
	private String xmx;
	private int[] dataDimensions;
	private Integer scanRank;
	private boolean readable;
	private String name;
	private String runDirectory;
	private int numberOfCores;
	
	public String getDataKey() {
		return dataKey;
	}
	public void setDataKey(String dataKey) {
		this.dataKey = dataKey;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getOutputFilePath() {
		return outputFilePath;
	}
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}
	public String getDatasetPath() {
		return datasetPath;
	}
	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}
	public String getSlicing() {
		return slicing;
	}
	public void setSlicing(String slicing) {
		this.slicing = slicing;
	}
	public String getProcessingPath() {
		return processingPath;
	}
	public void setProcessingPath(String processingPath) {
		this.processingPath = processingPath;
	}
	public List<String>[] getAxesNames() {
		return axesNames;
	}
	public void setAxesNames(List<String>[] axesNames) {
		this.axesNames = axesNames;
	}
	public boolean isDeleteProcessingFile() {
		return deleteProcessingFile;
	}
	public void setDeleteProcessingFile(boolean deleteProcessingFile) {
		this.deleteProcessingFile = deleteProcessingFile;
	}
	public String getXmx() {
		return xmx;
	}
	public void setXmx(String xmx) {
		this.xmx = xmx;
	}
	public int[] getDataDimensions() {
		return dataDimensions;
	}
	public void setDataDimensions(int[] dataDimensions) {
		this.dataDimensions = dataDimensions;
	}
	public Integer getScanRank() {
		return scanRank;
	}
	public void setScanRank(Integer scanRank) {
		this.scanRank = scanRank;
	}
	public boolean isReadable() {
		return readable;
	}
	public void setReadable(boolean readable) {
		this.readable = readable;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRunDirectory() {
		return runDirectory;
	}
	public void setRunDirectory(String runDirectory) {
		this.runDirectory = runDirectory;
	}
	public int getNumberOfCores() {
		return numberOfCores;
	}
	public void setNumberOfCores(int numberOfCores) {
		this.numberOfCores = numberOfCores;
	}

}

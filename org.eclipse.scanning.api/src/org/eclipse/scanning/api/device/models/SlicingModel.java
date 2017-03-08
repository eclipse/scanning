package org.eclipse.scanning.api.device.models;

import org.eclipse.scanning.api.ITimeoutable;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

public class SlicingModel implements ITimeoutable {
	
	@FieldDescriptor(minimum=1, hint="The rank of the data we will slice.")
	private int dataRank = 2;

	@FieldDescriptor(file=FileType.EXISTING_FILE)
	private String   dataFile;
	
	/**
	 * The name of the detector whose output we will be processing
	 * 
	 * This is used to figure out which part of the nexus file
	 * to look at when processing.
	 */
	@FieldDescriptor(device=DeviceType.RUNNABLE, hint="The name of the detector whose output we will get.")
	private String detectorName;
	
	@FieldDescriptor(editable=false, hint="The unique device name.")
	private String name;
	
	@FieldDescriptor(editable=true, hint="The timeout of each data point during the scan in seconds.")
	private long timeout = -1;

	public SlicingModel() {
		
	}

	public SlicingModel(String detectorName, String dataFile, long timeout) {
		this.detectorName = detectorName;
		this.dataFile     = dataFile;
		this.timeout      = timeout;
	}


	public String getDataFile() {
		return dataFile;
	}


	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}


	public String getDetectorName() {
		return detectorName;
	}


	public void setDetectorName(String detectorName) {
		this.detectorName = detectorName;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public long getTimeout() {
		return timeout;
	}


	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataFile == null) ? 0 : dataFile.hashCode());
		result = prime * result + dataRank;
		result = prime * result + ((detectorName == null) ? 0 : detectorName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (timeout ^ (timeout >>> 32));
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
		SlicingModel other = (SlicingModel) obj;
		if (dataFile == null) {
			if (other.dataFile != null)
				return false;
		} else if (!dataFile.equals(other.dataFile))
			return false;
		if (dataRank != other.dataRank)
			return false;
		if (detectorName == null) {
			if (other.detectorName != null)
				return false;
		} else if (!detectorName.equals(other.detectorName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (timeout != other.timeout)
			return false;
		return true;
	}

	public int getDataRank() {
		return dataRank;
	}

	public void setDataRank(int dataRank) {
		this.dataRank = dataRank;
	}

}

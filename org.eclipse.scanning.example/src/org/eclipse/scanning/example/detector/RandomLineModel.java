package org.eclipse.scanning.example.detector;

import org.eclipse.scanning.api.device.models.IDetectorModel;

public class RandomLineModel implements IDetectorModel {

	private String name="line";
	private double exposureTime=0.001;
	private long timeout=-1;
	private int lineSize=32;

	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public double getExposureTime() {
		return exposureTime;
	}
	@Override
	public void setExposureTime(double exposureTime) {
		this.exposureTime = exposureTime;
	}
	@Override
	public long getTimeout() {
		return timeout;
	}
	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	public int getLineSize() {
		return lineSize;
	}
	public void setLineSize(int lineSize) {
		this.lineSize = lineSize;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(exposureTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + lineSize;
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
		RandomLineModel other = (RandomLineModel) obj;
		if (Double.doubleToLongBits(exposureTime) != Double.doubleToLongBits(other.exposureTime))
			return false;
		if (lineSize != other.lineSize)
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

}

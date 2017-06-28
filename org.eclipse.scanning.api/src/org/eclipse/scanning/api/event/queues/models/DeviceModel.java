package org.eclipse.scanning.api.event.queues.models;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;

public class DeviceModel {
	
	private String type;
	private List<IQueueValue<?>> deviceConfiguration;
	private List<DeviceModel> roiConfiguration;
	
	public DeviceModel(String type, List<IQueueValue<?>> deviceConfiguration) {
		this(type, deviceConfiguration, new ArrayList<>());
	}
	
	public DeviceModel(String type, List<IQueueValue<?>> deviceConfiguration, List<DeviceModel> roiConfiguration) {
		this.type = type;
		this.deviceConfiguration = deviceConfiguration;
		this.roiConfiguration = roiConfiguration;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public List<IQueueValue<?>> getDeviceConfiguration() {
		return deviceConfiguration;
	}
	
	public void setDeviceConfiguration(List<IQueueValue<?>> deviceConfiguration) {
		this.deviceConfiguration = deviceConfiguration;
	}

	public List<DeviceModel> getRoiConfiguration() {
		return roiConfiguration;
	}

	public void setRoiConfiguration(List<DeviceModel> roiConfiguration) {
		this.roiConfiguration = roiConfiguration;
	}

	public IQueueValue<?> getDeviceModelValue(IQueueValue<String> valueReference) throws QueueModelException {
		Optional<IQueueValue<?>> value = deviceConfiguration.stream().filter(option -> valueReference.isReferenceFor(option)).findFirst();
		try {
			return value.get();
		} catch (NoSuchElementException nseEX) {
			throw new QueueModelException("No value in deviceConfiguration for reference '"+valueReference.evaluate()+"'");
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((deviceConfiguration == null) ? 0 : deviceConfiguration.hashCode());
		result = prime * result + ((roiConfiguration == null) ? 0 : roiConfiguration.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DeviceModel other = (DeviceModel) obj;
		if (deviceConfiguration == null) {
			if (other.deviceConfiguration != null)
				return false;
		} else if (!deviceConfiguration.equals(other.deviceConfiguration))
			return false;
		if (roiConfiguration == null) {
			if (other.roiConfiguration != null)
				return false;
		} else if (!roiConfiguration.equals(other.roiConfiguration))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	@Override
	public String toString() {
		String configString = deviceConfiguration.stream().map(option -> option.getName() + "="+option.evaluate().toString()).collect(Collectors.joining(", "));
		String fullString = "DeviceModel [" + type + ": deviceConfiguration={" + configString + "}"; 
		if (roiConfiguration.size() > 0) {
			String roiConfigString = roiConfiguration.stream().map(DeviceModel::toString).collect(Collectors.joining(", "));
			fullString = fullString + "; roiConfiguration={" + roiConfigString + "}";
		}
		return fullString + "]";
	}
	

}

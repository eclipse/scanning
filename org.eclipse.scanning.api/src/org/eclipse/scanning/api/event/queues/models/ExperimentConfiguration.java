package org.eclipse.scanning.api.event.queues.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

/**
 * Data Transfer Object for transferring all information recovered from the 
 * database by the {@link IQueueSpoolerService} to the 
 * {@link IQueueBeanFactory} so that it can be used during the assembly of 
 * queue beans & atoms.
 * 
 * @author Michael Wharmby
 *
 */
public class ExperimentConfiguration {
	
	private final List<IQueueValue<?>> localValues;
	private final Map<String, DeviceModel> detectorModelValues, pathModelValues;
	
	/**
	 * Create a new {@link ExperimentConfiguration} object configured with 
	 * detector & path information as well as more general information.
	 * @param localValues List of {@link IQueueValue} for general variables
	 * @param detectorModelValues Map of detector name String and List of 
	 *        {@link IQueueValue} to configure each detector
	 * @param pathModelValues Map of path type name String and List of 
	 *        {@link IQueueValue} to configure each scan path
	 */
	public ExperimentConfiguration(List<IQueueValue<?>> localValues, Map<String, DeviceModel> pathModelValues, Map<String, DeviceModel> detectorModelValues) {
		if (localValues == null) this.localValues = new ArrayList<>();
		else this.localValues = new ArrayList<>(localValues);
		
		if (detectorModelValues == null) this.detectorModelValues = new HashMap<>();
		else this.detectorModelValues = new HashMap<>(detectorModelValues);
		
		if (pathModelValues == null) this.pathModelValues = new HashMap<>();
		else this.pathModelValues = new HashMap<>(pathModelValues);
	}
	
	public List<IQueueValue<?>> getLocalValues() {
		return localValues;
	}
	
	public Map<String, DeviceModel> getDetectorModelValues() {
		return detectorModelValues;
	}
	
	public Map<String, DeviceModel> getPathModelValues() {
		return pathModelValues;
	}
	
	/**
	 * Identify whether a value corresponding to valueReference is in the 
	 * localValues and return it.
	 * @param valueReference {@link QueueValue} containing reference to find
	 * @return {@link IQueueValue} referred to
	 * @throws QueueModelException if no value is found in localValues with 
	 *         the reference
	 */
	public IQueueValue<?> getLocalValue(QueueValue<String> valueReference) throws QueueModelException {
		return getRegistryValue(valueReference, localValues);
	}
	
	public IQueueValue<?> getPathModelValue(QueueValue<String> valueReference, String deviceName) throws QueueModelException {
		return getRegistryValue(valueReference, pathModelValues.get(deviceName).getDeviceConfiguration());
	}
	
	private IQueueValue<?> getRegistryValue(QueueValue<String> valueReference, List<IQueueValue<?>> registry) throws QueueModelException {
		Optional<IQueueValue<?>> value = registry.stream().filter(option -> valueReference.isReferenceFor(option)).findFirst();
		try {
			return value.get();
		} catch (NoSuchElementException nseEX) {
			throw new QueueModelException("No value in localValues for reference '"+valueReference.evaluate()+"'");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detectorModelValues == null) ? 0 : detectorModelValues.hashCode());
		result = prime * result + ((localValues == null) ? 0 : localValues.hashCode());
		result = prime * result + ((pathModelValues == null) ? 0 : pathModelValues.hashCode());
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
		ExperimentConfiguration other = (ExperimentConfiguration) obj;
		if (detectorModelValues == null) {
			if (other.detectorModelValues != null)
				return false;
		} else if (!detectorModelValues.equals(other.detectorModelValues))
			return false;
		if (localValues == null) {
			if (other.localValues != null)
				return false;
		} else if (!localValues.equals(other.localValues))
			return false;
		if (pathModelValues == null) {
			if (other.pathModelValues != null)
				return false;
		} else if (!pathModelValues.equals(other.pathModelValues))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ExperimentConfiguration [localValues=" + localValues + ", detectorModelValues=" + detectorModelValues
				+ ", pathModelValues=" + pathModelValues + "]";
	}
}

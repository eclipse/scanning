package org.eclipse.scanning.api.event.queues.models;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;

public class DeviceModel {
	
	/**
	 * Boxed and unboxed equivalents. Used by
	 * {@link #isSetMethodForName(java.lang.reflect.Method) to determine 
	 * whether the boxed type stored for a given option in deviceConfig is the 
	 * same as the unboxed type it is trying to replace.
	 */
	public static final Map<Class<?>, Class<?>> UNBOXEDTYPES;
	static {
		UNBOXEDTYPES = new HashMap<>();
		UNBOXEDTYPES.put(Boolean.class, boolean.class);
		UNBOXEDTYPES.put(Byte.class, byte.class);
		UNBOXEDTYPES.put(Character.class, char.class);
		UNBOXEDTYPES.put(Double.class, double.class);
		UNBOXEDTYPES.put(Float.class, float.class);
		UNBOXEDTYPES.put(Integer.class, int.class);
		UNBOXEDTYPES.put(Long.class, long.class);
		UNBOXEDTYPES.put(Short.class, short.class);
	}
	
	private String name, type;
	private Map<String, Object> deviceConfiguration;
	private List<DeviceModel> roiConfiguration;
	
	public DeviceModel(String type, Map<String, Object> deviceConfiguration) {
		this(type, deviceConfiguration, null);
	}
	
	public DeviceModel(String type, Map<String, Object> deviceConfiguration, List<DeviceModel> roiConfiguration) {
		this.type = type;
		this.deviceConfiguration = deviceConfiguration;
		if (roiConfiguration == null) roiConfiguration = new ArrayList<>();//NPE protection
		this.roiConfiguration = roiConfiguration;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Map<String, Object> getDeviceConfiguration() {
		return deviceConfiguration;
	}
	
	public void setDeviceConfiguration(Map<String, Object> deviceConfiguration) {
		this.deviceConfiguration = deviceConfiguration;
	}

	public List<DeviceModel> getRoiConfiguration() {
		return roiConfiguration;
	}

	public void setRoiConfiguration(List<DeviceModel> roiConfiguration) {
		this.roiConfiguration = roiConfiguration;
	}

	public Object getDeviceModelValue(String reference) throws QueueModelException {
		if (deviceConfiguration.containsKey(reference)) {
			return deviceConfiguration.get(reference);
		}
		throw new QueueModelException("No value in deviceConfiguration for reference '"+reference+"'");
	}
	
	/**
	 * Tests whether the given method is a setter for a String configOption 
	 * (which is a key in the deviceConfiguration) and that the argument types 
	 * match. 
	 * @param method Method with name to compare
	 * @param configOption String name of key in deviceConfiguration map
	 * @return true if the method name is set+{@link #getName()}.
	 * @throws IllegalArgumentException if this {@link IQueueValue} has the 
	 *         wrong type
	 */
	public boolean isSetMethodForName(Method method, String configOption) {
		if (method.getName().toLowerCase().equals(("set"+configOption).toLowerCase()) && method.getParameterCount() == 1) {
			Class<?> parameterType = method.getParameterTypes()[0];
			Object configValue = deviceConfiguration.get(configOption);
			
			if (parameterType.equals(configValue.getClass()) || parameterType.equals(UNBOXEDTYPES.get(configValue.getClass()))) {
					return true; //TODO This doesn't handle arrays as I haven't included them in UNBOXEDVALUES
			}
			throw new IllegalArgumentException(configOption+" is incorrect type ("+configValue.getClass().getSimpleName()+") for set method (expected: "+parameterType.getSimpleName()+")");
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((deviceConfiguration == null) ? 0 : deviceConfiguration.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
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
		String configString = deviceConfiguration.entrySet().stream().map(option -> option.getKey()+"="+option.getValue()).collect(Collectors.joining(", "));
		String fullString = "DeviceModel ";
		if (name != null) {
			fullString = fullString+"("+name+") "; 
		}
		fullString = fullString+"[" + type + ": deviceConfiguration={" + configString + "}"; 
		if (roiConfiguration.size() > 0) {
			String roiConfigString = roiConfiguration.stream().map(DeviceModel::toString).collect(Collectors.joining(", "));
			fullString = fullString + "; roiConfiguration={" + roiConfigString + "}";
		}
		return fullString + "]";
	}
	

}

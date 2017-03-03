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
package org.eclipse.scanning.api.malcolm.message;

import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.attributes.BooleanAttribute;
import org.eclipse.scanning.api.malcolm.attributes.ChoiceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringAttribute;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;

public class MalcolmUtil {

	/**
	 * Translate State out of strangely encoded map
	 * @param msg
	 * @return
	 * @throws Exception 
	 */
	public static DeviceState getState(MalcolmMessage msg) throws Exception {

		return getState(msg, true);
	}
	
	public static DeviceState getState(MalcolmMessage msg, boolean requireException) throws Exception {

		try {
			if (msg.getValue() instanceof String) {
				return DeviceState.valueOf(msg.getValue().toString().toUpperCase());
			} else if (msg.getValue() instanceof ChoiceAttribute) {
				ChoiceAttribute attribute = (ChoiceAttribute)msg.getValue();
				return DeviceState.valueOf(attribute.getValue().toUpperCase());
			} else {
				try {
				    return DeviceState.valueOf(getStringValueFromMap((Map)msg.getValue(), "state").toUpperCase());
				} catch (Exception ne) {
					throw new Exception("Value is not a map containing state!", ne);
				}
			}
		} catch (Exception ne) {
			if (requireException) throw ne;
			return null;
		}
	}

	public static DeviceState getState(Map<String, ?> value) throws Exception {
		
		if (value.containsKey("state")) {
			final String state = (String)value.get("state");
			return DeviceState.valueOf(state.toUpperCase());
		}
		
		if (value.containsKey("value") && !value.containsKey("choices")) {
			if (value.get("value") instanceof Map) {
				value = (Map<String,Object>)value.get("value");
				List<String> choices = (List<String>)value.get("choices");
				int          index   = (int)value.get("index");
				final String state = choices.get(index);
				return DeviceState.valueOf(state.toUpperCase());
			} else if (value.get("value") instanceof String) {
				final String state = (String)value.get("value");
				return DeviceState.valueOf(state.toUpperCase());
			}
		}
		
		throw new Exception("Unable to get state from value Map");
	}
	
	public static String getStatus(MalcolmMessage msg) throws Exception {
		return getStatus(msg, true);
	}
	
	public static String getStatus(MalcolmMessage msg, boolean requireException) throws Exception {

		try {
			if (msg.getValue() instanceof String) {
				return msg.getValue().toString();
			} else if (msg.getValue() instanceof StringAttribute) {
				StringAttribute attribute = (StringAttribute)msg.getValue();
				return attribute.getValue().toString();
			} else {
				try {
				    return getStringValueFromMap((Map)msg.getValue(), "status");
				} catch (Exception ne) {
					throw new Exception("Value is not a map containing status!", ne);
				}
			}
		} catch (Exception ne) {
			if (requireException) throw ne;
			return null;
		}
	}
	
	public static boolean getBusy(MalcolmMessage msg) throws Exception {
		return getBusy(msg, true);
	}
	
	public static boolean getBusy(MalcolmMessage msg, boolean requireException) throws Exception {

		try {
			if (msg.getValue() instanceof Boolean) {
				return (boolean)msg.getValue();
			} else if (msg.getValue() instanceof String) {
				return Boolean.parseBoolean(msg.getValue().toString());
			} else if (msg.getValue() instanceof BooleanAttribute) {
				BooleanAttribute attribute = (BooleanAttribute)msg.getValue();
				return attribute.getValue();
			} else {
				try {
					return getBooleanValueFromMap((Map)msg.getValue(), "busy");
				} catch (Exception ne) {
					throw new Exception("Value is not a map containing busy!", ne);
				}
			}
		} catch (Exception ne) {
			if (requireException) throw ne;
			return false;
		}
	}
	
	private static String getStringValueFromMap(Map<String, ?> map, String key) throws Exception {
		if (map.containsKey(key)) {
			final String stringValue = (String)map.get(key);
			return stringValue;
		}
		
		if (map.containsKey("value") && !map.containsKey("choices")) {
			if (map.get("value") instanceof Map) {
				map = (Map<String,Object>)map.get("value");
				List<String> choices = (List<String>)map.get("choices");
				int          index   = (int)map.get("index");
				final String stringValue = choices.get(index);
				return stringValue;
			} else if (map.get("value") instanceof String) {
				final String stringValue = (String)map.get("value");
				return stringValue;
			}
		}
		
		throw new Exception("Unable to get state from value Map");
	}
	
	private static boolean getBooleanValueFromMap(Map<String, ?> map, String key) throws Exception {
		if (map.containsKey(key)) {
			final boolean booleanValue = (Boolean)map.get(key);
			return booleanValue;
		}
		
		if (map.containsKey("value") && !map.containsKey("choices")) {
			if (map.get("value") instanceof Map) {
				map = (Map<String,Object>)map.get("value");
				List<String> choices = (List<String>)map.get("choices");
				int          index   = (int)map.get("index");
				final String stringValue = choices.get(index);
				return Boolean.parseBoolean(stringValue);
			} else if (map.get("value") instanceof Boolean) {
				final Boolean booleanValue = (Boolean)map.get("value");
				return booleanValue;
			} else if (map.get("value") instanceof String) {
				final String stringValue = (String)map.get("value");
				return Boolean.parseBoolean(stringValue);
			}
		}
		
		throw new Exception("Unable to get state from value Map");
	}


    /**
     * Method to check if event is likely to be scanning, not the start or the end.
     * @param bean
     * @return
     */
	public static boolean isScanning(MalcolmEventBean bean) {
		return !bean.isScanEnd() && !bean.isScanStart() && bean.getDeviceState().isRunning();
	}

	public static boolean isStateChange(MalcolmEventBean bean) {
		return bean.getPreviousState()!=null && bean.getDeviceState()!=bean.getPreviousState();
	}

}

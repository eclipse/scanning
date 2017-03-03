package org.eclipse.scanning.device.ui.points;

/**
 * Class to contain validation results for display in the UI
 * 
 * @author Matt Taylor
 *
 */
public class ValidateResults {
	private String deviceName;
	private Object results;
	
	public ValidateResults(String deviceName, Object results) {
		this.deviceName = deviceName;
		this.results = results;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	public Object getResults() {
		return results;
	}
	
	public void setResults(Object results) {
		this.results = results;
	}
	
}

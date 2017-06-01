package org.eclipse.scanning.device.composite;


public abstract class ChildNode {

	protected String scannableName;
	
	public ChildNode() {
		this(null);
	}
	
	public ChildNode(String scannableName) {
		this.scannableName = scannableName;
	}

	public String getScannableName() {
		return scannableName;
	}

	public void setScannableName(String scannableName) {
		this.scannableName = scannableName;
	}

}

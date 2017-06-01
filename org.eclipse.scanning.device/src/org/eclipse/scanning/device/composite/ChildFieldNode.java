package org.eclipse.scanning.device.composite;


public class ChildFieldNode extends ChildNode {
	
	private String sourceFieldName;
	private String destinationFieldName;
	
	public ChildFieldNode() {
		this(null, null, null);
	}
	
	public ChildFieldNode(String scannableName, String fieldName) {
		this(scannableName, fieldName, null);
	}
	
	public ChildFieldNode(String scannableName, String sourceFieldName, String destinationFieldName) {
		super(scannableName);
		this.sourceFieldName = sourceFieldName;
		this.destinationFieldName = destinationFieldName;
	}
	
	
	public String getSourceFieldName() {
		return sourceFieldName;
	}
	
	public void setSourceFieldName(String sourceFieldName) {
		this.sourceFieldName = sourceFieldName;
	}
	
	public String getDestinationFieldName() {
		if (destinationFieldName == null) {
			return sourceFieldName;
		}
		return destinationFieldName;
	}
	
	public void setDestinationFieldName(String destinationFieldName) {
		this.destinationFieldName = destinationFieldName;
	}

}

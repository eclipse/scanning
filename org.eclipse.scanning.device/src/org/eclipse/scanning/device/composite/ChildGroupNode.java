package org.eclipse.scanning.device.composite;

public class ChildGroupNode extends ChildNode {

	private String groupName;

	public ChildGroupNode() {
		// no-arg constructor for spring configuration
	}

	public ChildGroupNode(String scannableName) {
		this(scannableName, null);
	}

	public ChildGroupNode(String scannableName, String groupName) {
		super(scannableName);
		this.groupName = groupName;
	}

	/**
	 * Returns the name of the nexus group . This defaults to the scannable name if not set.
	 * @return the group name, or scannable name if not set
	 */
	public String getGroupName() {
		if (groupName != null) {
			return groupName;
		}
		return scannableName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}

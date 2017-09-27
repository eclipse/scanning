package org.eclipse.scanning.api.points.models;

public class JythonArgument {
	
	public enum JythonArgumentType {
		STRING, INTEGER, FLOAT;
	}

	private JythonArgumentType type;
	private String             value;
	
	public JythonArgument() {
	}
	
	public JythonArgument(String value, JythonArgumentType type) {
		this();
		this.value = value;
		this.type  = type;
	}
	public JythonArgumentType getType() {
		return type;
	}
	public void setType(JythonArgumentType type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		JythonArgument other = (JythonArgument) obj;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}

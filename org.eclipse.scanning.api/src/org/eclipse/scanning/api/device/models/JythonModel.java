package org.eclipse.scanning.api.device.models;

public class JythonModel extends SlicingModel {

	/**
	 * Name of the module, usually script name minus '.py'
	 */
	private String moduleName;

	/**
	 * Name of the jython class implementing IJythonFunction
	 */
	private String className;

	/**
	 * Shape of data returned by the script.
	 */
	private int    outputRank=1;

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public int getOutputRank() {
		return outputRank;
	}

	public void setOutputRank(int dataShape) {
		this.outputRank = dataShape;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + outputRank;
		result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JythonModel other = (JythonModel) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (outputRank != other.outputRank)
			return false;
		if (moduleName == null) {
			if (other.moduleName != null)
				return false;
		} else if (!moduleName.equals(other.moduleName))
			return false;
		return true;
	}
}

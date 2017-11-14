package org.eclipse.scanning.api.points.models;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

/**
 *
 * This class allows loading of a
 *
 * @author Matthew Gerring
 *
 */
public class JythonGeneratorModel extends AbstractPointsModel {

	@FieldDescriptor(label="Module Name",
			         hint="The name of the module to load.\nUsually this is the same as the python file without an ending '.py'",
			         fieldPosition=0)
	private String moduleName;

	@FieldDescriptor(label="Module Path",
			         hint="The file path to the module folder.",
			         file=FileType.EXISTING_FOLDER,
			         fieldPosition=1)
	private String path;

	@FieldDescriptor(label="Class Name",
	         hint="The name of the class implementing ScanPointIterator.\nIt must extend Iterator<IPosition> and have three methods:\n size(), getShape(), getRank()",
	         fieldPosition=2)
	private String className;

	@FieldDescriptor(visible=false) // TODO We should probably produce a TypeEditor to allow the arguments to be edited...
	private List<JythonArgument> arguments;

	@FieldDescriptor(visible=false) // TODO We should probably produce a TypeEditor to allow the keywords to be edited...
	private List<String> keywords;


	public String getModuleName() {
		return moduleName;
	}


	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}


	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
	}


	public String getClassName() {
		return className;
	}


	public void setClassName(String className) {
		this.className = className;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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

		JythonGeneratorModel other = (JythonGeneratorModel) obj;
		if (!equals(arguments, other.arguments))    return false;
		if (!equals(keywords,  other.keywords))     return false;
		if (!equals(className,  other.className))   return false;
		if (!equals(moduleName,  other.moduleName)) return false;
		if (!equals(path,  other.path))             return false;

		return true;
	}


	private boolean equals(Object a, Object b) {
		if (a == null) {
			if (b != null)
				return false;
		} else if (!a.equals(b))
			return false;
		return true;
	}

	public List<JythonArgument> getArguments() {
		return arguments;
	}


	public void setArguments(List<JythonArgument> arguments) {
		this.arguments = arguments;
	}


	public List<String> getKeywords() {
		return keywords;
	}


	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}


	public void addArgument(JythonArgument jythonArgument) {
		if (arguments==null) arguments = new ArrayList<>();
		arguments.add(jythonArgument);
	}
}

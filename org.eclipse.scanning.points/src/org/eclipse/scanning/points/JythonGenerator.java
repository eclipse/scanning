package org.eclipse.scanning.points;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.JythonGeneratorModel;

public class JythonGenerator extends AbstractGenerator<JythonGeneratorModel> {

	JythonGenerator() {
		setLabel("Function");
		setDescription("Uses a function to get the motor positions for the scan");
		setIconPath("icons/scanner--function.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {
		return new JythonIterator(getModel());
	}

	@Override
	protected void validateModel() throws ValidationException {

		if (model.getPath()==null) throw new ModelValidationException("No module directory is set!", model, "path");
		final File file = new File(model.getPath());
		if (!file.exists()) throw new ModelValidationException("The module directory '"+file+"' does not exist!", model, "path");
		if (!file.isDirectory()) throw new ModelValidationException("The module directory path '"+file+"' is not a folder!", model, "path");

		if (!Optional.ofNullable(model.getModuleName()).isPresent())
			throw new ModelValidationException("The module name must be set!", model, "moduleName");
		if (!Optional.ofNullable(model.getClassName()).isPresent())
			throw new ModelValidationException("The class name must be set!", model, "className");

        super.validateModel();

	}
}

package org.eclipse.scanning.points;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.JythonArgument;
import org.eclipse.scanning.api.points.models.JythonArgument.JythonArgumentType;
import org.eclipse.scanning.api.points.models.JythonGeneratorModel;
import org.eclipse.scanning.jython.JythonInterpreterManager;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JythonGenerator extends AbstractGenerator<JythonGeneratorModel> {

	private static final Logger logger = LoggerFactory.getLogger(JythonGenerator.class);

	JythonGenerator() {
		setLabel("Function");
		setDescription("Uses a function to get the motor positions for the scan");
		setIconPath("icons/scanner--function.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {

		try {
			// Ensure that the module path is on the path
			JythonInterpreterManager.addPath(model.getPath());
		} catch (IOException e) {
			logger.error("Unable to add '"+model.getPath()+"' to path");
		}

		final JythonObjectFactory<ScanPointIterator> jythonObject =
				new JythonObjectFactory<>(ScanPointIterator.class, model.getModuleName(), model.getClassName());

		final ScanPointIterator pyIterator;
		if (model.getArguments() == null || model.getArguments().isEmpty()) {
			pyIterator = jythonObject.createObject();
		} else {
			final Object[] args = model.getArguments().stream().map(JythonGenerator::getArgument).toArray();
			final String[] keywords = model.getKeywords() == null ? new String[0] :
				model.getKeywords().toArray(new String[model.getKeywords().size()]);
			pyIterator = jythonObject.createObject(args, keywords);
		}

		return new SpgIterator(pyIterator);
	}

	private static Object getArgument(JythonArgument arg) {
		if (arg.getType()==JythonArgumentType.INTEGER) {
			return Integer.parseInt(arg.getValue());
		} else if (arg.getType()==JythonArgumentType.FLOAT) {
			return Float.parseFloat(arg.getValue());
		}
		return arg.getValue();
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

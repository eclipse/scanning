package org.eclipse.scanning.points;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.JythonArgument;
import org.eclipse.scanning.api.points.models.JythonArgument.JythonArgumentType;
import org.eclipse.scanning.api.points.models.JythonGeneratorModel;
import org.eclipse.scanning.jython.JythonInterpreterManager;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JythonIterator extends AbstractScanPointIterator implements Iterator<IPosition> {

	private static final Logger logger = LoggerFactory.getLogger(JythonIterator.class);

	JythonIterator(JythonGeneratorModel model) {

		try {
			// Ensure that the module path is on the path
			JythonInterpreterManager.addPath(model.getPath());
		} catch (IOException e) {
			logger.error("Unable to add '"+model.getPath()+"' to path");
		}

		final JythonObjectFactory<ScanPointIterator> jythonObject =
				new JythonObjectFactory<>(ScanPointIterator.class, model.getModuleName(), model.getClassName());

		if (model.getArguments() == null || model.getArguments().isEmpty()) {
			this.pyIterator = jythonObject.createObject();
		} else {
			final Object[] args = model.getArguments().stream().map(JythonIterator::getArgument).toArray();
			final String[] keywords = model.getKeywords() == null ? new String[0] :
				model.getKeywords().toArray(new String[model.getKeywords().size()]);
			this.pyIterator = jythonObject.createObject(args, keywords);
		}
	}

	private static Object getArgument(JythonArgument arg) {
		if (arg.getType()==JythonArgumentType.INTEGER) {
			return Integer.parseInt(arg.getValue());
		} else if (arg.getType()==JythonArgumentType.FLOAT) {
			return Float.parseFloat(arg.getValue());
		}
		return arg.getValue();
	}

}

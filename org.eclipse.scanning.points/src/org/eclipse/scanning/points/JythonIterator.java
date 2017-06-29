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
		
	private int               index;
	
	JythonIterator(JythonGeneratorModel model) {
		
		try {
			// Ensure that the module path is on the path
			JythonInterpreterManager.addPath(model.getPath());
		} catch (IOException e) {
			logger.error("Unable to add '"+model.getPath()+"' to path");
		} 
		
		JythonObjectFactory<ScanPointIterator> jythonObject = new JythonObjectFactory<>(ScanPointIterator.class, model.getModuleName(), model.getClassName());
		
		Object[] args = getArguments(model);
		String[] kwds = getKeywords(model);
		if (args==null) {
			this.pyIterator = jythonObject.createObject();
		} else {
			this.pyIterator = jythonObject.createObject(args, kwds);
		}
		this.index = 0;
	}

	private String[] getKeywords(JythonGeneratorModel model) {
		if (model.getKeywords()==null) return new String[]{};
		return model.getKeywords().toArray(new String[model.getKeywords().size()]);
	}

	private Object[] getArguments(JythonGeneratorModel model) {
		if (model.getArguments()==null) return null;
		return model.getArguments().stream().map(a->getArgument(a)).toArray();
	}
	private Object getArgument(JythonArgument arg) {
		if (arg.getType()==JythonArgumentType.INTEGER) {
			return Integer.parseInt(arg.getValue());
		} else if (arg.getType()==JythonArgumentType.FLOAT) {
			return Float.parseFloat(arg.getValue());
		}
		return arg.getValue();
	}

	@Override
	public boolean hasNext() {
		return pyIterator.hasNext();
	}

	@Override
	public IPosition next() {
		IPosition next = pyIterator.next();	
		next.setStepIndex(index);
		index++;
		return next;
	}

	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}

package org.eclipse.scanning.jython;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;

// This class creates Java objects from Jython classes
public final class JythonObjectFactory {


	private final Class<?> javaClass;
	private final PyObject pyClass;

	// This constructor passes through to the other constructor with the SystemState
	public JythonObjectFactory(Class<?> javaClass, String moduleName, String className) {

		JythonInterpreterManager.setupSystemState();
		PySystemState state = Py.getSystemState();

		this.javaClass = javaClass;
		PyObject importer = state.getBuiltins().__getitem__(Py.newString("__import__"));
		PyObject module = importer.__call__(Py.newString(moduleName));
		pyClass = module.__getattr__(className);
	}

	// The following methods return a coerced Jython object based upon the pieces of
	// information that were passed into the factory, for various argument structures

	public Object createObject() {
		return pyClass.__call__().__tojava__(javaClass);
	}
	public Object createObject(Object arg1) {
		return pyClass.__call__(Py.java2py(arg1)).__tojava__(javaClass);
	}
	public Object createObject(Object arg1, Object arg2) {
		return pyClass.__call__(Py.java2py(arg1), Py.java2py(arg2)).__tojava__(javaClass);
	}
	public Object createObject(Object arg1, Object arg2, Object arg3) {
		return pyClass.__call__(Py.java2py(arg1), Py.java2py(arg2), Py.java2py(arg3)).__tojava__(javaClass);
	}
	public Object createObject(Object args[], String keywords[]) {
		PyObject convertedArgs[] = new PyObject[args.length];
		for (int i = 0; i < args.length; i++) {
			convertedArgs[i] = Py.java2py(args[i]);
		}
		return pyClass.__call__(convertedArgs, keywords).__tojava__(javaClass);
	}
	public Object createObject(Object... args) {
		return createObject(args, Py.NoKeywords);
	}

}

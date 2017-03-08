package org.eclipse.scanning.jython;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;

// This class creates Java objects from Jython classes
public final class JythonObjectFactory<T> {


	private final Class<T> javaClass;
	private final PyObject pyClass;

	// This constructor passes through to the other constructor with the SystemState
	public JythonObjectFactory(Class<T> javaClass, String moduleName, String className, String... bundleNames) {

		JythonInterpreterManager.setupSystemState(bundleNames);
		PySystemState state = Py.getSystemState();

		this.javaClass = javaClass;
		PyObject importer = state.getBuiltins().__getitem__(Py.newString("__import__"));
		PyObject module = importer.__call__(Py.newString(moduleName));
		pyClass = module.__getattr__(className);
	}

	// The following methods return a coerced Jython object based upon the pieces of
	// information that were passed into the factory, for various argument structures

	@SuppressWarnings("unchecked")
	public T createObject() {
		return (T)pyClass.__call__().__tojava__(javaClass);
	}
	@SuppressWarnings("unchecked")
	public T createObject(Object arg1) {
		return (T)pyClass.__call__(Py.java2py(arg1)).__tojava__(javaClass);
	}
	@SuppressWarnings("unchecked")
	public T createObject(Object arg1, Object arg2) {
		return (T)pyClass.__call__(Py.java2py(arg1), Py.java2py(arg2)).__tojava__(javaClass);
	}
	@SuppressWarnings("unchecked")
	public T createObject(Object arg1, Object arg2, Object arg3) {
		return (T)pyClass.__call__(Py.java2py(arg1), Py.java2py(arg2), Py.java2py(arg3)).__tojava__(javaClass);
	}
	@SuppressWarnings("unchecked")
	public T createObject(Object args[], String keywords[]) {
		PyObject convertedArgs[] = new PyObject[args.length];
		for (int i = 0; i < args.length; i++) {
			convertedArgs[i] = Py.java2py(args[i]);
		}
		return (T)pyClass.__call__(convertedArgs, keywords).__tojava__(javaClass);
	}
	public T createObject(Object... args) {
		return createObject(args, Py.NoKeywords);
	}

}

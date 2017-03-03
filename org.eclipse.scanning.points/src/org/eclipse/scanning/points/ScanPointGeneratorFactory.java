/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.points;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.python.core.Py;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on JythonObjectFactory
 * See: http://www.jython.org/jythonbook/en/1.0/JythonAndJavaIntegration.html#more-efficient-version-of-loosely-coupled-object-factory
 */
public class ScanPointGeneratorFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(ScanPointGeneratorFactory.class);
	

	/**
	 * Call to load Jython asynchronously to avoid the
	 * long wait time that happens when points are first generated.
	 * 
	 * Call this method to load jython in a daemon thread such that
	 * when it is first used, for instance in the UI, it will execute
	 * fast because the interpreter has classloaded.
	 */
	public static void init() {
		
		final Thread background = new Thread() {
			public void run() {
				// Loading one causes Jython to class load.
				ScanPointGeneratorFactory.JLineGenerator1DFactory();
			}
		};
		background.setDaemon(true);
		background.setName("Jython loader thread");    // Always name threads.
		background.setPriority(Thread.MIN_PRIORITY+2); // Background but some urgency more than least
		background.start();
	}
	
	
	// This class compiles Jython objects and maps them to an IPointGenerator so they can be
	// used easily in Java. More specifically, it creates the Jython ScanPointGenerator interface
	// classes found in the scripts folder of this package (org.eclipse.scanning.points)
	
	// These are the constructors for each Jython SPG interface. To add a new one just replace, 
	// for example, "JArrayGenerator" with your new class and give the constructor a new name
	// like "<YourClass>Factory"
    public static JythonObjectFactory JLineGenerator1DFactory() {
        return new JythonObjectFactory(Iterator.class, "jython_spg_interface", "JLineGenerator1D");
    }
	
    private static void addLast(CompositeClassLoader composite, String bundleName) {
    	try {
    		ClassLoader cl = getBundleLoader(bundleName);
    		composite.addLast(cl);
	    } catch (NullPointerException ne) {
	    	// Allowed, the bundles do not have to be there we are just trying to be helpful
	    	// in loading classes without making hard dependencies on them.

    	}
	}

	public static JythonObjectFactory JLineGenerator2DFactory() {
        return new JythonObjectFactory(Iterator.class, "jython_spg_interface", "JLineGenerator2D");
    }
	
    public static JythonObjectFactory JArrayGeneratorFactory() {
        return new JythonObjectFactory(Iterator.class, "jython_spg_interface", "JArrayGenerator");
    }
	
    public static JythonObjectFactory JSpiralGeneratorFactory() {
        return new JythonObjectFactory(Iterator.class, "jython_spg_interface", "JSpiralGenerator");
    }
	
    public static JythonObjectFactory JLissajousGeneratorFactory() {
        return new JythonObjectFactory(Iterator.class, "jython_spg_interface", "JLissajousGenerator");
    }
	
    public static JythonObjectFactory JCompoundGeneratorFactory() {
        return new JythonObjectFactory(SerializableIterator.class, "jython_spg_interface", "JCompoundGenerator");
    }
	
    public static JythonObjectFactory JRandomOffsetMutatorFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JRandomOffsetMutator");
    }
    
    public static JythonObjectFactory JFixedDurationMutatorFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JFixedDurationMutator");
    }
    
    public static JythonObjectFactory JCircularROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JCircularROI");
    }
    
    public static JythonObjectFactory JEllipticalROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JEllipticalROI");
    }
    
    public static JythonObjectFactory JPointROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JPointROI");
    }
    
    public static JythonObjectFactory JPolygonalROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JPolygonalROI");
    }
    
    public static JythonObjectFactory JRectangularROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JRectangularROI");
    }
    
    public static JythonObjectFactory JSectorROIFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JSectorROI");
    }
    
    public static JythonObjectFactory JExcluderFactory() {
        return new JythonObjectFactory(PyObject.class, "jython_spg_interface", "JExcluder");
    }

    // This class creates Java objects from Jython classes
    public static class JythonObjectFactory {
    	
        
    	private final Class<?> javaClass;
        private final PyObject pyClass;
        
        // This constructor passes through to the other constructor with the SystemState
        JythonObjectFactory(Class<?> javaClass, String moduleName, String className) {

        	setupSystemState();
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
    
    private static File find(File loc, String name) {
    	
    	if (loc==null) return null;
    	if (!loc.exists()) return null;
    	File find = new File(loc, name);
        if (find.exists()) return find;
        
        for (File child : loc.listFiles()) {
		    if (child.getName().startsWith(name)) {
		    	return child;
		    }
		}
        return null;
	}

	private static volatile PySystemState configuredState;

	private static synchronized void setupSystemState() {
		
		ClassLoader loader=null;
		if (configuredState==null) { // Relies on setupSystemState() being called early in the server startup.
			loader = createJythonClassLoader(PySystemState.class.getClassLoader());
	 		initializePythonPath(loader); 
		}
		
    	PySystemState state = Py.getSystemState();
    	if (state==configuredState) return;
    	
    	if (loader==null) {
    		// Then someone else has changed the PySystemState
    		// They will not have added our 
			loader = createJythonClassLoader(state.getClassLoader());   // Don't clobber their working.		
    	}
	   	state.setClassLoader(loader);
    	
     	setJythonPaths(state);       // Tries to ensure that enough of jython is on the path
     	setSpgGeneratorPaths(state); // Adds the scripts directory from points
	   	Py.setSystemState(state);
 
	   	configuredState = state;
	}

	private static void setSpgGeneratorPaths(PySystemState state) {
		
        final File pointsLocation = getBundleLocation("org.eclipse.scanning.points");
        state.path.add(new PyString(pointsLocation.getAbsolutePath() + "/scripts/"));
	}


	private static void initializePythonPath(ClassLoader loader) {
		try {
	    	String jythonBundleName = System.getProperty("org.eclipse.scanning.jython.osgi.bundle.name", "uk.ac.diamond.jython");
	        File loc = getBundleLocation(jythonBundleName); // TODO Name the jython OSGi bundle without Diamond in it!
	        if (loc ==null) return;
	        File jythonDir = find(loc, "jython");
	        if (jythonDir ==null) return;
	        
	        Properties props = new Properties();
	    	props.setProperty("python.home", jythonDir.getAbsolutePath());
	    	props.setProperty("python.console.encoding", "UTF-8"); // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
	    	props.setProperty("python.options.showJavaExceptions", "true");
	    	props.setProperty("python.verbose", "warning");

	    	Properties preprops = System.getProperties();
	    	
	    	PySystemState.initialize(preprops, props, new String[]{}, loader);
	    
	    } catch (Throwable ne) {
	    	System.out.print("Problem loading jython bundles!");
	    	ne.printStackTrace();
	    	logger.debug("Problem loading jython bundles!", ne);
	    }
	}


	private static ClassLoader createJythonClassLoader(ClassLoader classLoader) {
		
    	ClassLoader jythonClassloader = ScanPointGeneratorFactory.class.getClassLoader();
    	
    	try { // For non-unit tests, attempt to use the OSGi classloader of this bundle.
    		String jythonBundleName = System.getProperty("org.eclipse.scanning.jython.osgi.bundle.name", "uk.ac.diamond.jython");
    		CompositeClassLoader composite = new CompositeClassLoader(classLoader);
   	  	    // Classloader for org.eclipse.scanning.points
    		composite.addLast(ScanPointGeneratorFactory.class.getClassLoader());
    		addLast(composite, jythonBundleName);
    		jythonClassloader = composite;
     		
    	} catch (Throwable ne) {
    		ne.printStackTrace();
    		// Legal, if static classloader does not work in tests, there will be
    		// errors. If bundle classloader does not work in product, there will be errors.
    		// Typically the message is something like: 'cannot find module org.eclipse.scanning.api'
    	}
    	return jythonClassloader;
	}

	private static void setJythonPaths(PySystemState state) {
    	        	
        try {
        	// Search for the Libs directory which should have been expanded out either
        	// directly into the bundle or into the 'jython2.7' folder.
	    	String jythonBundleName = System.getProperty("org.eclipse.scanning.jython.osgi.bundle.name", "uk.ac.diamond.jython");

            File lib = getBundleLocation(jythonBundleName); // TODO Name the jython OSGi bundle without Diamond in it!
	    	Iterator it = state.path.iterator();
	    	while (it.hasNext()) {
	    		Object ob = it.next();
	    		if (ob instanceof String && ((String)ob).endsWith(File.separator+"Lib")) {
	    			lib = new File((String)ob);
	    		}
	    	}
	    	
	    	if (lib==null) {
	            File loc = getBundleLocation(jythonBundleName); // TODO Name the jython OSGi bundle without Diamond in it!
           	    File jythonDir = find(loc, "jython");
            	lib       = find(jythonDir, "Lib");
	    	}
	    	
            if (lib != null) {
            	File site       = find(lib, "site-packages");
            	state.path.add(new PyString(site.getAbsolutePath())); // Resolves the collections

            	File[] fa = site.listFiles();
            	for (File dir : fa) {
            		if (!dir.isDirectory()) continue;
            		if (dir.getName().endsWith("-info")) continue;
            		state.path.add(new PyString(dir.getAbsolutePath())); // Resolves the collections
            	}
            	
            	state.toString();

            	System.out.println(state.path);
            }
            
        } catch (Exception ne) {
        	System.out.println("Problem setting jython path to include scripts");
        	ne.printStackTrace();
        	logger.debug("Problem setting jython path to include scripts!", ne);
        }
 	}

	private static ClassLoader getBundleLoader(String name) {
    	Bundle bundle = Platform.getBundle(name);
    	BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    	return bundleWiring.getClassLoader();
	}

	private static String bundlePath = null;
    
    public String getBundlePath() {
    	return bundlePath;
    }
 
	public static void setBundlePath(String newPath) {
    	bundlePath = newPath;
    }
    
    /**
	 * @param bundleName
	 * @return File
	 */
	private static File getBundleLocation(final String bundleName) {
		
		try {
			final Bundle bundle = Platform.getBundle(bundleName);
			if (bundle == null) {
				throw new IOException();
			}
			return FileLocator.getBundleFile(bundle);
		}
		catch (IOException e) {
			File dir = new File("../"+bundleName);
			if (dir.exists()) return dir;
			
			dir = new File("../../diamond-jython.git/"+bundleName);
			if (dir.exists()) return dir;
			
			dir = new File("../../daq-eclipse.git/"+bundleName);
			if (dir.exists()) return dir;
			
			// These paths refer to finding things in the travis build 
			// They will not resolve from the IDE or binary.
			dir = new File("../../org.eclipse.scanning/"+bundleName);
			if (dir.exists()) return dir;
			
			dir = new File("../../diamond-jython/"+bundleName);
			if (dir.exists()) return dir;
			
			dir = new File("../../../diamond-jython/"+bundleName);
			if (dir.exists()) return dir;
		}
		return null;
	}

}
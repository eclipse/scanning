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

import java.util.List;
import java.util.Vector;

/**
 * 
 * A simple classloader which can be used as a composite for 
 * the purposes of Jython loading.
 * 
 * @author Matthew Gerring
 *
 */
class CompositeClassLoader extends ClassLoader {

    private final List<ClassLoader> classLoaders;
 
    private CompositeClassLoader() {
        this.classLoaders = new Vector<>(7);
    }
    
    /**
     * Creates a CompositeClassLoader with the argument passed in as the first loader.
     * @param orig
     */
    public CompositeClassLoader(ClassLoader orig) {
    	this();
        addFirst(orig); 
    }
   
    /**
     * Call to add a loader at the end of this loader
     * @param classLoader
     */
    public void addLast(ClassLoader classLoader) {
        if (classLoader == null) throw new IllegalArgumentException("The classloader may not be null!");
        classLoaders.add(classLoader);
    }
    /**
     * Call to add a loader at the start of this loader
     * @param classLoader
     */
    public void addFirst(ClassLoader classLoader) {
        if (classLoader == null) return;
        classLoaders.add(0, classLoader);
    }
        
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        
    	for (ClassLoader classLoader : classLoaders) {
            try {
                return classLoader.loadClass(name);
            } catch (Throwable notFound) {
                // This is allowable
            	continue;
            }
        }
        
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader.loadClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }

}

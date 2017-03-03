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
package org.eclipse.scanning.api.annotation.scan;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.scanning.api.IServiceResolver;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.IScanParticipant;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * The device manager parses annotations and allows methods to be 
 * efficiently called during a scan to notify of progress. This replaces
 * the need to override an atScanStart() method as well as allowing resources
 * to be injected into the method. 
 * 
 * If attemps to parse all the reflection stuff up-front so that a call
 * to invoke(...) during the scan can be as efficiently despatched using
 * method.invoke(...) as possible.
 * 
 * This class could be made into a general purpose annotation parsing
 * and method calling class once tested.
 * 
 * NOTE: If you find yourself debugging this class to view despatched events,
 * consider adding a test to @see AnnotationManagerTest to reproduce the problem.
 * Trying to debug annotation parsing in a live scanning system is not desirable.
 * 
 * @author Matthew Gerring
 *
 */
public class AnnotationManager {
	
	private static Logger logger = LoggerFactory.getLogger(AnnotationManager.class);
	
	private Map<Class<? extends Annotation>, Collection<MethodWrapper>> annotationMap;
	private Map<Class<?>, Collection<Class<?>>>                         cachedClasses;
	private Map<Class<?>, Object>                                       services;
	private Collection<Object>                                          extraContext;

	private Collection<Class<? extends Annotation>> annotations;
	private IServiceResolver resolver;

	
	public AnnotationManager(IServiceResolver resolver) {
		this(resolver, DeviceAnnotations.getAllAnnotations());
	}
	
	@SafeVarargs
	public AnnotationManager(IServiceResolver resolver, Class<? extends Annotation>... a) {
		this(resolver, Arrays.asList(a));
	}

	/**
	 * Set some implementations of types, for instance services.
	 * Used in addition to the OSGi services available.
	 * In test mode replaces OSGi services.
	 * 
	 * @param services
	 */
	public AnnotationManager(IServiceResolver resolver, Map<Class<?>, Object> services) {
		this(resolver);
		this.services = services;
	}
	
	/**
	 * 
	 * @param resolver - may be null
	 * @param a
	 */
	private AnnotationManager(IServiceResolver resolver, Collection<Class<? extends Annotation>> a) {
		this.resolver = resolver;
		this.annotationMap = new Hashtable<>(31); // Intentionally synch
		this.cachedClasses = new Hashtable<>(31); // Intentionally synch
		this.annotations = a;
		
		if (resolver!=null) {
			try {
				Collection<IScanParticipant> others = resolver.getServices(IScanParticipant.class);
				if (others !=null) addDevices(others);
			} catch (Exception ne) {
				// We do not actually care if scanning could not get the IScanParticipants.
				logger.warn("Could not add implementors of "+IScanParticipant.class+" into annotated devices!", ne);
			}
		}
	}

	/**
	 * Add a group of devices. As the devices are added if they implement ILevel,
	 * they are sorted by level and added in that order. If another call to add
	 * devices is made the new collection of devices will be sorted by level and
	 * added to the end of the main list of devices.
	 * 
	 * So for:
	 * <code>
	 * manager.add(devices1[])
	 * manager.add(devices2[])
	 * </code>
	 * The notification order will be all the devices1, by level then all the devices2
	 * by level. So the overall order is by add order followed by level. This allows
	 * for instance all devices of a given type to be notified by level before another
	 * group of objects of another type. If no distinction of type is required, simply
	 * add all devices in one go and they will be sorted by level.
	 * 
	 * If a device does not implement ILevel its level is assumed to be ILevel.MAXIMUM 
	 * 
	 * @param ds
	 */
	public void addDevices(Object... ds) {
		if (ds == null)  throw new IllegalArgumentException("No devices specified!");
		if (ds.length<1) throw new IllegalArgumentException("No devices specified!");
		addDevices(Arrays.asList(ds));
	}
	
	/**
	 * Add a group of devices. As the devices are added if they implement ILevel,
	 * they are sorted by level and added in that order. If another call to add
	 * devices is made the new collection of devices will be sorted by level and
	 * added to the end of the main list of devices.
	 * 
	 * So for:
	 * <code>
	 * manager.add(devices1[])
	 * manager.add(devices2[])
	 * </code>
	 * The notification order will be all the devices1, by level then all the devices2
	 * by level. So the overall order is by add order followed by level. This allows
	 * for instance all devices of a given type to be notified by level before another
	 * group of objects of another type. If no distinction of type is required, simply
	 * add all devices in one go and they will be sorted by level.
	 * 
	 * If a device does not implement ILevel its level is assumed to be ILevel.MAXIMUM 
	 * 
	 * @param ds
	 */
	public void addDevices(Collection<?> ds) {
		
		if (ds == null)  throw new IllegalArgumentException("No devices specified!");
		// Make a copy of it and sort it
		List<Object> devices = new ArrayList<>(ds);
		Collections.sort(devices, new LevelComparitor());
		addOrderedDevices(devices);
	}
	private void addOrderedDevices(Collection<Object> ds) {
		for (Object object : ds) processAnnotations(object);
	}

	private void processAnnotations(Object device) {
		
		if (device==null) return;
		
		final Method[] methods = device.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			final Annotation[] as = methods[i].getAnnotations();
			if (as!=null) for (Annotation annotation : as) {
				Class<? extends Annotation> clazz = annotation.annotationType();
				if (this.annotations.contains(clazz)) {
					Collection<MethodWrapper> ms = annotationMap.get(clazz);
					if (ms == null) {
						ms = new ArrayList<>(31);
						annotationMap.put(clazz, ms);
					}
					ms.add(new MethodWrapper(clazz, device, methods[i]));
				}
			}
		}
	}

	/**
	 * Notify the methods with this annotation that it happened. 
	 * Optionally provide some context which the system will try to insert into the
	 * argument list when it is called.
	 * 
	 * @param annotation like &#64;ScanStart etc.
	 * @param context, extra things like ScanInformation, IPosition etc.
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public void invoke(Class<? extends Annotation> annotation, Object... context) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, ScanningException, EventException {
		try {
			final Collection<MethodWrapper> as = annotationMap.get(annotation);
			if (as!=null) for (MethodWrapper wrapper : as) wrapper.invoke(context);
			
		} catch (InvocationTargetException wapperExceptioned) {
		    Throwable supressed = wapperExceptioned.getTargetException();
		    if (supressed!=null) {
		    	// If they returned one of the scanning.api exceptions, throw this from the call.
		    	if (supressed instanceof ScanningException) throw (ScanningException)supressed;
		    	if (supressed instanceof EventException) throw (EventException)supressed;
		    }
		    throw wapperExceptioned;
		}
	}
	
	private class MethodWrapper {
		
		private Object          instance;
		private Method          method;
		private List<Class<?>>  argClasses;
		private Object[]        arguments; // Must be object[] for speed and is not variable
		
		MethodWrapper(final Class<? extends Annotation> aclass, Object instance, Method method) throws IllegalArgumentException {
			this.instance = instance;
			this.method   = method;
			
			final Class<?>[] args = method.getParameterTypes();
			this.argClasses = args!=null?Arrays.asList(args):null;
			
			if (argClasses!=null) {
				final Set<?> unique = new HashSet<>(argClasses);
				
				/**
				 * We do not allow duplications in the classes list because a given service or
				 * information object should be required once. Type is used to determine argument 
				 * position as well, therefore duplicates do not work with the current alg.
				 */
			    if (unique.size()!=argClasses.size()) throw new IllegalArgumentException("Duplicated types are not allowed in injected methods!\n"
			    		+ "Your annotation of @"+aclass.getSimpleName()+" sits over a method '"+method.getName()+"' on class '"+instance.getClass().getSimpleName()+"' with duplicated types!\n"
			    	    + "More than one of any given type is not allowed. Have you seen '"+ScanInformation.class.getSimpleName()+"' class, which can be used to provide various metrics about the scan?");
			}
			
			if (args!=null) {
				this.arguments= new Object[args.length];
				for (int i = 0; i < args.length; i++) {
					if (args[i] == IPosition.class) continue;
				    // Find OSGi service for it, if any.
					try {
						arguments[i] = getService(args[i]);
					} catch (Exception ne) {
						continue;
					}
				}
			}
		}
		
		public void invoke(Object... objects) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			
			if (arguments!=null) { // Put the context into the args (if there are any)
				
				List<Object> context = getContext(objects);
				for (int i = 0; i < context.size(); i++) {
					
				    final Collection<Class<?>> classes = getCachedClasses(context.get(i));
				    
				    // Find the first class in classes which is in argClasses
				    // NOTE this is why duplicates are not supported, type of argument used to map to injected class.
                	Optional<Class<?>> contained = classes.stream().filter(x -> argClasses.contains(x)).findFirst();
                	if (contained.isPresent()) {
                	    final int index = argClasses.indexOf(contained.get());
                	    arguments[index] = context.get(i);
                    }
				}
				boolean accessible = method.isAccessible();
				try {
					method.setAccessible(true);
					method.invoke(instance, arguments);
				} finally {
					method.setAccessible(accessible);
				}
			} else {
				method.invoke(instance);
			}
		}
	}

	/**
	 * TODO Cache for speed?
	 * @param object
	 * @return
	 */
	private Collection<Class<?>> getCachedClasses(Object object) {
		
		final Class<?> clazz = object.getClass();
		if (cachedClasses.containsKey(clazz)) return cachedClasses.get(clazz);
		
		final Collection<Class<?>> classes = new HashSet<>();
		classes.add(clazz);
		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> class1 : interfaces)  classes.add(class1);
		
		// TODO Currently only support one level deep
		classes.add(clazz.getSuperclass());
		interfaces = clazz.getSuperclass().getInterfaces();
		for (Class<?> class1 : interfaces)  classes.add(class1);
		
		cachedClasses.put(clazz, classes);
		
		return classes;
	}
	
	public List<Object> getContext(Object[] objects) {
		List<Object> context = new ArrayList<>();
		if (extraContext!=null) context.addAll(extraContext);
		if (objects!=null && objects.length>0) context.addAll(Arrays.asList(objects));
		return context;
	}

	/**
	 * 
	 * @param object
	 */
	public void addContext(Object object) {
		if (extraContext == null) extraContext = new HashSet<>();
		extraContext.add(object);
	}

	/**
	 * 
	 * @param object
	 */
	public void removeContext(Object object) {
		if (extraContext == null) return;
		extraContext.remove(object);
	}

	private Object getService(Class<?> class1) {
		Object object=null;
		if (resolver!=null) object = resolver.getService(class1);
		if (object==null)   object = services.get(class1);
		return object;
	}

	public void dispose() {
		annotationMap.clear();
		cachedClasses.clear();
		if (extraContext!=null) extraContext.clear();
	}
}

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
package org.eclipse.scanning.server.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.scanning.api.ISpringParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * Parses a spring file without making a dependency on spring.
 * Only supports a very limited subset of that which is available
 * in spring. 
 * 
 * @author Matthew Gerring
 *
 */
@SuppressWarnings("deprecation")
public class PseudoSpringParser implements ISpringParser {
	
	private static final Logger logger = LoggerFactory.getLogger(PseudoSpringParser.class);
	
	private static ComponentContext context;
	
	private File dir;

	/**
	 * Manually parse spring XML to create the objects.
	 * This means that the example has no dependency on a
	 * particular spring version and can be part of the open
	 * source project.
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> parse(String path) throws Exception {
		final File file = new File(path);
		if (!file.exists()) throw new IOException("Cannot find file "+file);
		this.dir = file.getParentFile();
		Document doc = getDocument(path);
		return parse(doc);
	}
	
	@Override
	public void setDirectory(File dir) {
		this.dir = dir;
	}
	
	/**
	 * Manually parse spring XML to create the objects.
	 * This means that the example has no dependency on a
	 * particular spring version and can be part of the open
	 * source project.
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> parse(InputStream in) throws Exception {
		Document doc = getDocument(in);
		return parse(doc);
	}

	private Document getDocument(String path) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    return builder.parse(new File(path));
	}
	
	private Document getDocument(InputStream in) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(in);
	}

	private Map<String, Object> parse(Document doc) throws Exception {
		
		Map<String, Object> objects = parseBeans(doc);
		return objects;
	}

	private Map<String, Object> parseBeans(Document doc) throws Exception {
		
	    
	    Map<String, Object>    objects = new HashMap<>();
	    Map<String, NamedList> lists   = new HashMap<>();
	    return parseBeans(doc, objects, lists);
	}
	
	private Map<String, Object> parseBeans(Document doc, Map<String, Object> objects, Map<String, NamedList> lists) throws Exception {
	    
	    doc.getDocumentElement().normalize();
		NodeList  nl = doc.getChildNodes().item(0).getChildNodes();
	    for (int i = 0; i < nl.getLength(); i++) {
			if (!(nl.item(i) instanceof Element)) continue;
	    	Element element = (Element)nl.item(i);
	    	
	    	if ("bean".equals(element.getTagName())) {
	    		parseBean(element, objects, lists);
	    		continue;
	    	}
	    	if ("osgi:service".equals(element.getTagName())) continue; // Deal with later
	    	
	    	if ("import".equals(element.getTagName())) {
				String link = element.getAttributes().getNamedItem("resource").getNodeValue();
				final File file = new File(dir, link); // No dir, no links!
				Document child = getDocument(file.getAbsolutePath());
				parseBeans(child, objects, lists);
	    		continue;
	    	}
	    	
	    	throw new Exception("Unrecognised element: "+element+" with tag "+element.getTagName());
	    }
	    
	    
	    // We process the lists to wire together objects
	    for (String id : lists.keySet()) {
	    	final NamedList namedList  = lists.get(id);
			final Object    object     = objects.get(id);
			if (object!=null) {
				final List<Object> listValue = getObjects(objects, namedList);
				setValue(object, namedList.getName(), listValue, List.class);
			}
		}
	    
	    nl = doc.getElementsByTagName("osgi:service");
	    if (nl!=null) for (int i = 0; i < nl.getLength(); i++) {
	    	
	    	Element service = (Element)nl.item(i);
			final String ref = service.getAttributes().getNamedItem("ref").getNodeValue();
	        final Object obj = objects.get(ref);
	        
			final String interfase = service.getAttributes().getNamedItem("interface").getNodeValue();
			final Bundle bundle    = getBundle("org.eclipse.scanning.api");
			final Class  clazz     = bundle!=null ? bundle.loadClass(interfase) : Class.forName(interfase);

			Activator.registerService(clazz, obj);
	    }
		return objects;

	}

	private Bundle getBundle(String bundleName) {
		if (context==null)    return null;
		if (bundleName==null) return null;
		BundleContext bcontext = context.getBundleContext();
		Bundle[] bundles = bcontext.getBundles();
		for (Bundle bundle : bundles) {
			if (bundleName.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}
		return getOSGiBundle(bundleName);
	}

	public Bundle getOSGiBundle(String symbolicName) {
		
		ServiceReference<PackageAdmin> ref = context.getBundleContext().getServiceReference(PackageAdmin.class);
		PackageAdmin packageAdmin = context.getBundleContext().getService(ref);
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private void parseBean(Element bean, Map<String, Object> objects, Map<String, NamedList> lists) throws Exception {
		
    	if (!bean.hasChildNodes()) return;
    	
		final String className = bean.getAttributes().getNamedItem("class").getNodeValue();
		
		Node initNode = bean.getAttributes().getNamedItem("init-method");
		final String init      = initNode!=null ? bean.getAttributes().getNamedItem("init-method").getNodeValue() : null;
		
		final String id = bean.getAttributes().getNamedItem("id").getNodeValue();

		// Look for parameters
		// bundle, broker, submitQueue, statusSet, statusTopic, durable;	
		NodeList props = bean.getElementsByTagName("property");
		final Map<String,Object> conf = new HashMap<>();
		for (int j = 0; j < props.getLength(); j++) {
			Node prop = props.item(j);
			String name = prop.getAttributes().getNamedItem("name").getNodeValue();
			Node value = prop.getAttributes().getNamedItem("value");
			Node ref = prop.getAttributes().getNamedItem("ref");
			if (value!=null) {
			    conf.put(name, value.getNodeValue());
			} else if (ref!=null && objects.containsKey(ref.getNodeValue())) {
				conf.put(name, objects.get(ref.getNodeValue()));
			} else {
				boolean useLists = true;
				NodeList children = prop.getChildNodes();
				final List<String> refs = new ArrayList<>();
				for (int k = 0; k < children.getLength(); k++) {
					Node list = children.item(k);
					if (!list.hasChildNodes()) continue;
					NodeList rs = list.getChildNodes();
					for (int l = 0; l < rs.getLength(); l++) {
						Node item = rs.item(l);
						NamedNodeMap attr = item.getAttributes();
						if (attr==null) continue;
						Node cbean  = attr.getNamedItem("bean");
						if (cbean!=null) {
						    refs.add(cbean.getNodeValue());
						    useLists = true;
						} else {
							refs.add(item.getTextContent());
							useLists = false;
						}
					}
				}
				if (useLists) {
					lists.put(id, new NamedList(name, refs));
				} else {
			        conf.put(name, refs);
				}
			}
		}
		Object created = createObject(className, init, conf);
		objects.put(id, created);
	}
	

	private List<Object> getObjects(Map<String, Object> objects, NamedList namedList) {
		final List<Object> ret = new ArrayList<>();
		for (String id : namedList.getRefs()) {
			ret.add(objects.get(id));
		}
		return ret;
	}

	private Object createObject(String className, String initMethod, Map<String, Object> conf) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		
		// Must have a bundle
		String bundleName = (String)conf.remove("bundle");
		if (bundleName==null) bundleName = "org.eclipse.scanning.server";
		final Bundle bundle    = getBundle(bundleName);
	
		Class<?> clazz;
		try {
			clazz = bundle != null ? bundle.loadClass(className) : Class.forName(className);
		} catch (java.lang.ClassNotFoundException ne) {
			throw new ClassNotFoundException("Cannot find class "+className+" in bundle "+bundleName+". Bundle is "+bundle, ne);
		}
		
		Object instance = clazz.newInstance();
		for (String fieldName : conf.keySet()) {
			final Object value      = getValue(conf, fieldName);
			setValue(clazz, instance, fieldName, value, null);
		}

		if (initMethod!=null) {
			Method method = clazz.getMethod(initMethod);
			method.invoke(instance);
			logger.debug("Created "+clazz.getName()+" using "+initMethod);
		} else {
			logger.debug("Created "+clazz.getName());
		}
		return instance;
	}

	private Object getValue(Map<String, Object> conf, String fieldName) {
		
		Object vObject = conf.get(fieldName);
		if (!(vObject instanceof String)) return vObject; // It was a ref!
		
		String val = replaceProperties((String)vObject); // Insert any system properties that the user used.
		
		if ("true".equalsIgnoreCase(val)) {
			return Boolean.TRUE;
		} else if ("false".equalsIgnoreCase(val)) {
			return Boolean.FALSE;
		} else {
			// There are faster ways to do this
			// but they are not required here...
			try {
				return Integer.parseInt(val);
			} catch (Exception ne) {
				try {
					return Double.parseDouble(val);
				} catch (Exception ignored) {
					// Legal
				}
			}
		}
		return val; // The String
	}

	/**
	 * Not very efficient but no dependencies required and does job.
	 * @param val
	 * @return
	 */
	private String replaceProperties(String val) {
		final Properties props = System.getProperties();
		for (Object name : props.keySet()) {
			val = val.replace("${"+name+"}", props.getProperty(name.toString()));
		}
		return val;
	}

	private void setValue(Object instance, final String fieldName, final Object value, final Class<?> valueClass) throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InvocationTargetException {
		setValue(instance.getClass(), instance, fieldName, value, valueClass);
	}
	
	private void setValue(final Class<?> clazz, Object instance, final String fieldName, Object value, Class<?> valueClass) throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InvocationTargetException {
		final String setterName = getSetterName(fieldName);
		if (valueClass==null) valueClass = value.getClass();
		Method method = getMethod(clazz, setterName, valueClass);
		try {
			if (method==null||instance==null||value==null) {
				System.err.println("Cannot find "+setterName+" in object "+instance+" to send value "+value);
			}
			if (value instanceof List) {
				Type argType = method.getGenericParameterTypes()[0];
				if (argType instanceof ParameterizedType) {
					if (((ParameterizedType) argType).getRawType().equals(Set.class)) {
						Type actualTypeArg = ((ParameterizedType) argType).getActualTypeArguments()[0];
						if (actualTypeArg instanceof Class && ((Class<?>) actualTypeArg).isEnum()) {
							Map<String, Enum<?>> enumMap =
									Arrays.stream(((Class<?>) actualTypeArg).getEnumConstants()).map(
											Enum.class::cast).collect(Collectors.toMap(Enum::name, Function.identity()));
							value = ((List<String>) value).stream().map(strVal -> enumMap.get(strVal)).collect(Collectors.toSet());
						} else {
							value = new HashSet<String>((List<String>) value);
						}
					}
				}
			}
		    method.invoke(instance, value);
		} catch (IllegalArgumentException are) {
			throw new IllegalArgumentException("Cannot set "+fieldName+" to "+value+" of class "+valueClass);
		}
	}

	private Method getMethod(Class<?> clazz, String setterName, Class<? extends Object> valueClass) throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
		try {
			return clazz.getMethod(setterName, valueClass);
		} catch (Exception ne) {
			try {
			    return clazz.getMethod(setterName, (Class<?>)valueClass.getField("TYPE").get(null));
			} catch (Exception neOther) {
				final Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if (method.getName().equals(setterName) && method.getParameterTypes().length==1) {
						return method;
					}
				}
			}
		}
        return null;
	}
	private String getSetterName(final String fieldName) {
		if (fieldName == null) return null;
		return getName("set", fieldName);
	}
	private String getName(final String prefix, final String fieldName) {
		return prefix + getFieldWithUpperCaseFirstLetter(fieldName);
	}
	public String getFieldWithUpperCaseFirstLetter(final String fieldName) {
		return fieldName.substring(0, 1).toUpperCase(Locale.US) + fieldName.substring(1);
	}

	public void start(ComponentContext context) {
		this.context = context;
	}

}

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
package org.eclipse.scanning.connector.epics.custommarshallers;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Structure;
import org.epics.pvmarshaller.marshaller.api.IPVStructureSerialiser;
import org.epics.pvmarshaller.marshaller.serialisers.Serialiser;
import org.python.core.PyArray;
import org.python.core.PyBoolean;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyUnicode;

/**
 * Custom serialiser for PyDictionary.
 * TODO - make this non 'test' and finalise custom serialisation strategy for PyDictionaries 
 * @author Matt Taylor
 *
 */
public class PyDictionarySerialiser implements IPVStructureSerialiser<PyDictionary> {

	@Override
	public Structure buildStructure(Serialiser serialiser, PyDictionary dictionary) throws Exception {
		// Convert to map first
		Map<String,?> dictionaryAsMap = convertMap(dictionary);
		Structure structure = serialiser.getMapSerialiser().buildStructureFromMap(dictionaryAsMap);
		return structure;
	}

	@Override
	public void populatePVStructure(Serialiser serialiser, PyDictionary dictionary, PVStructure pvStructure) throws Exception {
		Map<String,?> dictionaryAsMap = convertMap(dictionary);
		serialiser.getMapSerialiser().setMapValues(pvStructure, dictionaryAsMap);
	}
	
	private LinkedHashMap<String, Object> convertMap(Map<Object, Object> maptoCopy) {
		LinkedHashMap<String, Object> newObject = new LinkedHashMap<String, Object>();
		for (Object key : maptoCopy.keySet()) {
			String keyString = key.toString();
			newObject.put(keyString, convertPyObject(maptoCopy.get(key)));
		}
		return newObject;
	}
	
	/**
	 * Converts a python object into the raw java equivalent
	 * @param pyObj THe python object to convert
	 * @return the raw java equivalent of the python object
	 */
	private Object convertPyObject(Object pyObj) {
		if (pyObj == null) {
			return null;
		}
		if (pyObj instanceof PyList) {
			PyList pyList = (PyList)pyObj;
			
			if (pyList.size() == 1) {
				Object first = pyList.get(0);
				if (first.getClass().isArray()) {
					return convertPyObject(pyList.get(0));
				}
			} 
			Object[] array = pyList.toArray();
			LinkedList<Object> newList = new LinkedList<>();
			for (Object listElement : array) {
				newList.add(convertPyObject(listElement));
			}
			return newList;
		} else if (pyObj instanceof PyArray) {
			PyArray pyArray = (PyArray)pyObj;
			return convertPyObject(pyArray.tolist());
		} else if (pyObj instanceof PyDictionary) {
			PyDictionary pyDict = (PyDictionary) pyObj;
			LinkedHashMap<String, Object> newMap = convertMap(pyDict);
			return newMap;
		} else if (pyObj instanceof PyUnicode) {
			PyUnicode pyUnicode = (PyUnicode) pyObj;
			String newString = pyUnicode.getString();
			return newString;
		} else if (pyObj instanceof PyInteger) {
			PyInteger pyInteger = (PyInteger) pyObj;
			int newString = pyInteger.getValue();
			return newString;
		} else if (pyObj instanceof PyFloat) {
			PyFloat pyFloat = (PyFloat) pyObj;
			double newDouble = pyFloat.getValue();
			return newDouble;
		} else if (pyObj instanceof PyBoolean) {
			PyBoolean pyBoolean = (PyBoolean) pyObj;
			boolean newBoolean = pyBoolean.getBooleanValue();
			return newBoolean;
		} else if (pyObj instanceof PyString) {
			PyString pyString = (PyString) pyObj;
			String newString = pyString.getString();
			return newString;
		}
		
		if (pyObj.getClass().toString().contains("py")) {
			System.err.println("NOT CAUGHT [" + pyObj + "] class = " + pyObj.getClass());
		}
		
		return pyObj;
	}
	
}

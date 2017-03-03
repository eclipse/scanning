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
package org.eclipse.scanning.test.messaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Class to represent whether or not one JSON string is a subset of another JSON
 * string. The object will provide additional details that describe where the failure
 * was found.
 * 
 * @author Martin Gaughran
 *
 */
public class SubsetStatus {

	protected JsonFactory factory;
	protected ObjectMapper mapper;
	
	protected boolean isSubset = false;

	protected String message = "";
	protected String errorDescription = "";

	protected String expectedJson = "";
	protected String returnedJson = "";

	/**
	 * Library function that expects the returnedJson to contain the expectedJson.
	 * 
	 * @param failureMsg
	 * @param returnedJson
	 * @param expectedJson
	 * @throws Exception
	 */
	public static void assertJsonContains(String failureMsg, String returnedJson, String expectedJson) throws Exception {
		
	SubsetStatus status = new SubsetStatus(expectedJson, returnedJson);
	
	assertTrue(failureMsg + "\n" + status.getMessage(), status.isSubset());
	}

	/**
	 * Library function that expects the returnedJson to NOT contain the unexpectedJson.
	 * 
	 * @param failureMsg
	 * @param returnedJson
	 * @param nonExpectedJson
	 * @throws Exception
	 */
	public static void assertJsonDoesNotContain(String failureMsg, String returnedJson, String nonExpectedJson) throws Exception {
	
	SubsetStatus status = new SubsetStatus(nonExpectedJson, returnedJson);
	
	assertFalse(failureMsg, status.isSubset());
	}
	
	public SubsetStatus(String expectedJson, String returnedJson) throws Exception {
		
		this.expectedJson = expectedJson;
		this.returnedJson = returnedJson;
		
		// Used for JSON comparisons.
		mapper = new ObjectMapper();
		factory = mapper.getFactory();
		
		isSubset = isJsonSubset(expectedJson, returnedJson);
		
		message = compileMessage();
	}
	
	/**
	 * Determines the message to be printed to the user on failure.
	 * 
	 * @return
	 */
	protected String compileMessage() {
		
		if ("" == errorDescription) errorDescription = "Unknown error";
		
		String message = errorDescription;
		
		// Considered excessive!
		// message += "\nExpected JSON message:\n" + expectedJson + "\n Returned JSON message:\n" + returnedJson;
		
		return message;
	}

	/**
	 * Returns true if expected is a subset of returned
	 * 
	 * This is used for JSON serialiser comparisons.
	 * 
	 * @param expected
	 * @param returned
	 * @return
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws JsonParseException 
	 */
	protected boolean isJsonSubset(String expected, String returned) throws JsonParseException, JsonProcessingException, IOException {
		
		JsonNode expectedObj = mapper.readTree(factory.createParser(expected));
		JsonNode actualObj = mapper.readTree(factory.createParser(returned));
		
		return isJsonNodeSubset(expectedObj, actualObj);
	}
	
	/**
	 * Returns true if expected is a subset of returned
	 * 
	 * This is used for JSON serialiser comparisons. This is taken from
	 * the 'equals' definition of JsonNode's, but without the length check
	 * on the list of children nodes, plus location reporting.
	 * 
	 * @param expected
	 * @param returned
	 * @return
	 */
	protected boolean isJsonNodeSubset(JsonNode expected, JsonNode returned) {
		
	    if (returned == null) {
	    	errorDescription = "Returned value is null, expected JSON:\n" + expected.toString();
	    	return false;
	    }
	    if (returned == expected) return true;
	    
	    if (returned.getClass() != expected.getClass()) {
	    	errorDescription = "Returned value class is incorrect, expected JSON: " + expected.toString()
	    	+ ", returned JSON: " + returned.toString();
	    	return false;
	    }
	    
	    switch (expected.getNodeType()) {
	    	case ARRAY: 	return isArrayNodeSubset((ArrayNode)expected, (ArrayNode)returned);
	    	case OBJECT: 	return isObjectNodeSubset((ObjectNode)expected, (ObjectNode)returned);
	    	default: 		return isValueEqual((ValueNode)expected, (ValueNode)returned);	// Will be a ValueNode subclass
	    }
	}
	
	/**
	 * Returns true if expected is a subset of returned
	 * 
	 * This is used for JSON serialiser comparisons.
	 * 
	 * @param expected
	 * @param returned
	 * @return
	 */
	protected boolean isArrayNodeSubset(ArrayNode expected, ArrayNode returned) {
		
	    Iterator<JsonNode> expectedChildren = expected.elements();
	    Iterator<JsonNode> returnedChildren = returned.elements();
	    
		for (JsonNode en; expectedChildren.hasNext();) {
			en = expectedChildren.next();			
			
		    Boolean found = false;
			for (JsonNode en2; returnedChildren.hasNext();) {
				en2 = returnedChildren.next();
				if (isJsonNodeSubset(en, en2)) {
					found = true;
					
					// We want to be able to test for duplicates.
					returnedChildren.remove();
					break;
				}
			}
			if (!found) {
				// Can't use lower-down errors here, as I don't know which
				// item was the closest match.
				errorDescription = "Array does not contain expected value: " + en.toString();
				return false; 
			}
			
			// Reset iterator to beginning (with removed elements).
			returnedChildren = returned.elements();
	    }
		
		return true;
	}
	
	/**
	 * Returns true if expected is a subset of returned
	 * 
	 * This is used for JSON serialiser comparisons.
	 * 
	 * @param expected
	 * @param returned
	 * @return
	 */
	protected boolean isObjectNodeSubset(ObjectNode expected, ObjectNode returned) {
		
	    Iterator<Entry<String, JsonNode>> expectedChildren = expected.fields();
	    
		for (Map.Entry<String, JsonNode> en; expectedChildren.hasNext();) {
			en = expectedChildren.next();
	        String key = en.getKey();
	        JsonNode value = en.getValue();
	
	        JsonNode returnedValue = returned.get(key);
	
	        if (returnedValue == null) {
	        	errorDescription = "Returned JSON does not have key '" + key +"', with expected value:\n" + value.toString();
	        	return false;
	        } else if (!isJsonNodeSubset(value, returnedValue)) {
	        	return false;
	        }
		}
	    return true;
	}
	
	protected boolean isValueEqual(ValueNode expected, ValueNode returned) {
		boolean result = returned.equals(expected);
		if (!result) {
			errorDescription = "Expected value: '" + expected.toString() + "', returned value: '" + returned.toString() + "' are not equal";
		}
		return result;
	}
	
	public boolean isSubset() {
		return this.isSubset;
	}
	
	public String getMessage() {
		return this.message;
	}
}		
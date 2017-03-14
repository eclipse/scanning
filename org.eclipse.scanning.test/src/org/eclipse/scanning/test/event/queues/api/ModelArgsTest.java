package org.eclipse.scanning.test.event.queues.api;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueVariable;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueListVariable;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueTableVariable;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.junit.Before;
import org.junit.Test;

public class ModelArgsTest {
	
	private List<Double> simpleList;
	private Map<String, Double> simpleTable;
	private Map<String, List<Double>> complexTable;
	
	@Before
	public void setUp() {
		simpleList = Arrays.asList(88., -7.9, 20.356);
		
		simpleTable = new HashMap<>();
		simpleTable.put("111", 34.);
		simpleTable.put("211", 88.);
		simpleTable.put("311", 100.);
		
		complexTable = new HashMap<>();
		complexTable.put("111", Arrays.asList(34., -5.4));
		complexTable.put("211", Arrays.asList(88., -7.9));
		complexTable.put("311", Arrays.asList(100., 1.6));
	}
	
	@Test
	public void testSimpleArgument() {		
		IQueueValue<Double> argumA = new QueueValue<>(12.);
		argumA.evaluate();
		assertEquals("Arg has wrong value", 12., argumA.evaluate(), 0);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) //This is what I'm testing
	@Test
	public void testCreateArgumentWithoutValue() {
		QueueValue argumAA = new QueueValue(String.class);
		argumAA.setValue("Fish");
		argumAA.setValue(5);
	}
	
	@Test
	public void testListArgument() {
		//Return value-by-index from Arg
		IQueueVariable<Integer, Double> argumD = new QueueListVariable<>(new QueueValue<>(0), new QueueValue<>(simpleList));
		assertEquals("ArrayArg has wrong value at index 0", 88., argumD.evaluate(), 0);
		
		//Return value-by-index from function
		argumD.setArg(new QueueValue<>(1));
		assertEquals("ArrayArg has wrong value at index 1", -7.9, argumD.evaluate(), 0);
		argumD.setArg(new QueueValue<>(2));
		assertEquals("ArrayArg has wrong value at index 2", 20.356, argumD.evaluate(), 0);
	}
	
	@Test
	public void testLookupArgument() {
		//First a simple lookup to return a single value
		IQueueValue<Double> argumB = new QueueTableVariable<>(new QueueValue<>("311"), new QueueValue<>(simpleTable));
		assertEquals("LookupArg has wrong value for string 311", 100., argumB.evaluate(), 0);
		
		//Second a slightly more complicated one: return an array
		IQueueValue<List<Double>> argumC = new QueueTableVariable<>(new QueueValue<>("311"), new QueueValue<>(complexTable));
		assertEquals("LookupArg has wrong value for string 311", Arrays.asList(100., 1.6), argumC.evaluate());
	}
	
	@Test
	public void testQueueValueStacking() {
		QueueValue<String> baseArg = new QueueValue<>("211");
		IQueueVariable<Integer, Double> argumF = new QueueListVariable<>(new QueueValue<>(1),new QueueTableVariable<>(baseArg, new QueueValue<>(complexTable)));
		assertEquals("Decorated argument has wrong value for string 211, index 1", -7.9, argumF.evaluate(), 0);
		
		baseArg.setValue("111");
		assertEquals("Decorated argument has wrong value for string 111, index 1", -5.4, argumF.evaluate(), 0);
	}

}

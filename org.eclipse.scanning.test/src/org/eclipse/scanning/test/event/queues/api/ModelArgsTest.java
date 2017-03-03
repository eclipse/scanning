package org.eclipse.scanning.test.event.queues.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.Arg;
import org.eclipse.scanning.api.event.queues.models.arguments.ArrayArg;
import org.eclipse.scanning.api.event.queues.models.arguments.IArg;
import org.eclipse.scanning.api.event.queues.models.arguments.LookupArg;
import org.junit.Before;
import org.junit.Test;

public class ModelArgsTest {
	
	private Double[] simpleArray;
	private Map<String, Double> simpleTable;
	private Map<String, Double[]> complexTable;
	
	@Before
	public void setUp() {
		simpleArray = new Double[]{88., -7.9, 20.356};
		
		simpleTable = new HashMap<>();
		simpleTable.put("111", 34.);
		simpleTable.put("211", 88.);
		simpleTable.put("311", 100.);
		
		complexTable = new HashMap<>();
		complexTable.put("111", new Double[]{34., -5.4});
		complexTable.put("211", new Double[]{88., -7.9});
		complexTable.put("311", new Double[]{100., 1.6});
	}
	
	@Test
	public void testSimpleArgument() {		
		IArg argumA = new Arg(12.);
		argumA.evaluate();
		assertEquals("Arg has wrong value", 12., argumA.getValue());
	}
	
	@Test
	public void testArrayArgument() {
		//Return value-by-index from Arg
		Arg baseArg = new Arg(0);
		ArrayArg argumD = new ArrayArg(baseArg, simpleArray);
		argumD.evaluate();
		assertEquals("ArrayArg has wrong value at index 0", 88., argumD.getValue());
		
		//Return value-by-index from function
		assertEquals("ArrayArg has wrong value at index 1", -7.9, argumD.index(1));
		assertEquals("ArrayArg has wrong value at index 2", 20.356, argumD.index(2));
	}
	
	@Test
	public void testLookupArgument() {
		//First a simple lookup to return a single value
		Arg baseArg = new Arg<>("311");
		IArg argumB = new LookupArg(baseArg, simpleTable);
		argumB.evaluate();
		assertEquals("LookupArg has wrong value for string 311", 100., argumB.getValue());
		
		//Second a slightly more complicated one: return an array
		IArg<Double[]> argumC = new LookupArg(baseArg, complexTable);
		argumC.evaluate();
		assertArrayEquals("LookupArg has wrong value for string 311", new Double[]{100., 1.6}, argumC.getValue());
	}
	
	@Test
	public void testDecoratedLookup() {
		Arg baseArg = new Arg<>("211");
		ArrayArg argumF = new ArrayArg(new LookupArg(baseArg, complexTable));
		assertEquals("Decorated argument has wrong value for string 211, index 1", -7.9, argumF.index(1));
		
		baseArg = new Arg<>("111");
		ArrayArg argumG = new ArrayArg(new LookupArg(baseArg, complexTable), 1);
		argumG.evaluate();
		assertEquals("Decorated argument has wrong value for string 111, index 1", -5.4, argumG.getValue());
		
		baseArg = new Arg<>("111");
		argumG = new ArrayArg(new LookupArg(baseArg, complexTable));
		try {
			argumG.evaluate();
		} catch (QueueModelException qme) {
			//Expected. If the user doesn't supply an index, the evaluation will fail.
		}
		
	}

}

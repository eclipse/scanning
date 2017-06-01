package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertEquals;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ToleranceTest {

	private static IRunnableDeviceService  dservice;
	private static IScannableDeviceService connector;

	@BeforeClass
	public static void before() {
		connector = new MockScannableConnector(null);
		dservice  = new RunnableDeviceServiceImpl(connector);
		org.eclipse.scanning.example.Services.setScannableDeviceService(connector);
	}
	
    @Before
	public void beforeTest() throws Exception {
    	
		// Make a few detectors and models...
		PseudoSpringParser parser = new PseudoSpringParser();
		parser.parse(ToleranceTest.class.getResourceAsStream("test_tolerance.xml"));
	}
	
	@Test
	public void testMoveNoTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
        assertEquals(new Double(20d), a.getPosition());
	}
	
	@Test
	public void testMoveWithTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
		
        assertEquals(new Double(20d), a.getPosition());
	}

	@Test
	public void testMoveEdgeOfTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:21.0"));
		
        assertEquals(new Double(21d), a.getPosition());
	}
	
	@Test
	public void testOutsideTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:22.4"));
		
        assertEquals(new Double(22.4), a.getPosition());
	}

	@Test
	public void testChangeTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20d), a.getPosition());
		a.setTolerance(0.5d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20.5d), a.getPosition());
	}
	
	@Test
	public void testSetToleranceNull() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20d), a.getPosition());
		a.setTolerance(null);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20.5d), a.getPosition());
	}

	@Test
	public void testSpringConfigurationValue() throws Exception {
		
		// Something without 
		IScannable<Double> bnd   = connector.getScannable("bnd");        
        assertEquals(new Double(3.14), bnd.getPosition());
	}

	@Test
	public void testSpringConfigurationTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> bnd   = connector.getScannable("bnd");        
        assertEquals(new Double(1.0), bnd.getTolerance());
	}

	@Test
	public void testSpringMoveWithTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> bnd   = connector.getScannable("bnd");        
        assertEquals(new Double(1.0), bnd.getTolerance());
        
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("bnd:0:3.14"));
        assertEquals(new Double(3.14), bnd.getPosition());
      
        pos.setPosition(new MapPosition("bnd:0:3.5"));
		
        assertEquals(new Double(3.14), bnd.getPosition());

        pos.setPosition(new MapPosition("bnd:0:4.15"));
        assertEquals(new Double(4.15), bnd.getPosition());
        
        pos.setPosition(new MapPosition("bnd:0:3.14"));
        assertEquals(new Double(3.14), bnd.getPosition());

	}

	@Test
	public void testSpringMoveWithToleranceSetToZero() throws Exception {
		
		// Something without 
		IScannable<Double> bnd   = connector.getScannable("bnd");        
        assertEquals(new Double(1.0), bnd.getTolerance());
        
        bnd.setTolerance(0d);
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("bnd:0:3.14"));
        assertEquals(new Double(3.14), bnd.getPosition());
      
        pos.setPosition(new MapPosition("bnd:0:3.5"));
		
        assertEquals(new Double(3.5), bnd.getPosition());
        
        pos.setPosition(new MapPosition("bnd:0:3.14"));
        bnd.setTolerance(1.0d);

	}

}

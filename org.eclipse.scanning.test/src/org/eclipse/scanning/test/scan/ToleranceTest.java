package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertEquals;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class ToleranceTest {

	private static IRunnableDeviceService  dservice;
	private static IScannableDeviceService connector;

	@BeforeClass
	public static void before() {
		connector = new MockScannableConnector(null);
		dservice  = new RunnableDeviceServiceImpl(connector);
	}
	
	@Test
	public void testMoveNoTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
        assertEquals(new Double(20d), (Double)a.getPosition());
	}
	
	@Test
	public void testMoveWithTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
		
        assertEquals(new Double(20d), (Double)a.getPosition());
	}

	@Test
	public void testMoveEdgeOfTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:21.0"));
		
        assertEquals(new Double(21d), (Double)a.getPosition());
	}
	
	@Test
	public void testOutsideTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:22.4"));
		
        assertEquals(new Double(22.4), (Double)a.getPosition());
	}

	@Test
	public void testChangeTolerance() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20d), (Double)a.getPosition());
		a.setTolerance(0.5d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20.5d), (Double)a.getPosition());
	}
	
	@Test
	public void testSetToleranceNull() throws Exception {
		
		// Something without 
		IScannable<Double> a   = connector.getScannable("a");
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("a:0:20"));
        
		a.setTolerance(1d);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20d), (Double)a.getPosition());
		a.setTolerance(null);
        pos.setPosition(new MapPosition("a:0:20.5"));
        assertEquals(new Double(20.5d), (Double)a.getPosition());
	}

}

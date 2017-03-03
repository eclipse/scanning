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
package org.eclipse.scanning.example.scannable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.Location;
import org.eclipse.scanning.example.Services;

public class MockScannableConnector implements IScannableDeviceService, IDisconnectable {

	protected String               broker;
	private Map<String, INameable> cache;
	private IPublisher<Location>   positionPublisher;
	
	private boolean createIfNotThere = true;
	
	// Spring
	public MockScannableConnector() {
		// Called by Spring.
	}
	
	// Spring
	public void connect() throws URISyntaxException {
		IEventService eservice = Services.getEventService();
		this.positionPublisher = eservice.createPublisher(new URI(broker), EventConstants.POSITION_TOPIC);
        createMockObjects();
	}

	// Test decks
	public MockScannableConnector(IPublisher<Location> positionPublisher) {
		this.positionPublisher = positionPublisher;
        createMockObjects();
	}
	
	@Override
	public <T> void register(IScannable<T> mockScannable) {
		cache.put(mockScannable.getName(), mockScannable);
		if (mockScannable instanceof AbstractScannable) {
			((AbstractScannable)mockScannable).setPublisher(positionPublisher);
		}
	}

	/**
	 * Makes a bunch of things that the tests and example user interface connect to.
	 */
	private void createMockObjects() {
		
		if (cache==null) cache = new HashMap<String, INameable>(3);
		register(new MockPausingMonitor("pauser", 10d,  -1));
		register(new MockTopupScannable("topup", 1000));
		register(new MockScannable("beamcurrent", 5d,  1, "mA"));
		register(new MockStringScannable("portshutter", "Open", new String[]{"Open", "Closed", "Error"}));
		
		register(new MockScannable("period", 1000d, 1, "ms"));
		register(new MockBeamOnMonitor("beamon", 10d, 1));
		register(new MockScannable("bpos",  0.001,  -1));
		
		MockScannable a = new MockScannable("a", 10d, 1, "mm");
		a.setActivated(true);
		register(a);
		register(new MockScannable("b", 10d, 1, "mm"));
		register(new MockScannable("c", 10d, 1, "mm"));
		
		MockScannable p = new MockScannable("p", 10d, 2, "µm");
		p.setActivated(true);
		register(p);
		register(new MockScannable("q", 10d, 2, "µm"));
		register(new MockScannable("r", 10d, 2, "µm"));
		
		MockScannable x = new MockNeXusScannable("x", 0d,  3, "mm");
		x.setRealisticMove(true);
		x.setRequireSleep(false);
		x.setMoveRate(10000); // ï¿½m/s or 1 cm/s
		register(x);
		
		MockScannable y = new MockNeXusScannable("y", 0d,  3, "mm");
		y.setRealisticMove(true);
		y.setMoveRate(100); // ï¿½m/s, faster than real?
		register(y);
		
		x = new MockNeXusScannable("stage_x", 0d,  3, "mm");
		x.setRealisticMove(true);
		x.setRequireSleep(false);
		x.setMoveRate(10000); // ï¿½m/s or 1 cm/s
		register(x);
		
		y = new MockNeXusScannable("stage_y", 0d,  3, "mm");
		y.setRealisticMove(true);
		y.setMoveRate(100); // ï¿½m/s, faster than real?
		register(y);

		
		register(new MockNeXusScannable("z", 2d,  3, "mm"));
		register(new MockNeXusScannable("stage_z", 2d,  3, "mm"));
		register(new MockNeXusScannable("xNex", 0d,  3, "mm"));
		register(new MockNeXusScannable("yNex", 0d,  3, "mm"));
		register(new MockScannable("benchmark1",  0.0,  -1, false));
		register(new MockScannable("myScannable",  0.0,  -1, false));
		
		MockNeXusScannable temp= new MockNeXusScannable("T", 295,  3, "K");
		temp.setRealisticMove(true);
		String srate = System.getProperty("org.eclipse.scanning.example.temperatureRate");
		if (srate==null) srate = "10.0";
		temp.setMoveRate(Double.valueOf(srate)); // K/s much faster than real but device used in tests.
		register(temp);
		
		temp= new MockNeXusScannable("temp", 295,  3, "K");
		temp.setRealisticMove(false);
		temp.setRequireSleep(false);
		register(temp);
	
		for (int i = 0; i < 10; i++) {
			MockScannable t = new MockScannable("T"+i, 0d,  0, "K");
			t.setRequireSleep(false);
			register(t);

		}
		for (int i = 0; i < 10; i++) {
			register(new MockNeXusScannable("neXusScannable"+i, 0d,  3));
	    }
		for (int i = 0; i < 10; i++) {
			MockNeXusScannable mon = new MockNeXusScannable("monitor"+i, 0d,  3);
			mon.setActivated(i%3==0);
			register(mon);
	    }
		for (int i = 0; i < 10; i++) {
			MockNeXusScannable metadataScannable = new MockNeXusScannable("metadataScannable"+i, 0d, 3);
			metadataScannable.setMonitorRole(MonitorRole.PER_SCAN);
			metadataScannable.setInitialPosition(i * 10.0);
			register(metadataScannable);
		}
	}

	public void register(INameable mockScannable) {
		cache.put(mockScannable.getName(), mockScannable);
		if (mockScannable instanceof AbstractScannable) {
			((AbstractScannable)mockScannable).setPublisher(positionPublisher);
		}
	}

	@Override
	public <T> IScannable<T> getScannable(String name) throws ScanningException {
		
		if (name==null) throw new ScanningException("Invalid scannable "+name);
		if (cache==null) cache = new HashMap<String, INameable>(3);
		if (cache.containsKey(name)) return (IScannable<T>)cache.get(name);
		if (createIfNotThere) {
			register(new MockScannable(name, 0d));
			return (IScannable<T>)cache.get(name);
		} 
		return null;
	}


	@Override
	public List<String> getScannableNames() throws ScanningException {
		return cache.keySet().stream().filter(key -> cache.get(key) instanceof IScannable).collect(Collectors.toList());
	}

	@Override
	public void disconnect() throws EventException {
		if (positionPublisher!=null) positionPublisher.disconnect();
		if (cache!=null && !cache.isEmpty()) {
			INameable[] devices = cache.values().toArray(new INameable[cache.size()]);
			for (INameable device : devices) {
				if (device instanceof IDisconnectable) ((IDisconnectable)device).disconnect();
			}
			cache.clear();
		}
	}
	
	@Override
	public boolean isDisconnected() {
		if (positionPublisher!=null) return positionPublisher.isDisconnected(); 
		return true;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	public boolean isCreateIfNotThere() {
		return createIfNotThere;
	}

	public void setCreateIfNotThere(boolean createIfNotThere) {
		this.createIfNotThere = createIfNotThere;
	}

}

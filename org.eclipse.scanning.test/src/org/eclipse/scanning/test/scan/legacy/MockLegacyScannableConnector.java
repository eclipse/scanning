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
package org.eclipse.scanning.test.scan.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.example.scannable.MockNeXusScannable;
import org.eclipse.scanning.example.scannable.MockScannable;

public class MockLegacyScannableConnector implements IScannableDeviceService {
	
	private static Map<String, INameable> cache; 
	
	private static Set<String> globalMetadataScannableNames;
	
	private static Map<String, Set<String>> requiredMetadataScannableNames;
	
	static {
		System.out.println("Starting up MockLegacyScannableConnector");
		cache = new HashMap<String, INameable>(3);
		for (int i = 0; i < 10; i++) {
			put(new MockNeXusScannable("neXusScannable"+i, 0d,  3));
	    }
		for (int i = 0; i < 10; i++) {
			put(new MockNeXusScannable("monitor"+i, 0d,  3));
	    }
		String[] metadataScannableNames =
				new String[] { "a", "b", "c", "d", "e", "f", "g", "h",
						"p", "q", "r", "s", "t", "u", "v", "x", "y", "z" };
		for(String metadataScannableName : metadataScannableNames) {
			put(new MockNeXusScannable(metadataScannableName, 0d,  3));
		}
		// the global metadata scannables to be automatically added to scans
		globalMetadataScannableNames = new HashSet<>(Arrays.asList("a", "b", "c"));
		
		requiredMetadataScannableNames = new HashMap<>();
		requiredMetadataScannableNames.put("neXusScannable1", new HashSet<>(Arrays.asList("x")));
		requiredMetadataScannableNames.put("neXusScannable2", new HashSet<>(Arrays.asList("p")));
		requiredMetadataScannableNames.put("x", new HashSet<>(Arrays.asList("y", "z")));
		requiredMetadataScannableNames.put("p", new HashSet<>(Arrays.asList("q", "r")));
		requiredMetadataScannableNames.put("c", new HashSet<>(Arrays.asList("d", "e", "f")));
	}
	
	private static void put(INameable mockScannable) {
		cache.put(mockScannable.getName(), mockScannable);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IScannable<T> getScannable(String name) throws ScanningException {
		if (cache==null) cache = new HashMap<String, INameable>(3);
		if (cache.containsKey(name)) return (IScannable<T>)cache.get(name);
		register(new MockScannable(name, 0d));
		return (IScannable<T>)cache.get(name);
	}
	
	@Override
	public List<String> getScannableNames() throws ScanningException {
		return cache.keySet().stream().filter(key -> cache.get(key) instanceof IScannable).collect(Collectors.toList());
	}

	@Override
	public Set<String> getGlobalMetadataScannableNames() {
		return globalMetadataScannableNames;
	}

	@Override
	public Set<String> getRequiredMetadataScannableNames(String scannableName) {
		return requiredMetadataScannableNames.getOrDefault(scannableName, Collections.emptySet());
	}

	@Override
	public <T> void register(IScannable<T> scannable) {
		put(scannable);
	}
	
}

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
package org.eclipse.scanning.test.scan.nexus;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({

	AttributeTest.class,
	BasicScanTest.class,
	MetadataScannableTest.class,
	ConstantVelocityTest.class,
	DarkCurrentTest.class,
	MalcolmScanTest.class,
	MandelbrotAcquireTest.class,
	MandelbrotExampleTest.class,
	MandelbrotRemoteTest.class,
	MonitorTest.class,
	ScanMetadataTest.class,
	ScanProcessingTest.class,
	ScanClusterProcessingTest.class,
	SolsticeScanMonitorTest.class,
	LinearScanTest.class,
	NexusStepScanSpeedTest.class

})
public class Suite {
}
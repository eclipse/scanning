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
package org.eclipse.scanning.test.malcolm;
import org.eclipse.scanning.test.malcolm.mock.MockAbortingMalcolmTest;
import org.eclipse.scanning.test.malcolm.mock.MockCommunicationMalcolmTest;
import org.eclipse.scanning.test.malcolm.mock.MockMultipleClientTest;
import org.eclipse.scanning.test.malcolm.mock.MockPausingMalcolmTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({
	MockAbortingMalcolmTest.class,
	MockCommunicationMalcolmTest.class,
	MockMultipleClientTest.class,
	MockPausingMalcolmTest.class
})
public class Suite {

}

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
package org.eclipse.scanning.api.annotation.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotation methods as participating at a location in a scan.
 * <p>
 * It is possible using annotations to have more than one method annotated
 * which means a super class can declare its implementation as final, requiring a 
 * subclass to define another annotation.
 * <p>
 * Services such as IRunnableDeviceService, IPointsGenerator etc may be declared
 * in the method which we annotate. These will be passed in if am implementation
 * of them can be found or null with the method still called (which then will often
 * cause an NPE causing the scan to abort normally).
 * <p>
 * If the information about the scan is required (size, shape etc.) then the class
 * ScanInformation may be received by the annotated method. If the current position is
 * needed then the IPosition for it should be declared at the pointStart.
 * <p>
 * Examples:<p>
 * <code><pre>
 * public class Fred implements IScannable {
 *     &#64;ScanStart
 *     public final void prepareVoltages() throws Exception {
 *        ...
 *     }
 *     &#64;ScanEnd
 *     public void dispose() {
 *        ...
 *     }
 *     ...
 * }
 * public class Bill extends Fred {
 *     &#64;ScanStart
 *     public void moveToNonObstructingLocation(IRunnableDeviceService<?> rservice) throws Exception {
 *        ...
 *     }
 *     &#64;PointStart
 *     public void checkNextMoveLegal(IPosition pos) throws Exception {
 *        ...
 *     }
 *     &#64;PointStart
 *     public void notifyPosition(IPosition pos) throws Exception {
 *        ...
 *     }
 *     &#64;PointEnd
 *     public void deleteLocation() {
 *        ...
 *     }
 *     &#64;Override
 *     &#64;ScanEnd
 *     public void dispose() {
 *        super.dispose();
 *        ....
 *     }
 *}
 * </pre></code>
 * 
 * <p>
 * @author Matthew Gerring
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScanStart {

}

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
package org.eclipse.scanning.api.scan;

/**
 * 
 * Any class wishing to have annotated methods called during
 * a scan may implement this interface and then either:
 *  <p>
 * &bull; 1. Register the class with IRunnableDeviceService.register(IScanParticipant) <br>
 * &bull; 2. Export the class as an OSGi service. The annotation manager will look for any service <br>
 * implementing IScanParticipant
 *  <p>
 * The Scanning System will get all objects declared as implementing
 * this interface from OSGi and include the devices in the scan.
 * Any methods then implemented in the device will be called where they
 * are annotated as the scan expects.
 *  <p>
 * One example of this is archiving. The archiver implements IScanParticipant
 * and is then included in the scan. As the files are declared as being part
 * of the scan, the @FileDeclared annotation is called, at the end of the
 * scan @ScanEnd is called, finally @ScanFinally is called to close any resources
 * or reset the file list because there is no more a scan!
 * <p>
 * 
 * An example of the IScanParticipant is the FileRegistrar for archiving in GDA
 * This has a register method which gets the IScanService and adds the FileRegistrar
 * as an IScanParticipant. The annotations in FileRegistrar are then parsed and it
 * does the archiving.
 * <pre>
        {@literal <bean id="FileRegistrar" class="gda.data.fileregistrar.FileRegistrar" init-method="register">}
        {@literal         <property name="name" value="FileRegistrar" />}
        {@literal         <property name="directory" value="/dls/bl-misc/dropfiles2/icat/dropZone/}${gda.instrument}-" />
        {@literal </bean>}

 *  </pre>
 *  <p>
 * @author Matthew Gerring
 *
 */
public interface IScanParticipant {

}

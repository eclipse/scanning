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
package org.eclipse.scanning.command.factory;

import java.util.Collection;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.command.PyExpressionNotImplementedException;

class ROICollectionExpresser extends PyModelExpresser<Collection<IROI>> {

	final String pyExpress(Collection<IROI> rois, boolean verbose) throws Exception {

		if (rois.size() == 0)
			throw new PyExpressionNotImplementedException();

		else if (rois.size() == 1 && !verbose)
			return factory.pyExpress(rois.iterator().next(), verbose);

		else {
			String fragment = "[";
			boolean listPartiallyWritten = false;

			for (IROI roi : rois) {
				if (listPartiallyWritten) fragment += ", ";
				fragment += factory.pyExpress(roi, verbose);
				listPartiallyWritten |= true;
			}

			fragment += "]";
			return fragment;
		}
	}
}

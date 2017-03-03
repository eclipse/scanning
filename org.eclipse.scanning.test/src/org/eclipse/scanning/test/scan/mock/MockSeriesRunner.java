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
package org.eclipse.scanning.test.scan.mock;

import java.io.Serializable;

import org.eclipse.dawnsci.analysis.api.processing.ExecutionType;
import org.eclipse.dawnsci.analysis.api.processing.IExecutionVisitor;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationRunner;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.dawnsci.analysis.api.processing.OperationException;
import org.eclipse.dawnsci.analysis.dataset.slicer.ISliceViewIterator;
import org.eclipse.dawnsci.analysis.dataset.slicer.SliceFromSeriesMetadata;
import org.eclipse.dawnsci.analysis.dataset.slicer.SliceVisitor;
import org.eclipse.dawnsci.analysis.dataset.slicer.Slicer;
import org.eclipse.dawnsci.analysis.dataset.slicer.SourceInformation;
import org.eclipse.january.dataset.IDataset;

/**
 * Runs a pipeline by looping the services of operations.
 * 
 * This will not work unless the pipeline is a linear series of operations 
 * with one slug running the length.
 * 
 * If you have averaging or branching, you will need to consider using a 
 * graph to execute your operations.
 * 
 * @author Matthew Gerring
 *
 */
public class MockSeriesRunner implements IOperationRunner {


	private IOperationContext context;

	public void init(IOperationContext context) {
		this.context        = context;
	}

	@Override
	public void execute() throws Exception {
		final IExecutionVisitor visitor = context.getVisitor() ==null ? new IExecutionVisitor.Stub() : context.getVisitor();

		// determine data axes to populate origin metadata
		SourceInformation ssource = null; 
		
		try {
			 ssource = context.getData().getMetadata(SliceFromSeriesMetadata.class).get(0).getSourceInfo();
		} catch (Exception e) {
			logger.error("Source not obtainable. Hope this is just a unit test...");
		}
		
		final SourceInformation finalSource = ssource;
		
		// Create the slice visitor
		SliceVisitor sv = new SliceVisitor() {

			@Override
			public void visit(IDataset slice) throws Exception {

				OperationData  data = new OperationData(slice, (Serializable[])null);
				long start = System.currentTimeMillis();
				for (IOperation<?,?> i : context.getSeries()) {
					
					OperationData tmp = i.execute(data.getData(), context.getMonitor());
					//TODO only set metadata if doesnt already contain it!
					//TODO continue if null;
					
					if (tmp == null) {
						data = null;
						break;
					}
					visitor.notify(i, tmp); // Optionally send intermediate result
					data = i.isPassUnmodifiedData() ? data : tmp;
				}
				if (context.getMonitor() != null) context.getMonitor().worked(1);
				visitor.executed(data, context.getMonitor()); // Send result.
			}

			@Override
			public boolean isCancelled() {
				return context.getMonitor()!=null ? context.getMonitor().isCancelled() : false;
			}
		};

		visitor.init(context.getSeries(), context.getData());
		long start = System.currentTimeMillis();
		
		
		ISliceViewIterator iterator = null;
		
		iterator = new MockSliceViewIterator(context.getData(), context.getSlicing(), context.getDataDimensions());
		
		if (context.getExecutionType()==ExecutionType.SERIES) {
			Slicer.visit(iterator,sv);
		} else if (context.getExecutionType()==ExecutionType.PARALLEL) {
			Slicer.visitParallel(iterator,sv);
		} else {
			throw new OperationException(context.getSeries()[0], "The edges are needed to execute a graph using ptolemy!");
		}
		logger.debug("Data ran in: " +(System.currentTimeMillis()-start)/1000. + " s");
		
	}

	@Override
	public ExecutionType[] getExecutionTypes() {
		return new ExecutionType[]{ExecutionType.SERIES, ExecutionType.PARALLEL};
	}

}

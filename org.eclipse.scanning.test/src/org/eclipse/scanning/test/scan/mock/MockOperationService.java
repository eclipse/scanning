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

import java.util.Collection;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationBean;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.api.processing.InvalidRankException;
import org.eclipse.dawnsci.analysis.api.processing.OperationCategory;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.dawnsci.analysis.api.processing.OperationException;
import org.eclipse.dawnsci.analysis.api.processing.OperationRank;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.january.dataset.IDataset;

public class MockOperationService implements IOperationService {

	@Override
	public IOperationContext createContext() {
		return new MockOperationContextImpl();
	}

	@Override
	public void execute(IOperationContext context) throws OperationException {
		
		try {
			for (IOperation op : context.getSeries()) op.init();
			MockSeriesRunner runner = new MockSeriesRunner();
			runner.init(context);
			runner.execute();
			
		} catch (OperationException o) {
			throw o;
		} catch (Exception e) {
			e.printStackTrace();
			throw new OperationException(null, e);
		} finally {
			if (context.getVisitor() != null) {
				try {
					context.getVisitor().close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			for (IOperation op : context.getSeries()) {
				op.dispose();
			}
		}
	}

	@Override
	public String getName(String id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription(String id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<IOperation<? extends IOperationModel, ? extends OperationData>> find(String operationRegex)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<IOperation<? extends IOperationModel, ? extends OperationData>> find(OperationRank rank,
			boolean isInput) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IOperation<? extends IOperationModel, ? extends OperationData> findFirst(String operationRegex) throws Exception {
		if (!operationRegex.matches("subtractOperation")) throw new Exception("Cannot find operation matching "+operationRegex+" in "+getClass().getSimpleName());
		return new MockSubtractOperation();
	}

	@Override
	public Collection<String> getRegisteredOperations() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Collection<IOperation<? extends IOperationModel, ? extends OperationData>>> getCategorizedOperations()
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IOperation<? extends IOperationModel, ? extends OperationData> create(String operationId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends IOperationModel> getModelClass(String operationId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void validate(IDataset firstSlice, IOperation<? extends IOperationModel, ? extends OperationData>... series)
			throws InvalidRankException, OperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void createOperations(ClassLoader l, String pakage) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public OperationCategory getCategory(String operationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IOperationBean createBean() {
		return new DummyOperationBean();
	}

}

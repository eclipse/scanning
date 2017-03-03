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
package org.eclipse.scanning.sequencer.expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.Script;
import org.eclipse.dawnsci.analysis.api.expressions.ExpressionEngineEvent;
import org.eclipse.dawnsci.analysis.api.expressions.IExpressionEngine;
import org.eclipse.dawnsci.analysis.api.expressions.IExpressionEngineListener;
import org.eclipse.january.IMonitor;

public class VanillaExpressionEngine implements IExpressionEngine{
	
	private JexlEngine jexl;
	private Expression expression;
	private MapContext context;
	private HashSet<IExpressionEngineListener> expressionListeners;
	private Callable<Object> callable;
	
	public VanillaExpressionEngine() {
		//Create the Jexl engine with the DatasetArthmetic object to allows basic
		//mathematical calculations to be performed on Datasets
		jexl = new JexlEngine();
		
		expressionListeners = new HashSet<IExpressionEngineListener>();
	}

	@Override
	public void createExpression(String expr) throws Exception {
		this.expression = jexl.createExpression(expr);
		
		checkFunctions(expr);
	}

	/**
	 * TODO FIXME Must be better way than this...
	 * @param expr
	 * @throws Exception
	 */
	private void checkFunctions(String expr)  throws Exception {
		
		// We do not support the . operator for now because
		// otherwise http://jira.diamond.ac.uk/browse/SCI-1731
		//if  (expr.indexOf('.')>-1) {
		//	throw new Exception("The dot operator '.' is not supported.");
		//}

		// We now evaluate the expression to try and trap invalid functions.
		try {
			
			final Script script = jexl.createScript(expr);
			Set<List<String>> names = script.getVariables();
			Collection<String> vars = unpack(names);
			
			final Map<String,Object> dummy = new HashMap<String,Object>(vars.size());
			for (String name : vars) dummy.put(name, 1);
			MapContext dCnxt = new MapContext(dummy);
			
			expression.evaluate(dCnxt);
			
		} catch (JexlException ne) {
			if (ne.getMessage().toLowerCase().contains("no such function namespace")) {
				final String  msg = ne.toString();
				final String[] segs = msg.split(":");
				throw new Exception(segs[3], ne);
			}

		} catch (Exception ignored) {
			// We allow the expression but it might fail later
		}		
	}


	@Override
	public <T> T evaluate() throws Exception {
		checkAndCreateContext();
		return (T)expression.evaluate(context);
	}
	
	@Override
	public void addLoadedVariables(Map<String, Object> variables) {
		if (context == null) {
			context = new MapContext(variables);
			return;
		}
		
		for (String name : variables.keySet()) {
			context.set(name, variables.get(name));
		}
	}
	@Override
	public void addLoadedVariable(String name, Object value) {
		checkAndCreateContext();
		context.set(name, value);
	}

	@Override
	public Map<String, Object> getFunctions() {
		return jexl.getFunctions();
	}

	@Override
	public void setFunctions(Map<String, Object> functions) {
		jexl.setFunctions(functions);
	}

	@Override
	public void setLoadedVariables(Map<String, Object> variables) {
		context = new MapContext(variables);
	}

	@Override
	public Collection<String> getVariableNamesFromExpression() {
		try {
			final Script script = jexl.createScript(expression.getExpression());
			Set<List<String>> names = script.getVariables();
			return unpack(names);
		} catch (Exception e){
			return null;
		}
	}

	private Collection<String> unpack(Set<List<String>> dottednames) {
		Collection<String> ret = new LinkedHashSet<String>();
		for (List<String> dottedname : dottednames) {
			if (dottedname.size() == 0) {
				continue;
			} else if (dottedname.size() == 1) {
				ret.add(dottedname.get(0));
			} else {
				StringBuilder name = new StringBuilder();
				for (String namePart : dottedname) {
					name.append(namePart);
					name.append(".");
				}
				String assembledName = name.substring(0, name.length() - 1);
				ret.add(assembledName);
			}
		}
		return ret;
	}


	/**
	 * TODO FIXME Currently does a regular expression. If there was a 
	 * way in JEXL of getting the variables for a given function it would 
	 * be better than what we do: which is a regular expression!
	 * 
	 * lz:rmean(fred,0)                     -> fred
	 * 10*lz:rmean(fred,0)                  -> fred
	 * 10*lz:rmean(fred,0)+dat:mean(bill,0) -> fred
	 * lz:rmean(fred,0)+lz:rmean(bill,0)    -> fred, bill
	 * lz:func(fred*10,bill,ted*2)          -> fred, bill, ted
	 */
	@Override
	public Collection<String> getLazyVariableNamesFromExpression() {
        try {
            final String expr = expression.getExpression();
            return getLazyVariables(expr);
        } catch (Exception e){
			return null;
		}
	}
	
	private static final Pattern lazyPattern = Pattern.compile("lz\\:[a-zA-Z0-9_]+\\({1}([a-zA-Z0-9_ \\,\\*\\+\\-\\\\]+)\\){1}");

	private Collection<String> getLazyVariables(String expr) {

		final Collection<String> found = new HashSet<String>(4);
        final Matcher          matcher = lazyPattern.matcher(expr);
 		while (matcher.find()) {
			final String      exprLine = matcher.group(1);
			final Script      script   = jexl.createScript(exprLine.replace(',','+'));
			Set<List<String>> names    = script.getVariables();
			Collection<String> v = unpack(names);
			found.addAll(v);
		}
        return found;
	}

	private void checkAndCreateContext() {
		if (context == null) {
			context = new MapContext();
		}
	}

	@Override
	public void addExpressionEngineListener(IExpressionEngineListener listener) {
		expressionListeners.add(listener);
	}


	@Override
	public void removeExpressionEngineListener(
			IExpressionEngineListener listener) {
		expressionListeners.remove(listener);
	}


	@Override
	public void evaluateWithEvent(IMonitor mon) {
		if (expression == null) return;

		final Script script = jexl.createScript(expression.getExpression());

		checkAndCreateContext();

		callable = script.callable(context);

		final ExecutorService service = Executors.newCachedThreadPool();
		//final IMonitor monitor = mon == null ? new IMonitor.Stub() : mon;

		service.submit( new Runnable() {			
			@Override
			public void run() {
				String exp = expression.getExpression();
				try {

					Future<Object> future = service.submit(callable);

					Object result = future.get();
					ExpressionEngineEvent event = new ExpressionEngineEvent(VanillaExpressionEngine.this, result, exp);
					fireExpressionListeners(event);
					return;

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					ExpressionEngineEvent event = new ExpressionEngineEvent(VanillaExpressionEngine.this, e, exp);
					fireExpressionListeners(event);
				}
				ExpressionEngineEvent event = new ExpressionEngineEvent(VanillaExpressionEngine.this, null, exp);
				fireExpressionListeners(event);

				return;

			}
		});		
	}

	private void fireExpressionListeners(ExpressionEngineEvent event) {
		for (IExpressionEngineListener listener : expressionListeners){
			listener.calculationDone(event);
		}
	}

	@Override
	public Object getLoadedVariable(String name) {
		if (context == null) return null;
		return context.get(name);
	}
}

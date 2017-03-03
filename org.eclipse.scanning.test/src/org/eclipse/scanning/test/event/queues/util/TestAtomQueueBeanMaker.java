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
package org.eclipse.scanning.test.event.queues.util;

import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;

/**
 * Factory class to generate classes implementing the {@link IHasAtomQueue} interface.
 * Used within the testing framework.
 * 
 * @author Michael Wharmby
 *
 */
public class TestAtomQueueBeanMaker {
	
	/*
	 * List of Objects Available
	 * -------------------------
	 * DummySubTaskBeanA - SubTaskBean containing four DummyAtomBeans (ABCD)
	 * DummySubTaskBeanB - SubTaskBean containing two DummyAtomBeans (EF)
	 * DummySubTaskBeanC - SubTaskBean containing three DummyAtomBeans (HIJ)
	 * DummySubTaskBeanD - SubTaskBean containing five DummyAtomBeans (LMNOP)
	 * DummySubTaskBeanE - SubTaskBean containing two DummyAtomBeans (QR)
	 * 
	 * DummyTaskBeanA - TaskBean containing DummySTBA & DummySTBB
	 */
	
	public static SubTaskAtom makeDummySubTaskBeanA() {
		SubTaskAtom sta = new SubTaskAtom("Dummy SubTask A");
		sta.addAtom(new DummyAtom("Arnold", 10));
		sta.addAtom(new DummyAtom("Beatrice", 465));
		sta.addAtom(new DummyAtom("Carlos", 1245));
		sta.addAtom(new DummyAtom("Deirdre", 675));
		
		return sta;
	}
	
	public static SubTaskAtom makeDummySubTaskBeanB() {
		SubTaskAtom sta = new SubTaskAtom("Dummy SubTask B");
		sta.addAtom(new DummyAtom("Edmund", 10));
		sta.addAtom(new DummyAtom("Francis", 20));
		return sta;
	}
	
	public static SubTaskAtom makeDummySubTaskBeanC() {
		SubTaskAtom sta = new SubTaskAtom("Dummy SubTask C");
		sta.addAtom(new DummyAtom("Heinrich", 479));
		sta.addAtom(new DummyAtom("Imhotep", 236));
		sta.addAtom(new DummyAtom("Jane", 387));
		return sta;
	}
	
	public static SubTaskAtom makeDummySubTaskBeanD() {
		SubTaskAtom sta = new SubTaskAtom("Dummy SubTask D");
		sta.addAtom(new DummyAtom("Linda", 7444));
		sta.addAtom(new DummyAtom("Manuel", 125));
		sta.addAtom(new DummyAtom("Nicola", 4756));
		sta.addAtom(new DummyAtom("Olive", 5887));
		sta.addAtom(new DummyAtom("Pablo", 58));
		return sta;
	}
	
	public static SubTaskAtom makeDummySubTaskBeanE() {
		SubTaskAtom sta = new SubTaskAtom("Dummy SubTask E");
		sta.addAtom(new DummyAtom("Quentin", 986));
		sta.addAtom(new DummyAtom("Reginald", 5886));
		return sta;
	}
	
	public static TaskBean makeDummyTaskBeanA() {
		TaskBean tb = new TaskBean("Dummy Task A");
		tb.addAtom(makeDummySubTaskBeanA());
		tb.addAtom(makeDummySubTaskBeanB());
		return tb;
	}

}

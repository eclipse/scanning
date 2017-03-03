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
package org.eclipse.scanning.device.ui.util;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class PageUtil {

	
	/**
	 * Gets the page, even during startup.
	 * @return the page
	 */
	public static IWorkbenchPage getPage() {
		return getPage(null);
	}
	
	/**
	 * Gets the page, even during startup.
	 * @return the page
	 */
	public static IWorkbenchPage getPage(IWorkbenchPartSite site) {
		if (site != null) {
			IWorkbenchPage page = site.getPage();
			if (page != null) return page;
		}
		IWorkbenchPage activePage = PageUtil.getActivePage();
		if (activePage!=null) return activePage;
		return PageUtil.getDefaultPage();
	}
	
	/**
	 * @return IWorkbenchPage
	 */
	public static IWorkbenchPage getActivePage() {
		try {
			if (!PlatformUI.isWorkbenchRunning()) return null;
		} catch (Exception | NoClassDefFoundError ne) {
			return null;
		}
		final IWorkbench bench = PlatformUI.getWorkbench();
		if (bench==null) return null;
		final IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
		if (window==null) return null;
		return window.getActivePage();
	}
	
	/**
	 * @return IEditorPart
	 */
	public static IEditorPart getActiveEditor() {
		final IWorkbenchPage page = PageUtil.getPage();
		return page.getActiveEditor();
	}

	
	/**
	 * @return IWorkbenchPage
	 */
	public static IWorkbenchPage getDefaultPage() {
		try {
			if (!PlatformUI.isWorkbenchRunning()) return null;
		} catch (Exception | NoClassDefFoundError ne) {
			return null;
		}
		final IWorkbench bench = PlatformUI.getWorkbench();
		if (bench==null) return null;
		final IWorkbenchWindow[] windows = bench.getWorkbenchWindows();
		if (windows==null) return null;
		
		return windows[0].getActivePage();
	}	

}

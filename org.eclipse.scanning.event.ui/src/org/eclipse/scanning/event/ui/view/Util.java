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
package org.eclipse.scanning.event.ui.view;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

class Util {

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
		IWorkbenchPage activePage = getActivePage();
		if (activePage!=null) return activePage;
		return getDefaultPage();
	}
	
	/**
	 * @return IWorkbenchPage
	 */
	public static IWorkbenchPage getActivePage() {
		final IWorkbench bench = PlatformUI.getWorkbench();
		if (bench==null) return null;
		final IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
		if (window==null) return null;
		return window.getActivePage();
	}
	
	
	/**
	 * @return IWorkbenchPage
	 */
	public static IWorkbenchPage getDefaultPage() {
		final IWorkbench bench = PlatformUI.getWorkbench();
		if (bench==null) return null;
		final IWorkbenchWindow[] windows = bench.getWorkbenchWindows();
		if (windows==null) return null;
		
		return windows[0].getActivePage();
	}



	/**
	 * Returns a FileStoreEditorInput as IEditorInput on a file path,
	 * containing a file system reference to it.
	 * @param filename
	 */
	public static IEditorInput getExternalFileStoreEditorInput(String filename) {
		final IFileStore externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(filename));
		return new FileStoreEditorInput(externalFile);
	}

	
	/**
	 * 
	 * @param area
	 */
	public static void removeMargins(Composite area) {
		final GridLayout layout = (GridLayout)area.getLayout();
		if (layout==null) return;
		layout.horizontalSpacing=0;
		layout.verticalSpacing  =0;
		layout.marginBottom     =0;
		layout.marginTop        =0;
		layout.marginLeft       =0;
		layout.marginRight      =0;
		layout.marginHeight     =0;
		layout.marginWidth      =0;

	}

	
	public static boolean browse(final String location) throws Exception {
		
		final String     dir  = Util.getSanitizedPath(location);
		
		final File resultsDir = new File(dir);
		if (resultsDir.exists()) return browse(resultsDir);
		
		return false;
	}

	public static boolean browse(File resultsDir) throws Exception {
		
		final ProcessBuilder pb = new ProcessBuilder();
		
		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(resultsDir);
		
		if (isWindowsOS()) {
		    pb.command("cmd", "/C", "explorer \""+resultsDir.getAbsolutePath()+"\"");
		} else if (isLinuxOS()) {
		    pb.command("bash", "-c", "nautilus \""+resultsDir.getAbsolutePath()+"\"");
		} else if (isMacOS()) {
		    pb.command("/bin/sh", "-c", "open \""+resultsDir.getAbsolutePath()+"\"");
		}
		
		pb.start(); // We don't wait for it
 
		return true;
	}

	/**
	 * Tries to write the xinfo correctly even if the run is on windows.
	 * @param path
	 * @return
	 */
	public static String getSanitizedPath(String path) {
		if (isWindowsOS() && path.startsWith("/dls/")) {
			path = "\\\\Data.diamond.ac.uk\\"+path.substring(5);
		}
		return path;
	}
	
	public static boolean isWindowsOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}
	/**
	 * @return true if linux
	 */
	public static boolean isLinuxOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("linux");
	}
	
	public static boolean isMacOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().indexOf("mac") >= 0;
	}
}

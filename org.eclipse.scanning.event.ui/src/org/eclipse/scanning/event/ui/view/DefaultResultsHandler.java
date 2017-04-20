package org.eclipse.scanning.event.ui.view;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.ui.IResultHandler;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * A default results handler for a {@link StatusBean} that opens the bean's run directory.
 */
class DefaultResultsHandler implements IResultHandler<StatusBean> {

	@Override
	public boolean isHandled(StatusBean bean) {
		return bean.getRunDirectory() != null;
	}

	@Override
	public boolean open(StatusBean bean) throws Exception {
		try {
			final IWorkbenchPage page = Util.getPage();
			
			final File fdir = new File(Util.getSanitizedPath(bean.getRunDirectory()));
			if (!fdir.exists()){
				MessageDialog.openConfirm(getShell(), "Directory Not Found", "The directory '"+bean.getRunDirectory()+"' has been moved or deleted.\n\nPlease contact your support representative.");
			    return false;
			}
			
			if (Util.isWindowsOS()) { // Open inside DAWN
				final String         dir  = fdir.getAbsolutePath();		
				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(dir+"/fred.html");
				final IEditorInput edInput = Util.getExternalFileStoreEditorInput(dir);
				page.openEditor(edInput, desc.getId());
				
			} else { // Linux cannot be relied on to open the browser on a directory.
				Util.browse(fdir);
			}
			return true;
		} catch (Exception e1) {
			ErrorDialog.openError(getShell(), "Internal Error", "Cannot open "+bean.getRunDirectory()+".\n\nPlease contact your support representative.", 
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, e1.getMessage()));
			return false;
		}
	}
	
	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

}

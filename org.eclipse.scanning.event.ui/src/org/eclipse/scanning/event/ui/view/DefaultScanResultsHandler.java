package org.eclipse.scanning.event.ui.view;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.ui.IResultHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * Result handler for opening {@link ScanBean}s by opening their results files, as
 * returned by {@link ScanBean#getFilePath()}.
 *
 * @author Matthew Dickie
 */
public class DefaultScanResultsHandler implements IResultHandler<ScanBean> {

	private static final String DATA_PERSPECTIVE_ID = "org.edna.workbench.application.perspective.DataPerspective";

	@Override
	public boolean isHandled(StatusBean bean) {
		return bean.getRunDirectory() == null &&
				(bean instanceof ScanBean && ((ScanBean) bean).getFilePath() != null);
	}

	@Override
	public boolean open(ScanBean scanBean) throws Exception {
		if (scanBean.getFilePath() == null) {
			return false;
		}

		if (!scanBean.getStatus().isFinal() && !confirmOpen(scanBean)) {
			return false;
		}

		String filePath = scanBean.getFilePath();
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		// Set the perspective to Data Browsing Perspective
		// TODO FIXME When there is a general data viewing perspective from DAWN, use that.
		workbench.showPerspective(DATA_PERSPECTIVE_ID, window);
		String editorId = getEditorId(filePath);
		IEditorInput editorInput = getEditorInput(filePath);
		IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
		page.openEditor(editorInput, editorId);
		return true;
	}

	public boolean confirmOpen(ScanBean bean) {
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//		return MessageDialog.openQuestion(shell, "'"+bean.getName()+"' incomplete.",
//					"The run of '"+bean.getName()+"' has not completed.\n" +
//					"Would you like to try to open the results anyway?");

		// TODO: we currently do not open scan results for scans that have not finished as they cannot
		// In future, we may wish to add a feature to support this in future. Talk to Jacob Filik
		MessageDialog.openError(shell, "'"+bean.getName()+"' incomplete.",
		"Cannot open scan results.\nThe run of '"+bean.getName()+"' has not completed.");
		return false;
	}

	private String getEditorId(String filePath) {
		IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor desc = editorRegistry.getDefaultEditor(filePath);
		if (desc == null) {
			desc = editorRegistry.getDefaultEditor(filePath + ".txt");
		}
		return desc.getId();
	}

	private IEditorInput getEditorInput(String filePath) {
		final IFileStore externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(filePath));
		return new FileStoreEditorInput(externalFile);
	}

}

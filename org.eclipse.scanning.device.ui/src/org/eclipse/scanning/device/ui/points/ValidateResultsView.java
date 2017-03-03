package org.eclipse.scanning.device.ui.points;

import java.util.Date;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ScanningPerspective;
import org.eclipse.scanning.device.ui.util.PageUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A view which attempts to pick up selection events
 * from calling validate on a device, and display the
 * results of the validation to a user
 * 
 * @author Matt Taylor
 *
 */
public class ValidateResultsView extends ViewPart implements ISelectionListener {

	public static final String ID = "org.eclipse.scanning.device.ui.scan.validateResultsView"; //$NON-NLS-1$
	private static final Logger logger = LoggerFactory.getLogger(ValidateResultsView.class);
	
	// UI
	private StyledText text;
	
	public ValidateResultsView() {
		PageUtil.getPage(getSite()).addSelectionListener(this);
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		
		this.text = new StyledText(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		text.getParent().layout(new Control[]{text});
		
		// We force the scan view to exist. It might be the one to return the compound model
		// that we will use.
		ScanningPerspective.createKeyPlayers();
		
        createActions();
	}
	
	/**
	 * Create the actions
	 */
	private void createActions() {
		final IContributionManager man = getViewSite().getActionBars().getToolBarManager();

		final Action restart = new Action("Clear", Activator.getImageDescriptor("icons/layers-stack.png")) {
			public void run() {
				clear();
			}
		};
		man.add(restart);

		final MenuManager menuMan = new MenuManager();
		menuMan.add(restart);
		
		text.setMenu(menuMan.createContextMenu(text));
	}

	@Override
	public void dispose() {
		if (PageUtil.getPage(getSite())!=null) {
			PageUtil.getPage(getSite()).removeSelectionListener(this);
		}
		super.dispose();
	}
	
	private void clear() {
		setThreadSafeText(text, new StyledString(""));
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object ob = ((IStructuredSelection)selection).getFirstElement();

			if (ob instanceof ValidateResults) {
				ValidateResults validationResults  = (ValidateResults)ob;
				update(validationResults);
			}

		}
	}

	/**
	 * Method for updating the display with the validation results
	 * @param results the ValidationResults object
	 */
	public void update(ValidateResults results) {
		if (results == null) {
			return;
		}
		
		try {

    		StyledString styledString = new StyledString();
        	
			styledString.append("Results received from '");
			styledString.append(results.getDeviceName(), FontStyler.BOLD);
			styledString.append("' at ");
			styledString.append(new Date(System.currentTimeMillis()).toString(), StyledString.COUNTER_STYLER);
			styledString.append("\n");
        	
			styledString.append(appendResultsToStyledString(results));

            setThreadSafeText(text, styledString);
            return; 

		} catch (Exception ne) {
			logger.error("Cannot create validation results", ne);
			if (ne.getMessage()!=null) {
				setThreadSafeText(text, ne.getMessage());
			} else {
				setThreadSafeText(text, ne.toString());
			}
		}
	}
	
	/**
	 * Turn the results into a styled string for display to user, highlighting key values
	 * @param results The ValidationResults
	 * @return a StyledString to display on the view
	 */
	private StyledString appendResultsToStyledString(ValidateResults results) {
		StyledString styledString = new StyledString();
		if (results.getResults() != null) {
			if (results.getResults() instanceof String) {
				// Print out all the results from the raw PVStructure string, but style key information 
				String resultString = (String)results.getResults();
				styledString.append(resultString);
				
				// Style the duration
				adjustStyleOfDuration(styledString, resultString);
				
				// Style the axes to move
				adjustStyleOfAxesToMove(styledString, resultString);
				
			} else {
				// not a string, just print out the results object
				 styledString.append(results.getResults().toString());
			}
		}
		
		return styledString;
		
	}
	
	/**
	 * Adjust the style of the duration section of the styled string if it has one
	 * @param styledString
	 * @param resultString
	 */
	private void adjustStyleOfDuration(StyledString styledString, String resultString) {
		int durationIndex = resultString.indexOf("double duration");
		if (durationIndex != -1) {
			int indexOfNewLine = resultString.substring(durationIndex).indexOf('\n');
			if (indexOfNewLine == -1) {
				indexOfNewLine = resultString.substring(durationIndex).length() - 1;
			}
	    	styledString.setStyle(durationIndex + "double".length(), indexOfNewLine - "double".length(), FontStyler.BOLD);		    	
		}
	}
	
	/**
	 * Adjust the style of the AxesToMove section of the styled string if it has one
	 * @param styledString
	 * @param resultString
	 */
	private void adjustStyleOfAxesToMove(StyledString styledString, String resultString) {
		int axesToMoveIndex = resultString.indexOf("string[] axesToMove");
		if (axesToMoveIndex != -1) {
			int indexOfNewLine = resultString.substring(axesToMoveIndex).indexOf('\n');
			if (indexOfNewLine == -1) {
				indexOfNewLine = resultString.substring(axesToMoveIndex).length();
			}
	    	styledString.setStyle(axesToMoveIndex + "string[]".length(), indexOfNewLine - "string[]".length(), FontStyler.BOLD);		    	
		}
	}

	/**
	 * Set the text of the display from a String object
	 * @param text The StyledText object on the display to set
	 * @param string The text with which to set the display to
	 */
	private void setThreadSafeText(StyledText text, String string) {
		setThreadSafeText(text, new StyledString(string));
	}
	
	/**
	 * Set the text of the display from a StyledString object
	 * @param text The StyledText object on the display to set
	 * @param styledString The text with which to set the display to in StyledString format
	 */
	private void setThreadSafeText(StyledText text, StyledString styledString) {
		if (text.isDisposed()) {
			return;
		}
    	text.getDisplay().syncExec(() -> {
			if (text.isDisposed()) {
				return;
			}
	    	text.setText(styledString.toString());
	    	text.setStyleRanges(styledString.getStyleRanges());
    	});	
    }

	@Override
	public void setFocus() {
		if (text!=null && !text.isDisposed()) {
			text.setFocus();
		}
	}

	/**
	 * Class for styling text for display on the view
	 * @author vtu42223
	 *
	 */
	private static class FontStyler extends Styler {
		
		public static final Styler BOLD = new FontStyler(new Font(null, "Dialog", 10, SWT.BOLD));

		private Font font;

		public FontStyler(Font toSet) {
			this.font = toSet;
		}

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.font  = font;
		}
	}

}

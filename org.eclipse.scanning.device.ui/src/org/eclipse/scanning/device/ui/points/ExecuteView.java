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
package org.eclipse.scanning.device.ui.points;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.SampleData;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.AbstractPointsModel;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.scan.IParserService;
import org.eclipse.scanning.api.scan.ScanEstimator;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.ui.AbstractControl;
import org.eclipse.scanning.api.script.ScriptRequest;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.api.ui.auto.IModelDialog;
import org.eclipse.scanning.api.ui.auto.InterfaceInvalidException;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ScanningPerspective;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.PageUtil;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A view which attempts to pick up selection events
 * from building a scan and then display the scan information.
 * 
 * @author Matthew Gerring
 *
 */
public class ExecuteView extends ViewPart implements ISelectionListener {

	public static final String ID = "org.eclipse.scanning.device.ui.scan.executeView"; //$NON-NLS-1$
	private static final Logger logger = LoggerFactory.getLogger(ExecuteView.class);
	
	// UI
	private StyledText text;
	private Composite  run;
	private Label      timeEstimate;

	// Services
	private IPointGeneratorService  pservice; // Used to create a compound generator
	private IValidatorService       vservice; // Used to validate a selection
	private IRunnableDeviceService  dservice;
	private IScannableDeviceService cservice;

	// Job
	private Job updateJob;
	
	// Data
	private SampleData sampleData;
	
	public ExecuteView() {
		
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_SCAN_INFO, true);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_SCAN_CMD,  true);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_VERBOSE_SCAN_CMD,  false);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_SCAN_TIME,  true);
		this.pservice = ServiceHolder.getGeneratorService();
		this.vservice = ServiceHolder.getValidatorService();
		try {
			this.dservice = ServiceHolder.getRemote(IRunnableDeviceService.class);
			this.cservice = ServiceHolder.getRemote(IScannableDeviceService.class);

		} catch (Exception e) {
			logger.error("Unable to get remote device service!", e);
		}
		updateJob = new Job("Update Scna Information") {
			public IStatus run(IProgressMonitor monitor) {
				update(monitor);
				return Status.OK_STATUS;
			}
		};
		updateJob.setUser(false);
		updateJob.setSystem(true);
		updateJob.setPriority(Job.INTERACTIVE);
		
		final IStashing stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.scan.execute.sample.json");
		sampleData = new SampleData();
		if (stash.isStashed()) {
			try {
				sampleData = stash.unstash(SampleData.class);
			} catch (Exception e) {
				logger.error("Cannot unstash sample data!", e);
			}
		}
	}
	
	@Override
    public void saveState(IMemento memento) {
		super.saveState(memento);
		try {
			final IStashing stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.scan.execute.sample.json");
			stash.stash(sampleData);
		} catch (Exception ne) {
			logger.error("Cannot save sample information!", ne);
		}
    }


	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		
		this.text = new StyledText(container, SWT.MULTI | SWT.H_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		text.getParent().layout(new Control[]{text});
		
		run = new Composite(container, SWT.NONE);
		run.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		run.setLayout(new GridLayout(3, false));
		
		submitButton = new Button(run, SWT.PUSH);
		submitButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		submitButton.setText("Submit");
		submitButton.setToolTipText("Execute current scan\n(Submits it to the queue of scans to be run.)");
		submitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				submit();
			}
		});
		submitButton.setImage(Activator.getImageDescriptor("icons/shoe--arrow.png").createImage());

		timeEstimate = new Label(run, SWT.NONE);
		timeEstimate.setText("                    ");
		timeEstimate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		final Composite rightButtons = new Composite(run, SWT.NONE);
		rightButtons.setLayout(new GridLayout(2, false));
		rightButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		final Button clipboard = new Button(rightButtons, SWT.PUSH);
		clipboard.setText("Copy");
		clipboard.setToolTipText("Copy the scan command to the clipboard");
		clipboard.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				clipboard();
			}
		});
		clipboard.setImage(Activator.getImageDescriptor("icons/clipboard-invoice.png").createImage());
		
		final Button sampleData = new Button(rightButtons, SWT.PUSH);
		sampleData.setText("Sample");
		sampleData.setToolTipText("Set the sample information for the run.");
		sampleData.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sampleInformation();
			}
		});
		sampleData.setImage(Activator.getImageDescriptor("icons/beaker.png").createImage());

	
		createActions();
		PageUtil.getPage(getSite()).addSelectionListener(this);
		
		// We force the scan view to exist. It might be the one to return the compound model
		// that we will use.
		ScanningPerspective.createKeyPlayers();

		updateJob.schedule();
	}
	
	protected void sampleInformation() {
		
		try {
			IModelDialog<SampleData> dialog = ServiceHolder.getInterfaceService().createModelDialog(getViewSite().getShell());
			dialog.setPreamble("Please define the sample data.");
			dialog.create();
			dialog.setSize(550,450); // As needed
			dialog.setText("Scan Area");
			if (this.sampleData==null) sampleData = new SampleData();
			dialog.setModel(sampleData);
			int ok = dialog.open();
			if (ok==IModelDialog.OK) {
				this.sampleData = dialog.getModel();
				updateJob.schedule();
			}
		} catch ( InterfaceInvalidException e) {
			logger.error("Internal error setting Sample Information", e);
		}
 
	}

	protected void submit() {
		try {

			// Send it off
			ScanBean bean=null;
			try {
				bean = new ScanBean(createScanRequest());
			} catch (Exception ne) {
				ErrorDialog.openError(getViewSite().getShell(), "Cannot Create Scan Request", "Unable to create a legal scan request.\nThere is something invalid in your current configuration of the scan.\n\nPlease contact your support representative.", new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage(), ne));
				logger.error("Unable to create a legal scan request!", ne);
				return;
			}
			bean.setStatus(org.eclipse.scanning.api.event.status.Status.SUBMITTED);

			ISubmitter<ScanBean> submitter = ServiceHolder.getEventService().createSubmitter(new URI(CommandConstants.getScanningBrokerUri()), EventConstants.SUBMISSION_QUEUE);
			submitter.submit(bean);
			
			// Show the Queue
			showQueue();
			
		} catch (Exception ne) {
			ErrorDialog.openError(getViewSite().getShell(), "Cannot Submit Scan", "There was a problem submitting the scan.\n\nPlease contact your support representative.", new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage(), ne));
		    logger.error("Unable to submit scan", ne);
		}
	}

	private void showQueue() {
		// Make sure a view is opened that looks at it.
		try {
			ViewUtil.openQueueMonitor(ScanBean.class, "Scans");
		} catch (PartInitException | UnsupportedEncodingException ne) {
			ErrorDialog.openError(getViewSite().getShell(), "Cannot Show Queue", "There was a problem showing the queue.\n\nPlease contact your support representative.", new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage(), ne));
		    logger.error("Unable to show scan queue", ne);
		}
	}

	private ScanRequest<IROI> createScanRequest() throws Exception {
		return createScanRequest(!Boolean.getBoolean("org.eclipse.scanning.ignore.scan.request.adapters"));
	}

	/**
	 * If there is a view which provides the whole ScanRequest. 
	 * This will be used and returned. Otherwise we cycle through the
	 * components of the scan request looking for views which provide
	 * their definitions.
	 * 
	 * @return
	 * @throws Exception
			// TODO Use IScanBuilderService
	 */
	private ScanRequest<IROI> createScanRequest(boolean lookForScanRequest) throws Exception {
		
		if (lookForScanRequest) {
			// TODO Replace with IScanBuilderService to make e4 compatible
			IViewReference[] refs = PageUtil.getPage().getViewReferences();
			for (IViewReference iViewReference : refs) {
				IViewPart part = iViewReference.getView(false);
				if (part==null) continue;
				ScanRequest<IROI> req = part.getAdapter(ScanRequest.class);
				if (req!=null) return req;
			}
		}
		
		if (modelAdaptable==null) {
			// We see if there is a view with a compound model adaptable
			// TODO Replace with IScanBuilderService to make e4 compatible
			IViewReference[] refs = PageUtil.getPage().getViewReferences();
			for (IViewReference iViewReference : refs) {
				IViewPart part = iViewReference.getView(false);
				if (part==null) continue;
                CompoundModel<IROI> cm = part.getAdapter(CompoundModel.class);
                if (cm !=null) {
                	modelAdaptable = part;
                }
			}
		}
		if (modelAdaptable==null) return null; // Nothing to update, no view gives us a CompoundModel!

		// We see if there is a view with a compound model adaptable
		// TODO Replace with IScanBuilderService to make e4 compatible

		ScanRequest<IROI> ret = new ScanRequest<IROI>();
		CompoundModel<IROI> cm = modelAdaptable.getAdapter(CompoundModel.class);
		ret.setCompoundModel(cm);
		
		IPosition[] pos = modelAdaptable.getAdapter(IPosition[].class);
		ret.setStart(pos[0]);
		ret.setEnd(pos[1]);

		ScriptRequest[] req = modelAdaptable.getAdapter(ScriptRequest[].class);
		ret.setBefore(req[0]);
		ret.setAfter(req[1]);

		ret.setMonitorNames(getMonitors());
		ret.setDetectors(getDetectors());
		ret.setSampleData(sampleData);
        vservice.validate(ret);
		
		return ret;
	}

	@Override
	public void dispose() {
		if (PageUtil.getPage()!=null) PageUtil.getPage().removeSelectionListener(this);
		super.dispose();
	}
	
	private IAdaptable modelAdaptable;
	private IAction    submitAction;
	private Button     submitButton;
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object ob = ((IStructuredSelection)selection).getFirstElement();
			
			// This slightly funny alg or assign and sometimes update
			// is correct. Do not change unless sure that UI is working afterwards.
			if (ob instanceof IAdaptable) { // TODO Replace with ScanBuilderService
                CompoundModel<IROI> cm = ((IAdaptable)ob).getAdapter(CompoundModel.class);
                if (cm !=null) modelAdaptable = (IAdaptable)ob;
			}
			if (isUpdatableSelection(ob)) updateJob.schedule();

		}
	}

	private boolean isUpdatableSelection(Object ob) {
		if (ob == null)                        return true; // Something deleted.
		if (ob instanceof GeneratorDescriptor) return true; // Generator changed.
		if (ob instanceof FieldValue)          return true; // Model changed.
		if (ob instanceof DeviceInformation)   return true; // Device changed.
		if (ob instanceof IScannable<?>)       return true; // Device changed.
		if (ob instanceof ScanRegion)          return true; // Region changed.
		if (ob instanceof IROI)                return true; // Region changed.
		if (ob instanceof AbstractControl)     return true; // Position changed.
		if (ob instanceof ScanRequest)         return true; // Whole request changed.
		return false;
	}

	private final static long HOUR_IN_MS = 60*60*1000;
	/**
	 * Thread safe method for getting the string with should be shown to the user about the scan.
	 * @param monitor
	 */
	private void update(IProgressMonitor monitor) {
		
		try {
			ScanRequest<IROI> req = createScanRequest();
			if (monitor.isCanceled()) return;
			if (req==null) {
	    		StyledString styledString = new StyledString();
	    		String name = modelAdaptable != null && modelAdaptable instanceof IWorkbenchPart ? ((IWorkbenchPart)modelAdaptable).getTitle() : "Scan Editor";
	        	styledString.append("Please create a model using '"+name+"'", StyledString.COUNTER_STYLER);
	            setThreadSafeText(text, styledString);
	            return;
			}
	        CompoundModel<IROI> cm = req.getCompoundModel();
	        if (cm != null) {
	        	// Validate
	        	vservice.validate(cm);
				setThreadSafeEnabled(true);
	        	
	    		StyledString styledString = new StyledString();
		        	
	        	// Create generator for points
				if (monitor.isCanceled()) return;
	        	final IPointGenerator<?> gen = pservice.createCompoundGenerator(cm);
	        	styledString.append("A scan of ");
	        	styledString.append((new DecimalFormat()).format(gen.size()), StyledString.COUNTER_STYLER);
	        	styledString.append(" points, scanning motors: ");
	        	styledString.append(getMotorNames(gen), FontStyler.BOLD);

	        	if (Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_SCAN_INFO)) {
			        IPosition start = req.getStart();
			        if (start!=null) {
						if (monitor.isCanceled()) return;
			        	styledString.append("\nStart: "+start);
			        }
			        
			        ScriptRequest before = req.getBefore();
			        if (before!=null) {
						if (monitor.isCanceled()) return;
			        	styledString.append("\nBefore: ");
			        	styledString.append(before.toString(), StyledString.DECORATIONS_STYLER);
			        }
		        	
					if (monitor.isCanceled()) return;
		        	styledString.append("\nScan: ");
		        	styledString.append(getModelNames(cm), StyledString.DECORATIONS_STYLER);
	
			        ScriptRequest after = req.getAfter();
			        if (after!=null) {
						if (monitor.isCanceled()) return;
			        	styledString.append("\nAfter: ");
			        	styledString.append(after.toString(), StyledString.DECORATIONS_STYLER);
			        }
			        IPosition end = req.getEnd();
			        if (end!=null) {
						if (monitor.isCanceled()) return;
			        	styledString.append("\nEnd: "+end);
			        }
	
					if (monitor.isCanceled()) return;
		        	styledString.append("\nDetectors: ");
		        	styledString.append(getDetectorNames(), FontStyler.BOLD);
		        	
					if (monitor.isCanceled()) return;
		        	styledString.append("\nRegions: ");
		        	styledString.append(getScanRegions(cm.getRegions()), StyledString.QUALIFIER_STYLER);
		        	
					if (monitor.isCanceled()) return;
		        	styledString.append("\nMonitors: ");
		        	styledString.append(getMonitorNames(), StyledString.DECORATIONS_STYLER);

					if (monitor.isCanceled()) return;
		        	if (sampleData!=null && sampleData.getName()!=null && sampleData.getName().length()>0) {
			        	styledString.append("\nSample: ");
			        	styledString.append(sampleData.getName(), StyledString.QUALIFIER_STYLER);
		        	}
	        	}
	        	
	        	if (Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_SCAN_CMD)) {
	        		try {
	        			final IParserService pyService = ServiceHolder.getParserService();
	        			boolean verbose = Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_VERBOSE_SCAN_CMD);
		        		final String cmd = pyService.getCommand(req, verbose);
			        	styledString.append("\n\nScan Command:\n");
			        	styledString.append(cmd, FontStyler.CODE);
	        		} catch (Exception ne) {
	        			styledString.append("\n\nCannot print scan command: '"+IParserService.class.getSimpleName()+"' is misconfigured! Ask you support representative to ensure it is there.");
	        			styledString.append("\n"+ne.toString());
	        			logger.error("Cannot parse a scan request", ne);
	        		}
	        	}
	            setThreadSafeText(text, styledString);
	        	
	            String timeString = "";
	        	if (Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_SCAN_TIME)) {

	        		try {
	        			final ScanEstimator estimator = new ScanEstimator(ServiceHolder.getGeneratorService(), req);
	        			long time = estimator.getEstimatedScanTime();
	        			Format format = (time<HOUR_IN_MS) ? new SimpleDateFormat("mm'm' ss's'") : new SimpleDateFormat("h'h' mm'm' ss's'");
	        			timeString = "   "+format.format(new Date(time));
	        		} catch (Exception ne) {
	        			timeString = ne.getMessage();
	        		}
	        	}
	        	setThreadSafeLabel(timeEstimate, timeString);
 	        }
		} catch (ModelValidationException ne) {
			setThreadSafeEnabled(false);
			setThreadSafeText(text, ne.getMessage());
			 
		} catch (Exception ne) {
			setThreadSafeEnabled(false);
			logger.error("Cannot create summary of scan!", ne);
			if (ne.getMessage()!=null) {
				setThreadSafeText(text, ne.getMessage());
			} else {
				setThreadSafeText(text, ne.toString());
			}
		}
	}

	private void setThreadSafeEnabled(boolean enabled) {
		submitButton.getDisplay().syncExec(()->{
			this.submitAction.setEnabled(enabled);
			this.submitButton.setEnabled(enabled);
		});
	}

	private String getModelNames(CompoundModel<IROI> compound) {
		StringBuilder buf = new StringBuilder();
		for (Iterator<Object> it = compound.getModels().iterator(); it.hasNext();) {
			Object model = it.next();
			if (model instanceof AbstractPointsModel) {
				buf.append(((AbstractPointsModel)model).getSummary());
			} else {
				buf.append(model);
			}
			if (it.hasNext()) buf.append(", ");
		}
		return buf.toString();
	}

	private void setThreadSafeText(StyledText text, String string) {
		setThreadSafeText(text, new StyledString(string));
	}
	private void setThreadSafeText(StyledText text, StyledString styledString) {
		if (text.isDisposed()) return;
    	text.getDisplay().syncExec(new Runnable() {
    		public void run() {
    			if (text.isDisposed()) return;
	        	text.setText(styledString.toString());
	        	text.setStyleRanges(styledString.getStyleRanges());
    		}
    	});	
    }
	private void setThreadSafeLabel(Label label, String message) {
		if (label.isDisposed()) return;
		label.getDisplay().syncExec(new Runnable() {
    		public void run() {
    			if (label.isDisposed()) return;
    			label.setText(message);
    		}
    	});	
    }

	private String getScanRegions(Collection<ScanRegion<IROI>> regions) {
		
		final StringBuilder buf = new StringBuilder();
		if (regions==null) return "None";
     	for (Iterator<ScanRegion<IROI>> it = regions.iterator(); it.hasNext();) {
    		ScanRegion<IROI> region = it.next();
    		buf.append(region);
    		if(it.hasNext()) buf.append(",");
    		buf.append(" ");
    	}
    	if (buf.length()>0) return buf.toString();
    	return "None";
 	}

	private String getDetectorNames() throws Exception {
		
		Collection<DeviceInformation<?>> infos = getDeviceInformation();
		Collection<DeviceInformation<?>> activated = new ArrayList<>();
    	for (Iterator<DeviceInformation<?>> it = infos.iterator(); it.hasNext();) {
			DeviceInformation<?> deviceInformation = it.next();
			if (deviceInformation.isActivated()) activated.add(deviceInformation);
    	}
    	
		final StringBuilder buf = new StringBuilder();
    	for (Iterator<DeviceInformation<?>> it = activated.iterator(); it.hasNext();) {
    		DeviceInformation<?> info = it.next();
    		IRunnableDevice<Object> device = dservice.getRunnableDevice(info.getName());
    		device.validate(info.getModel());
    		buf.append(info.getName());
    		if(it.hasNext()) buf.append(",");
    		buf.append(" ");
    	}

    	if (buf.length()>0) return buf.toString();
    	return "None";
	}
	
	private Map<String,Object> getDetectors() throws Exception {
		
		Map<String,Object> detectors = new HashMap<>();
		Collection<DeviceInformation<?>> infos = getDeviceInformation();
		Collection<DeviceInformation<?>> activated = new ArrayList<>();
    	for (Iterator<DeviceInformation<?>> it = infos.iterator(); it.hasNext();) {
			DeviceInformation<?> deviceInformation = it.next();
			if (deviceInformation.isActivated()) activated.add(deviceInformation);
    	}
    	for (Iterator<DeviceInformation<?>> it = activated.iterator(); it.hasNext();) {
    		DeviceInformation<?> info = it.next();
    		detectors.put(info.getName(), info.getModel());
    	}

    	return detectors;
	}

	private List<String> getMonitors() throws Exception {
		
		final Collection<DeviceInformation<?>> scannables = cservice.getDeviceInformation();
		final List<String> ret = new ArrayList<String>();
		for (DeviceInformation<?> info : scannables) {
			if (info.isActivated()) ret.add(info.getName());
		}
		return ret;
	}
	
	private String getMonitorNames() throws Exception {
		
		List<String> mons = getMonitors();
		if (mons==null || mons.isEmpty()) return "None";
		
		final StringBuilder buf = new StringBuilder();
    	for (Iterator<String> it = mons.iterator(); it.hasNext();) {
    		String name = it.next();
    		buf.append(name);
    		if(it.hasNext()) buf.append(",");
    		buf.append(" ");
    	}
        return buf.toString();
	}


	private Collection<DeviceInformation<?>> getDeviceInformation() throws ScanningException {
		
		IViewReference[] refs = PageUtil.getPage().getViewReferences();
		for (IViewReference iViewReference : refs) {
			IViewPart part = iViewReference.getView(false);
			if (part==null) continue;
			Object info = part.getAdapter(DeviceInformation.class);
			if (info!=null && info instanceof Collection) { // A collection of device information
				return (Collection<DeviceInformation<?>>)info;
			}
		}
		
		// We cannot find a part which has the temp information so
        // we use the server information.
		return dservice.getDeviceInformation(); 
	}

	private String getMotorNames(IPointGenerator<?> gen) {

		StringBuilder buf = new StringBuilder();
		IPosition first = gen.iterator().next();
		for (Iterator<String> it = first.getNames().iterator(); it.hasNext();) {
			String name = it.next();
			buf.append(name);
			if(it.hasNext()) buf.append(",");
			buf.append(" ");
		}
		return buf.toString();
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		List<IContributionManager> mans = new ArrayList<>(Arrays.asList(getViewSite().getActionBars().getToolBarManager(), getViewSite().getActionBars().getMenuManager()));
		MenuManager     rightClick     = new MenuManager();
		mans.add(rightClick);
		

		
		IAction showInfo = createPreferenceAction("Show scan information", DevicePreferenceConstants.SHOW_SCAN_INFO, "icons/information-white.png");
		IAction showCmd = createPreferenceAction("Show scan command", DevicePreferenceConstants.SHOW_SCAN_CMD, "icons/information-green.png");
		IAction showVerbose = createPreferenceAction("Show verbose scan command", DevicePreferenceConstants.SHOW_VERBOSE_SCAN_CMD, "icons/information-purple.png");
		IAction showTime = createPreferenceAction("Show time estimation", DevicePreferenceConstants.SHOW_SCAN_TIME, "icons/information-red.png");
	
		ViewUtil.addGroups("show", mans, showInfo, showCmd, showVerbose, showTime);
		
		this.submitAction = new Action("Submit current scan\n(Submits it to the queue of scans to be run.)", Activator.getImageDescriptor("icons/shoe--arrow.png")) {
			public void run() {
				submit();
			}
		};
		IAction copy = new Action("Copy scan command to clipboard", Activator.getImageDescriptor("icons/clipboard-invoice.png")) {
			public void run() {
				clipboard();
			}
		};
		IAction sample = new Action("Edit sample information", Activator.getImageDescriptor("icons/beaker.png")) {
			public void run() {
				sampleInformation();
			}
		};
		IAction showQueue = new Action("Show the scan queue", Activator.getImageDescriptor("icons/cards-stack.png")) {
			public void run() {
				showQueue();
			}
		};
	
		ViewUtil.addGroups("execute", mans, submitAction);
		ViewUtil.addGroups("auxilary", mans, copy, sample, showQueue);

		
		text.setMenu(rightClick.createContextMenu(text));

	}

	private IAction createPreferenceAction(String label, String preference, String icon) {
		IAction ret = new Action(label, IAction.AS_CHECK_BOX) {
			public void run() {
				Activator.getDefault().getPreferenceStore().setValue(preference, isChecked());
				updateJob.schedule();
			}
		};
		ret.setImageDescriptor(Activator.getImageDescriptor(icon));
		ret.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(preference));	
		return ret;
	}

	private void clipboard() {
		try {
		    ScanRequest<IROI> req = createScanRequest();
		    String cmd = ServiceHolder.getParserService().getCommand(req, true);
			Clipboard clipboard = new Clipboard(Display.getDefault());
			clipboard.setContents(new Object[] { cmd }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
			logger.debug("Copied command to clipboard:");
			logger.debug(cmd);
			
		} catch (Exception ne) {
			ErrorDialog.openError(getViewSite().getShell(), 
					"Problem Generating Command", 
					"The mscan(..) command is currently invalid because of the\n"+
					"current stepup. Please fix any errors in the setup.\n\n"+
					"Nothing was copied to the clipboard",
					new Status(IStatus.ERROR, "org.eclipse.scanning.device.ui", ne.getMessage()));
		}
	}

	@Override
	public void setFocus() {
		if (text!=null && !text.isDisposed()) text.setFocus();
	}

	private static class FontStyler extends Styler {
		
		public static final Styler CODE = new FontStyler(new Font(null, "Courier", 10, SWT.NONE));
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

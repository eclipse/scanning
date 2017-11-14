/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.event.ui.view;

import java.math.RoundingMode;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ConsumerConfiguration;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.QueueViews;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.status.AdministratorMessage;
import org.eclipse.scanning.api.event.status.OpenRequest;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.ui.IModifyHandler;
import org.eclipse.scanning.api.ui.IRerunHandler;
import org.eclipse.scanning.api.ui.IResultHandler;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.scanning.event.ui.ServiceHolder;
import org.eclipse.scanning.event.ui.dialog.PropertiesDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A view for which the secondary id MUST be set and provides the queueName
 * and optionally the queue view name if a custom one is required. Syntax of
 * these parameters in the secondary id are key1=value1;key2=value2...
 *
 * The essential keys are: beanBundleName, beanClassName, queueName, topicName, submissionQueueName
 * You can use createId(...) to generate a legal id from them.
 *
 * The optional keys are: partName,
 *                        uri (default CommandConstants.JMS_URI),
 *                        userName (default is user.name system property)
 *
 * Example id for this view would be:
 * org.eclipse.scanning.event.ui.queueView:beanClassName=org.dawnsci.commandserver.mx.beans.ProjectBean;beanBundleName=org.dawnsci.commandserver.mx
 *
 * You can optionally extend this class to provide a table which is displayed for your
 * queue of custom objects. For instance for a queue showing xia2 reruns, the
 * extra columns for this could be defined. However by default the
 *
 * @author Matthew Gerring
 *
 */
@SuppressWarnings({"squid:S1192", "squid:S1659"})
public class StatusQueueView extends EventConnectionView {

	private static final String RERUN_HANDLER_EXTENSION_POINT_ID = "org.eclipse.scanning.api.rerunHandler";

	private static final String MODIFY_HANDLER_EXTENSION_POINT_ID = "org.eclipse.scanning.api.modifyHandler";

	private static final String RESULTS_HANDLER_EXTENSION_POINT_ID = "org.eclipse.scanning.api.resultsHandler";

	public static final String ID = "org.eclipse.scanning.event.ui.queueView";

	private static final Logger logger = LoggerFactory.getLogger(StatusQueueView.class);

	// UI
	private TableViewer                       viewer;
	private DelegatingSelectionProvider       selectionProvider;

	// Data
	private Map<String, StatusBean>           queue;
	private boolean                           hideOtherUsersResults = false;

	private ISubscriber<IBeanListener<StatusBean>>           topicMonitor;
	private ISubscriber<IBeanListener<AdministratorMessage>> adminMonitor;
	private ISubmitter<StatusBean>                           queueConnection;

	private Action rerun, edit, remove, up, down, pause;
	private IEventService service;

	private ISubscriber<EventListener> pauseSubscriber;

	private List<IResultHandler> resultsHandlers = null;

	public StatusQueueView() {
		this.service = ServiceHolder.getEventService();
	}

	@Override
	public void createPartControl(Composite content) {

		content.setLayout(new GridLayout(1, false));
		Util.removeMargins(content);

		this.viewer   = new TableViewer(content, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setUseHashlookup(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createColumns();
		viewer.setContentProvider(createContentProvider());

		try {
			queueConnection = service.createSubmitter(getUri(), getSubmissionQueueName());
			queueConnection.setStatusTopicName(getTopicName());
			updateQueue();

			String name = getSecondaryIdAttribute("partName");
			if (name!=null) setPartName(name);

			createActions();

			// We just use this submitter to read the queue
			createTopicListener(getUri());

		} catch (Exception e) {
			logger.error("Cannot listen to topic of command server!", e);
		}

		selectionProvider = new DelegatingSelectionProvider(viewer);
		getViewSite().setSelectionProvider(selectionProvider);
		viewer.addSelectionChangedListener(event -> updateSelected() );
	}

	protected void updateSelected() {

		for(StatusBean bean : getSelection()) {
			remove.setEnabled(bean.getStatus()!=null);
			rerun.setEnabled(true);

			boolean isSubmitted = bean.getStatus()==org.eclipse.scanning.api.event.status.Status.SUBMITTED;
			up.setEnabled(isSubmitted);
			edit.setEnabled(isSubmitted);
			down.setEnabled(isSubmitted);
			pause.setEnabled(bean.getStatus().isRunning()||bean.getStatus().isPaused());
			pause.setChecked(bean.getStatus().isPaused());
			pause.setText(bean.getStatus().isPaused()?"Resume job":"Pause job");
		}
	}

	/**
	 * Listens to a topic
	 */
	private void createTopicListener(final URI uri) {

		// Use job because connection might timeout.
		final Job topicJob = new Job("Create topic listener") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					topicMonitor = service.createSubscriber(uri, getTopicName());
					topicMonitor.addListener(evt -> {
							final StatusBean bean = evt.getBean();
							try {
								mergeBean(bean);
							} catch (Exception e) {
								logger.error("Cannot merge changed bean!", e);
							}
						});

					adminMonitor = service.createSubscriber(uri, IEventService.ADMIN_MESSAGE_TOPIC);
					adminMonitor.addListener(evt -> {
							final AdministratorMessage bean = evt.getBean();
							getSite().getShell().getDisplay().syncExec(() -> {
								MessageDialog.openError(getViewSite().getShell(),
									bean.getTitle(),
									bean.getMessage());
								viewer.refresh();
							});
						});
					return Status.OK_STATUS;

				} catch (Exception ne) {
					logger.error("Cannot listen to topic changes because command server is not there", ne);
					return Status.CANCEL_STATUS;
				}
			}
		};

		topicJob.setPriority(Job.INTERACTIVE);
		topicJob.setSystem(true);
		topicJob.setUser(false);
		topicJob.schedule();
	}

	@Override
	public void dispose() {
		super.dispose();
		try {
			if (topicMonitor!=null) topicMonitor.disconnect();
			if (adminMonitor!=null) adminMonitor.disconnect();
			if (pauseSubscriber!=null) pauseSubscriber.disconnect();
		} catch (Exception ne) {
			logger.warn("Problem stopping topic listening for "+getTopicName(), ne);
		}
	}

	/**
	 * Updates the bean if it is found in the list, otherwise
	 * refreshes the whole list because a bean we are not reporting
	 * has been(bean?) encountered.
	 *
	 * @param bean
	 */
	protected void mergeBean(final StatusBean bean) {

		getSite().getShell().getDisplay().asyncExec(() -> {
				if (queue.containsKey(bean.getUniqueId())) {
					queue.get(bean.getUniqueId()).merge(bean);
					viewer.refresh();
					updateSelected();
				} else {
					reconnect();
				}
			});
	}

	@SuppressWarnings("squid:S3776")
	private void createActions() throws Exception {

		final IContributionManager toolMan  = getViewSite().getActionBars().getToolBarManager();
		final IContributionManager dropDown = getViewSite().getActionBars().getMenuManager();
		final MenuManager          menuMan = new MenuManager();

		final Action openResults = new Action("Open results for selected run", Activator.getImageDescriptor("icons/results.png")) {
			@Override
			public void run() {
				for (StatusBean bean : getSelection()) {
					openResults(bean);
				}
			}
		};

		toolMan.add(openResults);
		toolMan.add(new Separator());
		menuMan.add(openResults);
		menuMan.add(new Separator());
		dropDown.add(openResults);
		dropDown.add(new Separator());

		this.up = new Action("Less urgent (-1)", Activator.getImageDescriptor("icons/arrow-090.png")) {
			@Override
			public void run() {
				for(StatusBean bean : getSelection()) {
					try {
						queueConnection.reorder(bean, -1);
					} catch (EventException e) {
						ErrorDialog.openError(getViewSite().getShell(), "Cannot move "+bean.getName(), "'"+bean.getName()+"' cannot be moved in the submission queue.",
								new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
					}
				}
				refresh();
			}
		};
		up.setEnabled(false);
		toolMan.add(up);
		menuMan.add(up);
		dropDown.add(up);

		this.down = new Action("More urgent (+1)", Activator.getImageDescriptor("icons/arrow-270.png")) {
			@Override
			public void run() {
				for (StatusBean bean : getSelection()) {
					try {
						queueConnection.reorder(bean, +1);
					} catch (EventException e) {
						ErrorDialog.openError(getViewSite().getShell(), "Cannot move "+bean.getName(), e.getMessage(),
								new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
					}
				}
				refresh();
			}
		};
		down.setEnabled(false);
		toolMan.add(down);
		menuMan.add(down);
		dropDown.add(down);

		this.pause = new Action("Pause job.\nPauses a running job.", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				pauseJob();
			}
		};
		pause.setImageDescriptor(Activator.getImageDescriptor("icons/control-pause.png"));
		pause.setEnabled(false);
		pause.setChecked(false);
		toolMan.add(pause);
		menuMan.add(pause);
		dropDown.add(pause);

		final Action pauseConsumer = new Action("Pause "+getPartName()+" Queue. Does not pause running job.", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				togglePausedConsumer(this);
			}
		};
		pauseConsumer.setImageDescriptor(Activator.getImageDescriptor("icons/control-pause-red.png"));
		pauseConsumer.setChecked(queueConnection.isQueuePaused(getSubmissionQueueName()));
		toolMan.add(pauseConsumer);
		menuMan.add(pauseConsumer);
		dropDown.add(pauseConsumer);

		ISubscriber<IBeanListener<PauseBean>> pauseMonitor = service.createSubscriber(getUri(), EventConstants.CMD_TOPIC);
		pauseMonitor.addListener(evt -> pauseConsumer.setChecked(queueConnection.isQueuePaused(getSubmissionQueueName())));

		this.remove = new Action("Stop job or remove if finished", Activator.getImageDescriptor("icons/control-stop-square.png")) {
			@Override
			public void run() {
				stopJob();
			}
		};
		remove.setEnabled(false);
		toolMan.add(remove);
		menuMan.add(remove);
		dropDown.add(remove);

		this.rerun = new Action("Rerun...", Activator.getImageDescriptor("icons/rerun.png")) {
			@Override
			public void run() {
				rerunSelection();
			}
		};
		rerun.setEnabled(false);
		toolMan.add(rerun);
		menuMan.add(rerun);
		dropDown.add(rerun);

		IAction open = new Action("Open...", Activator.getImageDescriptor("icons/application-dock-090.png")) {
			@Override
			public void run() {
				openSelection();
			}
		};
		toolMan.add(open);
		menuMan.add(open);
		dropDown.add(open);

		this.edit = new Action("Edit...", Activator.getImageDescriptor("icons/modify.png")) {
			@Override
			public void run() {
				editSelection();
			}
		};
		edit.setEnabled(false);
		toolMan.add(edit);
		menuMan.add(edit);
		dropDown.add(edit);

		toolMan.add(new Separator());
		menuMan.add(new Separator());

		final Action hideOtherUsersResultsAction = new Action("Hide other users results", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				hideOtherUsersResults = isChecked();
				viewer.refresh();
			}
		};
		hideOtherUsersResultsAction.setImageDescriptor(Activator.getImageDescriptor("icons/spectacle-lorgnette.png"));

		toolMan.add(hideOtherUsersResultsAction);
		menuMan.add(hideOtherUsersResultsAction);
		dropDown.add(hideOtherUsersResultsAction);

		toolMan.add(new Separator());
		menuMan.add(new Separator());
		dropDown.add(new Separator());

		final Action refresh = new Action("Refresh", Activator.getImageDescriptor("icons/arrow-circle-double-135.png")) {
			@Override
			public void run() {
				reconnect();
			}
		};

		toolMan.add(refresh);
		menuMan.add(refresh);
		dropDown.add(refresh);

		final Action configure = new Action("Configure...", Activator.getImageDescriptor("icons/document--pencil.png")) {
			@Override
			public void run() {
				PropertiesDialog dialog = new PropertiesDialog(getSite().getShell(), idProperties);

				int ok = dialog.open();
				if (ok == PropertiesDialog.OK) {
					idProperties.clear();
					idProperties.putAll(dialog.getProps());
					reconnect();
				}
			}
		};

		toolMan.add(configure);
		menuMan.add(configure);
		dropDown.add(configure);

		final Action clearQueue = new Action("Clear Queue") {
			@Override
			public void run() {
				try {
					purgeQueues();
				} catch (EventException e) {
					logger.error("Canot purge queues", e);
				}
			}
		};
		menuMan.add(new Separator());
		dropDown.add(new Separator());
		menuMan.add(clearQueue);
		dropDown.add(clearQueue);

		viewer.getControl().setMenu(menuMan.createContextMenu(viewer.getControl()));
	}

	protected void togglePausedConsumer(IAction pauseConsumer) {

		// The button can get out of sync if two clients are used.
		final boolean currentState = queueConnection.isQueuePaused(getSubmissionQueueName());
		try {
			pauseConsumer.setChecked(!currentState); // We are toggling it.

			IPublisher<PauseBean> pauser = service.createPublisher(getUri(), IEventService.CMD_TOPIC);
			pauser.setStatusSetName(IEventService.CMD_SET); // The set that other clients may check
			pauser.setStatusSetAddRequired(true);

			PauseBean pbean = new PauseBean();
			pbean.setQueueName(getSubmissionQueueName()); // The queue we are pausing
			pbean.setPause(pauseConsumer.isChecked());
			pauser.broadcast(pbean);

		} catch (Exception e) {
			ErrorDialog.openError(getViewSite().getShell(), "Cannot pause queue "+getSubmissionQueueName(), "Cannot pause queue "+getSubmissionQueueName()+"\n\nPlease contact your support representative.",
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
		pauseConsumer.setChecked(queueConnection.isQueuePaused(getSubmissionQueueName()));
	}

	protected void pauseJob() {

		for(StatusBean bean : getSelection()) {

			if (bean.getStatus().isFinal()) {
				MessageDialog.openInformation(getViewSite().getShell(), "Run '"+bean.getName()+"' inactive", "Run '"+bean.getName()+"' is inactive and cannot be paused.");
				continue;
			}

			try {
				if (bean.getStatus().isPaused()) {
					bean.setStatus(org.eclipse.scanning.api.event.status.Status.REQUEST_RESUME);
					bean.setMessage("Resume of "+bean.getName());
				} else {
					bean.setStatus(org.eclipse.scanning.api.event.status.Status.REQUEST_PAUSE);
					bean.setMessage("Pause of "+bean.getName());
				}

				IPublisher<StatusBean> terminate = service.createPublisher(getUri(), getTopicName());
				terminate.broadcast(bean);

			} catch (Exception e) {
				ErrorDialog.openError(getViewSite().getShell(), "Cannot pause "+bean.getName(), "Cannot pause "+bean.getName()+"\n\nPlease contact your support representative.",
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
			}
		}
	}

	@SuppressWarnings({"squid:S3776", "squid:S135"})
	protected void stopJob() {

		for(StatusBean bean : getSelection()) {

			if (!bean.getStatus().isActive()) {

				String queueName = null;

				if (bean.getStatus()!=org.eclipse.scanning.api.event.status.Status.SUBMITTED) {
					queueName = getQueueName();
					boolean ok = MessageDialog.openQuestion(getSite().getShell(), "Confirm Remove '"+bean.getName()+"'", "Are you sure you would like to remove '"+bean.getName()+"'?");
					if (!ok) continue;
				} else {
					// Submitted delete it right away without asking or the consumer will run it!
					queueName = getSubmissionQueueName();
				}

				// It is submitted and not running. We can probably delete it.
				try {
					queueConnection.remove(bean, queueName);
					refresh();
				} catch (EventException e) {
					ErrorDialog.openError(getViewSite().getShell(), "Cannot delete "+bean.getName(), "Cannot delete "+bean.getName()+"\n\nIt might have changed state at the same time and being remoted.",
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
				continue;
			}

			try {
				final DateFormat format = DateFormat.getDateTimeInstance();
				boolean ok = MessageDialog.openQuestion(getViewSite().getShell(), "Confirm terminate "+bean.getName(),
						  "Are you sure you want to terminate "+bean.getName()+" submitted on "+format.format(new Date(bean.getSubmissionTime()))+"?");

				if (!ok) continue;

				bean.setStatus(org.eclipse.scanning.api.event.status.Status.REQUEST_TERMINATE);
				bean.setMessage("Termination of "+bean.getName());

				IPublisher<StatusBean> terminate = service.createPublisher(getUri(), getTopicName());
				terminate.broadcast(bean);

			} catch (Exception e) {
				ErrorDialog.openError(getViewSite().getShell(), "Cannot terminate "+bean.getName(), "Cannot terminate "+bean.getName()+"\n\nPlease contact your support representative.",
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
			}
		}
	}

	protected void purgeQueues() throws EventException {

		boolean ok = MessageDialog.openQuestion(getSite().getShell(), "Confirm Clear Queues", "Are you sure you would like to remove all items from the queue "+getQueueName()+" and "+getSubmissionQueueName()+"?\n\nThis could abort or disconnect runs of other users.");
		if (!ok) return;

		queueConnection.clearQueue(getQueueName());
		queueConnection.clearQueue(getSubmissionQueueName());
		reconnect();
	}

	private List<IResultHandler> getResultsHandlers() {
		if (resultsHandlers == null) {
			final IConfigurationElement[] configElements = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(RESULTS_HANDLER_EXTENSION_POINT_ID);
			final List<IResultHandler> handlers = new ArrayList<>(configElements.length + 1);
			for (IConfigurationElement configElement : configElements) {
				try {
					final IResultHandler handler = (IResultHandler) configElement.createExecutableExtension("class");
					handler.init(service, createConsumerConfiguration());
					handlers.add(handler);
				} catch (Exception e) {
					ErrorDialog.openError(getSite().getShell(), "Internal Error",
							"Could not create results handler for class " + configElement.getAttribute("class") +
							".\n\nPlease contact your support representative.",
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
			}
			handlers.add(new DefaultResultsHandler());
			resultsHandlers = handlers;
		}
		return resultsHandlers;
	}

	/**
	 * You can override this method to provide custom opening of
	 * results if required.
	 *
	 * @param bean
	 */
	protected void openResults(StatusBean bean) {

		if (bean == null) return;

		for (IResultHandler handler : getResultsHandlers()) {
			if (handler.isHandled(bean)) {
				try {
					boolean ok = handler.open(bean);
					if (ok) return;
				} catch (Exception e) {
					ErrorDialog.openError(getSite().getShell(), "Internal Error", handler.getErrorMessage(null),
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
			}
		}
	}

	/**
	 * Pushes any previous run back into the UI
	 */
	protected void openSelection() {

		final StatusBean [] beans = getSelection();
		if (beans.length == 0) {
			MessageDialog.openInformation(getViewSite().getShell(), "Please select a run", "Please select a run to open.");
			return;
		}

		// TODO FIXME Change to IScanBuilderService not selections so that it works with e4.
		// We fire a special object into the selection mechanism with the data for this run.
		// It is then up to parts to respond to this selection and update their contents.
		// We call fireSelection as the openRequest isn't in the table. This sets the workb
		for (StatusBean bean : beans) {
			selectionProvider.fireSelection(new StructuredSelection(new OpenRequest(bean)));
		}
	}

	/**
	 * Edits a not run yet selection
	 */
	@SuppressWarnings({"squid:S3776", "squid:S135"})
	protected void editSelection() {

		for (StatusBean bean : getSelection()) {
			if (bean.getStatus()!=org.eclipse.scanning.api.event.status.Status.SUBMITTED) {
				MessageDialog.openConfirm(getSite().getShell(), "Cannot Edit '"+bean.getName()+"'", "The run '"+bean.getName()+"' cannot be edited because it is not waiting to run.");
				continue;
			}

			try {
				final IConfigurationElement[] c = Platform.getExtensionRegistry().getConfigurationElementsFor(MODIFY_HANDLER_EXTENSION_POINT_ID);
				if (c!=null) {
					for (IConfigurationElement i : c) {
						final IModifyHandler handler = (IModifyHandler)i.createExecutableExtension("class");
						handler.init(service, createConsumerConfiguration());
						if (handler.isHandled(bean)) {
							boolean ok = handler.modify(bean);
							if (ok) continue;
						}
					}
				}
			} catch (Exception ne) {
				final String err = "Cannot modify "+bean.getRunDirectory()+" normally.\n\nPlease contact your support representative.";
				logger.error(err, ne);
				ErrorDialog.openError(getSite().getShell(), "Internal Error", err, new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage()));
				continue;
			}
			MessageDialog.openConfirm(getSite().getShell(), "Cannot Edit '"+bean.getName()+"'", "There are no editers registered for '"+bean.getName()+"'\n\nPlease contact your support representative.");
		}
	}

	@SuppressWarnings("squid:S3776")
	protected void rerunSelection() {

		for (StatusBean bean : getSelection()) {
			try {
				final IConfigurationElement[] c = Platform.getExtensionRegistry().getConfigurationElementsFor(RERUN_HANDLER_EXTENSION_POINT_ID);
				if (c!=null) {
					for (IConfigurationElement i : c) {
						final IRerunHandler handler = (IRerunHandler)i.createExecutableExtension("class");
						handler.init(service, createConsumerConfiguration());
						if (handler.isHandled(bean)) {
							final StatusBean copy = bean.getClass().newInstance();
							copy.merge(bean);
							copy.setUniqueId(UUID.randomUUID().toString());
							copy.setStatus(org.eclipse.scanning.api.event.status.Status.SUBMITTED);
							copy.setSubmissionTime(System.currentTimeMillis());
							boolean ok = handler.run(copy);
							if (ok) continue;
						}
					}
				}
			} catch (Exception ne) {
				final String err = "Cannot rerun "+bean.getRunDirectory()+" normally.\n\nPlease contact your support representative.";
				logger.error(err, ne);
				ErrorDialog.openError(getSite().getShell(), "Internal Error", err, new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage()));
				continue;
			}
			// If we have not already handled this rerun, it is possible to call a generic one.
			rerun(bean);
		}
	}

	private ConsumerConfiguration createConsumerConfiguration() throws Exception {
		return new ConsumerConfiguration(getUri(), getSubmissionQueueName(), getTopicName(), getQueueName());
	}

	private void rerun(StatusBean bean) {

		try {
			final DateFormat format = DateFormat.getDateTimeInstance();
			boolean ok = MessageDialog.openQuestion(getViewSite().getShell(), "Confirm resubmission "+bean.getName(),
					  "Are you sure you want to rerun "+bean.getName()+" submitted on "+format.format(new Date(bean.getSubmissionTime()))+"?");

			if (!ok) return;

			final StatusBean copy = bean.getClass().newInstance();
			copy.merge(bean);
			copy.setUniqueId(UUID.randomUUID().toString());
			copy.setMessage("Rerun of "+bean.getName());
			copy.setStatus(org.eclipse.scanning.api.event.status.Status.SUBMITTED);
			copy.setPercentComplete(0.0);
			copy.setSubmissionTime(System.currentTimeMillis());

			queueConnection.submit(copy, true);

			reconnect();

		} catch (Exception e) {
			ErrorDialog.openError(getViewSite().getShell(), "Cannot rerun "+bean.getName(), "Cannot rerun "+bean.getName()+"\n\nPlease contact your support representative.",
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
	}

	public void refresh() {
		reconnect();
		updateSelected();
	}

	protected void reconnect() {
		try {
			updateQueue();
		} catch (Exception e) {
			logger.error("Cannot resolve uri for activemq server of "+getSecondaryIdAttribute("uri"));
		}
	}

	@SuppressWarnings("squid:S3776")
	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				queue = (Map<String, StatusBean>)newInput;
			}

			@Override
			public void dispose() {
				if (queue!=null) queue.clear();
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (queue==null) return new StatusBean[]{StatusBean.EMPTY};
				final List<StatusBean> retained = new ArrayList<>(queue.values());

				// This preference is not secure people could hack DAWN to do this.
				if (!Boolean.getBoolean("org.dawnsci.commandserver.ui.view.showWholeQueue")) {
					// Old fashioned loop. In Java8 we will use a predicate...
					final String userName = getUserName();
					for (Iterator it = retained.iterator(); it.hasNext();) {
						StatusBean statusBean = (StatusBean) it.next();
						if (statusBean.getUserName()==null) continue;
						if (hideOtherUsersResults && !userName.equals(statusBean.getUserName())) it.remove();
					}
					// This form of filtering is not at all secure because we
					// give the full list of the queue to the clients.
				}
				return retained.toArray(new StatusBean[retained.size()]);
			}
		};
	}

	protected StatusBean [] getSelection() {
		final ISelection sel = viewer.getSelection();
		IStructuredSelection ss = (IStructuredSelection)sel;
		return Arrays.stream(ss.toArray()).toArray(StatusBean[]::new);
	}

	/**
	 * Read Queue and return in submission order.
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	protected synchronized void updateQueue() {

		final Job queueJob = new Job("Connect and read queue") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Connect to command server", 10);
					monitor.worked(1);

					queueConnection.setBeanClass(getBeanClass());
					List<StatusBean> runningList = queueConnection.getQueue(getQueueName(), null);
					Collections.reverse(runningList); // The list comes out with the head @ 0 but we have the last submitted at 0 in our table.
					monitor.worked(1);

					List<StatusBean> submittedList = queueConnection.getQueue(getSubmissionQueueName(), null);
					Collections.reverse(submittedList); // The list comes out with the head @ 0 but we have the last submitted at 0 in our table.
					monitor.worked(1);

					// We reverse the queue because it comes out date ascending and we want newest submissions first.
					final Map<String,StatusBean> ret = new LinkedHashMap<>();
					for (StatusBean bean : submittedList) {
						ret.put(bean.getUniqueId(), bean);
					}
					monitor.worked(1);
					for (StatusBean bean : runningList) {
						ret.put(bean.getUniqueId(), bean);
					}
					monitor.worked(1);

					getSite().getShell().getDisplay().syncExec(() -> {
							viewer.setInput(ret);
							viewer.refresh();
						});
					monitor.done();

					return Status.OK_STATUS;

				} catch (final Exception e) {
					monitor.done();
					logger.error("Updating changed bean from topic", e);
					getSite().getShell().getDisplay().syncExec(() ->
							ErrorDialog.openError(getViewSite().getShell(), "Cannot connect to queue", "The server is unavailable at "+getUriString()+".\n\nPlease contact your support representative.",
									new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()))
						);
					return Status.CANCEL_STATUS;
				}
			}
		};
		queueJob.setPriority(Job.INTERACTIVE);
		queueJob.setUser(true);
		queueJob.schedule();
	}

	private Class<StatusBean> getBeanClass() {
		String beanBundleName = getSecondaryIdAttribute("beanBundleName");
		String beanClassName  = getSecondaryIdAttribute("beanClassName");
		try {

			Bundle bundle = Platform.getBundle(beanBundleName);
			return (Class<StatusBean>)bundle.loadClass(beanClassName);
		} catch (Exception ne) {
			logger.error("Cannot get class "+beanClassName+". Defaulting to StatusBean. This will probably not work though.", ne);
			return StatusBean.class;
		}
	}

	@SuppressWarnings("squid:S3776")
	protected void createColumns() {

		final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(260);
		name.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((StatusBean)element).getName();
			}
		});

		final TableViewerColumn status = new TableViewerColumn(viewer, SWT.LEFT);
		status.getColumn().setText("Status");
		status.getColumn().setWidth(80);
		status.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((StatusBean)element).getStatus().toString();
			}
		});

		final TableViewerColumn pc = new TableViewerColumn(viewer, SWT.CENTER);
		pc.getColumn().setText("Complete");
		pc.getColumn().setWidth(70);
		final NumberFormat percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setRoundingMode(RoundingMode.DOWN);
		pc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return percentFormat.format(((StatusBean)element).getPercentComplete()/100d);
				} catch (Exception ne) {
					return "-";
				}
			}
		});

		final TableViewerColumn submittedDate = new TableViewerColumn(viewer, SWT.CENTER);
		submittedDate.getColumn().setText("Date Submitted");
		submittedDate.getColumn().setWidth(120);
		submittedDate.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((StatusBean)element).getSubmissionTime()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn message = new TableViewerColumn(viewer, SWT.LEFT);
		message.getColumn().setText("Message");
		message.getColumn().setWidth(150);
		message.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return ((StatusBean)element).getMessage();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn location = new TableViewerColumn(viewer, SWT.LEFT);
		location.getColumn().setText("Location");
		location.getColumn().setWidth(300);
		location.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					final StatusBean bean = (StatusBean)element;
					return getLocation(bean);
				} catch (Exception e) {
					return e.getMessage();
				}
			}

			@Override
			public Color getForeground(Object element) {
				boolean isFinal = ((StatusBean) element).getStatus().isFinal();
				return getSite().getShell().getDisplay().getSystemColor(isFinal ? SWT.COLOR_BLUE : SWT.COLOR_BLACK);
			}
		});

		final TableViewerColumn host = new TableViewerColumn(viewer, SWT.CENTER);
		host.getColumn().setText("Host");
		host.getColumn().setWidth(90);
		host.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return ((StatusBean)element).getHostName();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn user = new TableViewerColumn(viewer, SWT.CENTER);
		user.getColumn().setText("User Name");
		user.getColumn().setWidth(80);
		user.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return ((StatusBean)element).getUserName();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn startTime = new TableViewerColumn(viewer, SWT.CENTER);
		startTime.getColumn().setText("Start Time");
		startTime.getColumn().setWidth(120);
		startTime.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					long statusStartTime = ((StatusBean)element).getStartTime();
					if (statusStartTime == 0) return "";
					return DateFormat.getTimeInstance().format(new Date(statusStartTime));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn estimatedEndTime = new TableViewerColumn(viewer, SWT.CENTER);
		estimatedEndTime.getColumn().setText("E. End Time");
		estimatedEndTime.getColumn().setWidth(120);
		estimatedEndTime.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					long statusEstimatedEndTime = ((StatusBean)element).getStartTime() + ((StatusBean)element).getEstimatedTime();
					if (statusEstimatedEndTime == 0) return "";
					return DateFormat.getTimeInstance().format(new Date(statusEstimatedEndTime));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		MouseMoveListener cursorListener = e -> {
				Point pt = new Point(e.x, e.y);
				TableItem item = viewer.getTable().getItem(pt);

				Cursor cursor = null;
				if (item != null && item.getBounds(5).contains(pt)) {
					StatusBean statusBean = (StatusBean) item.getData();
					if (statusBean != null && getLocation(statusBean) != null && statusBean.getStatus().isFinal()) {
						cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_HAND);
					}
				}
				viewer.getTable().setCursor(cursor);
			};

		viewer.getTable().addMouseMoveListener(cursorListener);

		MouseAdapter mouseClick = new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = viewer.getTable().getItem(pt);
				if (item == null) return;
				Rectangle rect = item.getBounds(5);
				if (rect.contains(pt)) {
					final StatusBean bean = (StatusBean)item.getData();
					if (bean.getStatus().isFinal())
						openResults(bean);
				}
			}
		};
		viewer.getTable().addMouseListener(mouseClick);
	}

	@Override
	public void setFocus() {
		if (!viewer.getTable().isDisposed()) {
			viewer.getTable().setFocus();
		}
	}

	public static String createId(final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {

		final StringBuilder buf = new StringBuilder();
		buf.append(ID);
		buf.append(":");
		buf.append(QueueViews.createSecondaryId(beanBundleName, beanClassName, queueName, topicName, submissionQueueName));
		return buf.toString();
	}

	public static String createId(final String uri, final String beanBundleName, final String beanClassName, final String queueName, final String topicName, final String submissionQueueName) {

		final StringBuilder buf = new StringBuilder();
		buf.append(ID);
		buf.append(":");
		buf.append(QueueViews.createSecondaryId(uri, beanBundleName, beanClassName, queueName, topicName, submissionQueueName));
		return buf.toString();
	}

	private String getLocation(final StatusBean statusBean) {
		if (statusBean instanceof ScanBean) return ((ScanBean)statusBean).getFilePath();
		return statusBean.getRunDirectory();
	}
}

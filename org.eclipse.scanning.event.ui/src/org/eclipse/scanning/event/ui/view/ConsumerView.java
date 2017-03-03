/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.event.ui.view;

import java.net.URI;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.ConsumerBean;
import org.eclipse.scanning.api.event.alive.ConsumerStatus;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.AdministratorMessage;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.scanning.event.ui.ServiceHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A view which shows the active consumers available to process commands.
 * 
 * @author Matthew Gerring
 *
 */
public class ConsumerView extends EventConnectionView {
	
	public static final String ID = "org.eclipse.scanning.event.ui.consumerView";
	
	private static final Logger logger = LoggerFactory.getLogger(ConsumerView.class);
	
	// UI
	private TableViewer                       viewer;
	
	// Data
	private Map<String, HeartbeatBean>        consumers;

	private ISubscriber<IHeartbeatListener>          heartMonitor;
	private ISubscriber<IBeanListener<ConsumerBean>> oldBeat;

	private IEventService service;
	
	public ConsumerView() {
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
		
		consumers = new ConcurrentHashMap<>();
		viewer.setInput(consumers);	
		
        createActions();
        try {
			createTopicListener(new URI(Activator.getJmsUri()));
		} catch (Exception e) {
			logger.error("Cannot listen to topic of command server!", e);
		}
        
        final String partName = getSecondaryIdAttribute("partName");
        if (partName!=null) setPartName(partName);
	}
	
	/**
	 * Listens to a topic
	 */
	private void createTopicListener(final URI uri) throws Exception {
		
		// Use job because connection might timeout.
		final Job topicJob = new Job("Create topic listener") {

			@SuppressWarnings("deprecation")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {					
					heartMonitor = service.createSubscriber(uri, IEventService.HEARTBEAT_TOPIC);
					heartMonitor.addListener(new IHeartbeatListener() {
						@Override
						public void heartbeatPerformed(HeartbeatEvent evt) {
							
							HeartbeatBean bean = evt.getBean();
	        				bean.setLastAlive(System.currentTimeMillis());
	        				sync(bean);
						}

					});
					
					// This subscriber should be removed around DAWN 2.2 please. There will not be
					// any more old consumers doing this then.
					oldBeat = service.createSubscriber(uri, "scisoft.commandserver.core.ALIVE_TOPIC");
					oldBeat.addListener(new IBeanListener<ConsumerBean>() {
						// Old heartbeat
						public void beanChangePerformed(BeanEvent<ConsumerBean> evt) {
							
							ConsumerBean cbean = evt.getBean();
							HeartbeatBean bean  = cbean.toHeartbeat();
	        				bean.setLastAlive(System.currentTimeMillis());
	        				sync(bean);
						}
                        public Class<ConsumerBean> getBeanClass() {
                        	return ConsumerBean.class;
                        }
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
	
	private void sync(HeartbeatBean bean) {
		
		HeartbeatBean old = consumers.put(bean.getUniqueId(), bean);
		if (!bean.equalsIgnoreLastAlive(old)) {
			viewer.getControl().getDisplay().syncExec(new Runnable() {
				public void run () {
					viewer.refresh();
				}
			});
		}					
	}

	
	public void dispose() {
		super.dispose();
		try {
			if (heartMonitor!=null) heartMonitor.disconnect();
		} catch (Exception ne) {
			logger.warn("Problem stopping topic listening for "+heartMonitor.getTopicName(), ne);
		}
		try {
			if (oldBeat!=null) oldBeat.disconnect();
		} catch (Exception ne) {
			logger.warn("Problem stopping topic listening for "+oldBeat.getTopicName(), ne);
		}
	}

	private void createActions() {
		final IContributionManager man = getViewSite().getActionBars().getToolBarManager();
	
		final Action refresh = new Action("Refresh", Activator.getImageDescriptor("icons/arrow-circle-double-135.png")) {
			public void run() {
				consumers.clear();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("Refreshing consumers that we can process", e);
				}
				viewer.refresh();
			}
		};
		
		man.add(refresh);

		final Action restart = new Action("Restart consumer", Activator.getImageDescriptor("icons/control-power.png")) {
			public void run() {
				restart();
			}
		};
		man.add(restart);
	
		Action stop = null;
		if (Boolean.getBoolean("org.eclipse.scanning.consumer.view.showHardStop")) {
		    stop = new Action("Stop consumer", Activator.getImageDescriptor("icons/terminate.png")) {
				public void run() {
					stop();
				}
			};
		}
		
		if (stop!=null) man.add(stop);

		final MenuManager menuMan = new MenuManager();
		menuMan.add(refresh);
		menuMan.add(restart);
		if (stop!=null) menuMan.add(stop);
		
		viewer.getControl().setMenu(menuMan.createContextMenu(viewer.getControl()));
		
	}
	
	private void stop() {
				
	    HeartbeatBean bean = getSelection();
	    if (bean==null) return;

	    boolean ok = MessageDialog.openConfirm(getSite().getShell(), "Confirm Stop", "If you stop this consumer it will have to be restarted by an administrator using a server hard restart.\n\n"
				                                                                      + "Are you sure that you want to do this?\n\n"
				                                                                      + "(NOTE: Long running jobs can be terminated without stopping the consumer!)");
	    if (!ok) return;
	    
	    
	    boolean notify = MessageDialog.openQuestion(getSite().getShell(), "Warn Users", "Would you like to warn users before stopping the consumer?\n\n"
						                        + "If you say yes, a popup will open on users clients to warn about the imminent stop.");
        if (notify) {
        	
        	final AdministratorMessage msg = new AdministratorMessage();
        	msg.setTitle("'"+bean.getConsumerName()+"' will shutdown.");
        	msg.setMessage("'"+bean.getConsumerName()+"' is about to shutdown.\n\n"+
        	               "Any runs corrently running may loose progress notification,\n"+
        			       "however they should complete.\n\n"+
        	               "Runs yet to be started will be picked up when\n"+
        	               "'"+bean.getConsumerName()+"' restarts.");
        	try {
        		final IPublisher<AdministratorMessage> send = service.createPublisher(new URI(Activator.getJmsUri()), IEventService.ADMIN_MESSAGE_TOPIC);
        		send.broadcast(msg);
			} catch (Exception e) {
				logger.error("Cannot notify of shutdown!", e);
			}
        }

	    final KillBean kbean = new KillBean();
		kbean.setMessage("Requesting a termination of "+bean.getConsumerName());
	    kbean.setConsumerId(bean.getConsumerId());
		send(bean, kbean);

	}
	
	private void restart() {
		
	    HeartbeatBean bean = getSelection();
	    if (bean==null) return;
	    
	    boolean ok = MessageDialog.openConfirm(getSite().getShell(), "Confirm Retstart", "If you restart this consumer other people's running jobs may be lost.\n\n"
				                                                                      + "Are you sure that you want to continue?");
	    if (!ok) return;
	    
	    final KillBean kbean = new KillBean();
	    kbean.setExitProcess(false);
		kbean.setMessage("Requesting a restart of "+bean.getConsumerName());
	    kbean.setConsumerId(bean.getConsumerId());
	    kbean.setRestart(true);
		send(bean, kbean); 
		
		consumers.clear();
        viewer.refresh();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.error("Refreshing consumers that we can process", e);
		}
        viewer.refresh();
	}

	private HeartbeatBean getSelection() {
		if (viewer.getSelection() == null || viewer.getSelection().isEmpty()) return null;
		return (HeartbeatBean)((IStructuredSelection)viewer.getSelection()).getFirstElement();
	}

	private void send(HeartbeatBean bean, KillBean kbean) {

		try {
			final IPublisher<KillBean> send = service.createPublisher(new URI(Activator.getJmsUri()), IEventService.CMD_TOPIC);
			send.broadcast(kbean);
		} catch (Exception e) {
			logger.error("Cannot terminate consumer "+bean.getConsumerName(), e);
		}
	}

	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
				if (consumers!=null) consumers.clear();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (consumers==null) return new HeartbeatBean[]{HeartbeatBean.EMPTY};
				final List<HeartbeatBean> beats = new ArrayList<>(consumers.values());
				Collections.sort(beats, new Comparator<HeartbeatBean>() {
					public int compare(HeartbeatBean o1, HeartbeatBean o2) {
						return (int)(o2.getConceptionTime()-o1.getConceptionTime());
					}
				});
				return beats.toArray(new HeartbeatBean[consumers.size()]);
			}
		};
	}

	private final static long HOUR_IN_MS = 60*60*1000;

	protected void createColumns() {
		
		final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(300);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((HeartbeatBean)element).getConsumerName();
			}
		});
		
		final TableViewerColumn status = new TableViewerColumn(viewer, SWT.CENTER);
		status.getColumn().setText("Status");
		status.getColumn().setWidth(100);
		status.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				final HeartbeatBean cbean = (HeartbeatBean)element;
				ConsumerStatus status = cbean.getConsumerStatus();
				
				if (status==ConsumerStatus.ALIVE) {
					if (cbean.getLastAlive()>(System.currentTimeMillis()-UIConstants.NOTIFICATION_FREQUENCY*10) && 
						cbean.getLastAlive()<(System.currentTimeMillis()-UIConstants.NOTIFICATION_FREQUENCY*2)) {
						status = ConsumerStatus.STOPPING;
						
					} else if (cbean.getLastAlive()<(System.currentTimeMillis()-UIConstants.NOTIFICATION_FREQUENCY*10)) {
						status = ConsumerStatus.STOPPED;
					}
				}
				return status.toString();
			}
		});

		final TableViewerColumn startDate = new TableViewerColumn(viewer, SWT.CENTER);
		startDate.getColumn().setText("Date Started");
		startDate.getColumn().setWidth(150);
		startDate.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((HeartbeatBean)element).getConceptionTime()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});
		
		final TableViewerColumn host = new TableViewerColumn(viewer, SWT.CENTER);
		host.getColumn().setText("Host");
		host.getColumn().setWidth(150);
		host.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return ((HeartbeatBean)element).getHostName();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		
		final TableViewerColumn lastAlive = new TableViewerColumn(viewer, SWT.CENTER);
		lastAlive.getColumn().setText("Last Alive");
		lastAlive.getColumn().setWidth(150);
		lastAlive.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((HeartbeatBean)element).getLastAlive()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn age = new TableViewerColumn(viewer, SWT.CENTER);
		age.getColumn().setText("Age");
		age.getColumn().setWidth(150);
		age.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					final HeartbeatBean cbean = (HeartbeatBean)element;
					long time = cbean.getLastAlive()-cbean.getConceptionTime();
        			Format format = (time<HOUR_IN_MS) ? new SimpleDateFormat("mm'm' ss's'") : new SimpleDateFormat("dd'd' mm'm' ss's'");
					return format.format(new Date(time));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});


	}

	@Override
	public void setFocus() {
		if (!viewer.getTable().isDisposed()) {
			viewer.getTable().setFocus();
		}
	}

}

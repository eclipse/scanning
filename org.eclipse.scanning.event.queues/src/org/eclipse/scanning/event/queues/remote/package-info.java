/**
 * 
 * This package contains classes which allow remote control of the 
 * {@link IQueueService} through the {@link IQueueControllerService} by a 
 * client. Most of the interactions are mediated though the 
 * {@link AbstractQueueResponseProcess}. The QueueReponseCreator is the runner of the 
 * {@link IResponder} associated with the {@link IQueueService} and is 
 * responsible for creating the correct type of {@link AbstractQueueResponseProcess}. 
 *
 */
package org.eclipse.scanning.event.queues.remote;
/**
 * 
 * This package is for implementations of remote objects which connect back to the
 * server or servers using JMS rather than a direct connection.
 * 
 * It is not clear if we want Solstice to be delivering client-side services this
 * way in the future. The hand coding of post and response which these services are par of, has advantages and
 * disadvantages. Current the design meets the requirement of server without an endpoint (multiple servers)
 * and allows any technology like python/stomp to interact with it. However the Java client design then
 * becomes a little inelegant because the services have these remote versions implemented. We currently cannot
 * use OSGi remote services for instance because we want the interactions with the server to be possible from
 * any messaging technology i.e. over anything which can deal with JSON and any of the many messaging
 * which ActiveMQ supports.
 * 
 */
package org.eclipse.scanning.event.remote;


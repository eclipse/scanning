package org.eclipse.scanning.api.scan;

/**
 * The role of the level when it is run
 * 
 * @author Matthew Gerring
 *
 */
public enum LevelRole {

	/**
	 * The level is running something like a detector.
	 */
	RUN, 
	
	/**
	 * The level is writing something like information to a NeXus file.
	 */
	WRITE, 
	
	/**
	 * The level is moving hardware.
	 */
	MOVE;
}

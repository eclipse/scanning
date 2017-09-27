package org.eclipse.scanning.api.database;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * The Operations which may proceed on the database connection.
 * 
 * @author Matthew Gerring
 *
 */
public enum Operation {
	INSERT, UPSERT, UPDATE, COMPOSITE;
	
	public static List<Operation> ends() {
		return Arrays.asList(INSERT, UPSERT, UPDATE); // Order matters
	}
}


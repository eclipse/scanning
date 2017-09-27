package org.eclipse.scanning.api.database;

/**
 * 
 * This class is a database 
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
@FunctionalInterface
public interface DatabaseOperation<T> {
	public Id operate(T bean) throws Exception;
}
package org.eclipse.scanning.api.event.queues.models;

/**
 * Exception thrown during evaluation of the model arguments or during 
 * construction of beans from a model.
 * 
 * @author Michael Wharmby
 *
 */
public class QueueModelException extends RuntimeException {

	private static final long serialVersionUID = -9079623888539642342L;

	public QueueModelException() {
		super();
	}

	public QueueModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public QueueModelException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public QueueModelException(String message) {
		super(message);
	}

	public QueueModelException(Throwable throwable) {
		super(throwable);
	}
	
	

}

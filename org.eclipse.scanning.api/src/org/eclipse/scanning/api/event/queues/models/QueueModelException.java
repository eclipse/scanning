package org.eclipse.scanning.api.event.queues.models;

/**
 * Exception thrown during construction of models or beans from a model.
 *
 * @author Michael Wharmby
 *
 */
public class QueueModelException extends Exception {

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

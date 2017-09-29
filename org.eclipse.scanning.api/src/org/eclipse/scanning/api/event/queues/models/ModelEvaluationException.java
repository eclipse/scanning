package org.eclipse.scanning.api.event.queues.models;

/**
 * Exception thrown during evaluation of the model arguments.
 *
 * @author Michael Wharmby
 *
 */
public class ModelEvaluationException extends RuntimeException {

	private static final long serialVersionUID = 5563470388326655604L;

	public ModelEvaluationException() {
		super();
	}

	public ModelEvaluationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ModelEvaluationException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ModelEvaluationException(String message) {
		super(message);
	}

	public ModelEvaluationException(Throwable throwable) {
		super(throwable);
	}

}

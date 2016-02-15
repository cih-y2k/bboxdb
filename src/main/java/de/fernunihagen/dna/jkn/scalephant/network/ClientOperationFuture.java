package de.fernunihagen.dna.jkn.scalephant.network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientOperationFuture<V> implements OperationFuture<V> {
	
	/**
	 * The id of the operation
	 */
	protected short requestId;
	
	/**
	 * The result of the operation
	 */
	protected volatile V operationResult = null;
	
	/**
	 * The mutex for sync operations
	 */
	protected final Object mutex = new Object();
	
	/**
	 * Empty constructor
	 */
	public ClientOperationFuture() {
	}
	
	/**
	 * Constructor with the request id
	 * @param requestId
	 */
	public ClientOperationFuture(final short requestId) {
		this.requestId = requestId;
	}
	
	/**
	 * Canceling is not supported
	 */
	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	/**
	 * Canceling is not supported
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return operationResult != null;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		synchronized (mutex) {
			while(operationResult == null) {
				mutex.wait();
			}
		}
		
		return operationResult;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		
		synchronized (mutex) {
			while(operationResult == null) {
				mutex.wait(unit.toMillis(timeout));
			}
		}
		
		return operationResult;
	}

	/**
	 * Returns the request id
	 */
	@Override
	public short getRequestId() {
		return requestId;
	}

	/**
	 * Set the result of the operation
	 */
	@Override
	public void setOperationResult(final V result) {
		synchronized (mutex) {
			this.operationResult = result;
			mutex.notifyAll();
		}
	}

	/**
	 * Set the ID of the request
	 */
	@Override
	public void setRequestId(final short requestId) {
		this.requestId = requestId;
	}
}

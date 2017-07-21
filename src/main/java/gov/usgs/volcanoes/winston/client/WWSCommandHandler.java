package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class WWSCommandHandler {
	protected final Semaphore sem;

	public WWSCommandHandler() {
		sem = new Semaphore(0);
	}
	/** Process resonse from winston */
	public abstract void handle(Object msg) throws IOException;
	
	/**
	 * Block until the handler has received and processed server response.
	 * @throws InterruptedException 
	 */
	public void responseWait() throws InterruptedException {
		sem.acquire();
	}
}

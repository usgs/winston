package gov.usgs.volcanoes.winston.client;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Receive and process response from winston.
 *
 * @author Tom Parker
 */
public abstract class WWSCommandHandler {
	protected final Semaphore sem;

	public WWSCommandHandler() {
		sem = new Semaphore(0);
	}
	/** Process resonse from winston */
	public abstract void handle(Object msg) throws IOException;
	
	/**
	 * Block until the handler has received and processed server response.
	 * @throws InterruptedException when receives InterruptedException
	 */
	public void responseWait() throws InterruptedException {
		sem.acquire();
	}
}

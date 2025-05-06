// src/com/mcic/anbalytics/wavemetadata/util/RetryHandler.java
package com.mcic.analytics.wavemetadata.util;

import java.util.concurrent.Callable;

/**
 * Utility for retrying actions with exponential backoff.
 */
public class RetryHandler {
    private final ProgressLogger logger = new ProgressLogger(RetryHandler.class);

    /**
     * Executes the given action, retrying up to maxRetries on exception.
     * Retries use exponential backoff starting at 1s.
     */
    public <T> T executeWithRetry(Callable<T> action, int maxRetries) {
        int attempt = 1;
        long delay = 1000L;
        while (true) {
            try {
                return action.call();
            } catch (Exception e) {
                if (attempt >= maxRetries) {
                    logger.error("Action failed after " + attempt + " attempts", e);
                    throw new RuntimeException(e);
                }
                logger.error("Attempt " + attempt + " failed, retrying in " + delay + "ms", e);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
                attempt++;
                delay *= 2;
            }
        }
    }
}

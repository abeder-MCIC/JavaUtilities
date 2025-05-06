// src/com/mcic/anbalytics/wavemetadata/util/ProgressLogger.java
package com.mcic.anbalytics.wavemetadata.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple SLF4J-backed progress reporting.
 */
public class ProgressLogger {
    private final Logger logger;

    public ProgressLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /** Logs an informational message. */
    public void info(String msg) {
        logger.info(msg);
    }

    /** Logs an error with context and exception. */
    public void error(String context, Exception e) {
        logger.error(context, e);
    }
}

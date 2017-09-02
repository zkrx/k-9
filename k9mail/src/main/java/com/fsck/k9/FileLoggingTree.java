package com.fsck.k9;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import timber.log.Timber;


public class FileLoggingTree extends Timber.DebugTree {

    private static Logger mLogger = LoggerFactory.getLogger(FileLoggingTree.class);
    private final boolean enableDebugLogging;

    FileLoggingTree(boolean enableDebugLogging) {
        super();
        this.enableDebugLogging = enableDebugLogging;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.VERBOSE) {
            return;
        }
        if (priority == Log.DEBUG && !enableDebugLogging) {
            return;
        }

        String logMessage = tag + ": " + message;
        switch (priority) {
            case Log.DEBUG:
                mLogger.debug(logMessage);
                break;
            case Log.INFO:
                mLogger.info(logMessage);
                break;
            case Log.WARN:
                mLogger.warn(logMessage);
                break;
            case Log.ERROR:
                mLogger.error(logMessage);
                break;
        }
    }
}
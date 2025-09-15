package com.valmet.watermark.logging;

public class ErrorMessageInfo {

    /**
     * Log level.
     */
    private LogLevel level;

    /**
     * Message.
     */
    private String message;

    /**
     * Constructor.
     *
     * @param level   Log level
     * @param message message
     */
    ErrorMessageInfo (LogLevel level, String message) {
        this.level = level;
        this.message = message;
    }

    /**
     * @return the level
     */
    LogLevel getLevel () {
        return level;
    }

    /**
     * @return the message
     */
    public String getMessageTemplate () {
        return message;
    }
}

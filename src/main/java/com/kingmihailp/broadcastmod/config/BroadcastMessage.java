package com.kingmihailp.broadcastmod.config;

/**
 * A single named broadcast entry: the text to send and how often to send it.
 */
public class BroadcastMessage {

    private final String text;
    private final int    intervalSeconds;

    public BroadcastMessage(String text, int intervalSeconds) {
        this.text            = text;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    public String getText()            { return text; }
    public int    getIntervalSeconds() { return intervalSeconds; }
}

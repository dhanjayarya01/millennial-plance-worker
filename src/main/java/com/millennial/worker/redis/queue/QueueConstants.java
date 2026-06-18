package com.millennial.worker.redis.queue;

public final class QueueConstants {
    public static final String DEADLINE_REMINDERS_QUEUE = "millennial:deadline-reminders";
    public static final String NOTIFICATIONS_QUEUE = "millennial:notifications";
    public static final String EMAILS_QUEUE = "millennial:emails";

    private QueueConstants() {}
}

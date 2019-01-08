package com.vvt.andon.events;

public class NotificationEvent {

    String message;

    public NotificationEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

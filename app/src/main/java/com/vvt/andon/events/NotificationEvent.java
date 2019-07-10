package com.vvt.andon.events;

public class NotificationEvent {

    String message;
    String department;

    public NotificationEvent(String message, String department) {
        this.message = message;
        this.department = department;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

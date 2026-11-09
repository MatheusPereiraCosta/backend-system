package com.agendapro.exception;

public class ScheduleConflictException extends BusinessRuleException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}

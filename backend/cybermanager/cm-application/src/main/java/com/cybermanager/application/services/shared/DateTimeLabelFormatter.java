package com.cybermanager.application.services.shared;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeLabelFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateTimeLabelFormatter() {
    }

    public static String format(LocalDateTime value) {
        return value.format(DATE_TIME_FORMATTER);
    }
}

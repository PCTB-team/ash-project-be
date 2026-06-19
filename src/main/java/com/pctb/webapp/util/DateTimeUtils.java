package com.pctb.webapp.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    private static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter API_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DateTimeUtils() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(STORAGE_ZONE);
    }

    public static String toDisplayDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.atOffset(STORAGE_ZONE)
                .atZoneSameInstant(DISPLAY_ZONE)
                .format(API_DATE_TIME_FORMATTER);
    }

    public static String formatTimeSince(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        Duration duration = Duration.between(dateTime, nowUtc());
        if (duration.isNegative() || duration.getSeconds() < 5) {
            return "v\u1eeba xong";
        }

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " gi\u00e2y tr\u01b0\u1edbc";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " ph\u00fat tr\u01b0\u1edbc";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " gi\u1edd tr\u01b0\u1edbc";
        }

        long days = duration.toDays();
        if (days < 30) {
            return days + " ng\u00e0y tr\u01b0\u1edbc";
        }

        if (days < 365) {
            return days / 30 + " th\u00e1ng tr\u01b0\u1edbc";
        }

        return days / 365 + " n\u0103m tr\u01b0\u1edbc";
    }
}

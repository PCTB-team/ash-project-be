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
            return "Vừa xong";
        }

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " giây trước";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = duration.toDays();
        if (days < 30) {
            return days + " ngày trước";
        }

        if (days < 365) {
            return days / 30 + " tháng trước";
        }

        return days / 365 + " năm trước";
    }
}

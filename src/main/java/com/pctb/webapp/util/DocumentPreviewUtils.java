package com.pctb.webapp.util;

import java.util.Locale;
import java.util.Set;

public final class DocumentPreviewUtils {
    private static final Set<String> OFFICE_EXTENSIONS = Set.of("docx", "ppt", "pptx", "xls", "xlsx");
    private static final Set<String> DIRECT_EXTENSIONS = Set.of(
            "pdf", "txt", "csv", "jpg", "jpeg", "png", "svg", "md", "mp3", "mp4"
    );

    private DocumentPreviewUtils() {
    }

    public static String resolvePreviewMode(String fileExtension) {
        String normalizedExtension = normalizeExtension(fileExtension);
        if (OFFICE_EXTENSIONS.contains(normalizedExtension)) {
            return "OFFICE";
        }

        if (DIRECT_EXTENSIONS.contains(normalizedExtension)) {
            return "DIRECT";
        }

        return "NONE";
    }

    public static boolean isPreviewSupported(String fileExtension) {
        return !"NONE".equals(resolvePreviewMode(fileExtension));
    }

    public static String resolvePreviewUrl(String fileExtension, String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) {
            return null;
        }

        String previewMode = resolvePreviewMode(fileExtension);
        if ("OFFICE".equals(previewMode)) {
            return "https://view.officeapps.live.com/op/embed.aspx?src=" + encodeUrl(storageUrl);
        }

        if ("DIRECT".equals(previewMode)) {
            return storageUrl;
        }

        return null;
    }

    private static String normalizeExtension(String fileExtension) {
        if (fileExtension == null) {
            return "";
        }

        return fileExtension.trim().toLowerCase(Locale.ROOT);
    }

    private static String encodeUrl(String url) {
        return java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8);
    }
}

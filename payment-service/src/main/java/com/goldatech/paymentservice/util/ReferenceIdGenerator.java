package com.goldatech.paymentservice.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class ReferenceIdGenerator {

    public ReferenceIdGenerator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate reference id: {CLIENT_INITIALS}-{8 digit random number}-{timestamp}
     * Example: "JK-01234567-240405123045678" (yyMMddHHmmssSSS)
     *
     * @param clientInitials client initials (will be uppercased and trimmed; non-letters removed; fallback "FPG")
     * @return formatted reference id
     */
    public static String generate(String clientInitials) {
        String initials = sanitizeInitials(clientInitials);
        String random8 = String.format("%08d", ThreadLocalRandom.current().nextInt(0, 100_000_000));
        String timestamp = ZonedDateTime.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));
        return initials + "-" + random8 + "-" + timestamp;
    }

    private static String sanitizeInitials(String input) {
        if (input == null) return "FPG";
        String cleaned = input.trim().toUpperCase().replaceAll("[^A-Z]", "");
        if (cleaned.isEmpty()) return "FPG";
        // limit to max 3 characters for brevity (adjust as needed)
        return cleaned.length() <= 3 ? cleaned : cleaned.substring(0, 3);
    }
}

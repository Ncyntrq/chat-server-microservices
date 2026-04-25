package com.chatsever.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt password hashing utility.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.1.
 *
 * BCryptPasswordEncoder cua Spring Security la thread-safe → singleton OK.
 */
public final class SecurityUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SecurityUtil() {}

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password khong duoc null");
        }
        return ENCODER.encode(rawPassword);
    }

    public static boolean checkPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}

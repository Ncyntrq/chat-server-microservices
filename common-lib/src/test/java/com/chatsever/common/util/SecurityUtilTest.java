package com.chatsever.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void hashedPasswordVerifiesAgainstOriginal() {
        String raw = "mypassword123";
        String hashed = SecurityUtil.hashPassword(raw);

        assertNotNull(hashed);
        assertNotEquals(raw, hashed, "Hash phai khac plaintext");
        assertTrue(hashed.startsWith("$2"), "BCrypt hash bat dau bang $2");
        assertTrue(SecurityUtil.checkPassword(raw, hashed));
    }

    @Test
    void wrongPasswordFailsVerification() {
        String hashed = SecurityUtil.hashPassword("correct");
        assertFalse(SecurityUtil.checkPassword("wrong", hashed));
    }

    @Test
    void sameInputProducesDifferentHashes() {
        String raw = "samepassword";
        String h1 = SecurityUtil.hashPassword(raw);
        String h2 = SecurityUtil.hashPassword(raw);
        assertNotEquals(h1, h2, "BCrypt dung salt ngau nhien");
    }
}

package com.chatsever.file.exception;

/**
 * Exception khi file vượt quá kích thước cho phép (10MB).
 */
public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(String message) {
        super(message);
    }
}

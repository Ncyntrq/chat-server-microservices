package com.chatsever.file.exception;

/**
 * Exception khi không tìm thấy file trong DB hoặc MinIO.
 */
public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super(message);
    }
}

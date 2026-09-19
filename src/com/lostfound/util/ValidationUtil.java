package com.lostfound.util;

import com.lostfound.exception.ValidationException;

import java.util.regex.Pattern;








public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; 
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    private ValidationUtil() {
        
    }

    public static void requireNonBlank(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }

    public static void requireValidEmail(String email) throws ValidationException {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Email format is invalid.");
        }
    }

    public static void requireMinLength(String value, int minLength, String fieldName) throws ValidationException {
        if (value == null || value.length() < minLength) {
            throw new ValidationException(fieldName + " must be at least " + minLength + " characters.");
        }
    }

    
    public static void requireValidImageExtension(String fileName) throws ValidationException {
        requireNonBlank(fileName, "Image file name");
        String lower = fileName.toLowerCase();
        for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return;
            }
        }
        throw new ValidationException("Image must be one of: .jpg, .jpeg, .png, .gif");
    }

    
    public static void requireValidImageSize(long sizeBytes) throws ValidationException {
        if (sizeBytes <= 0) {
            throw new ValidationException("Image file appears to be empty.");
        }
        if (sizeBytes > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("Image must be smaller than 5 MB.");
        }
    }
}

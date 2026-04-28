package com.xc.study.module.media.storage;

public class MediaObjectNotFoundException extends RuntimeException {

    public MediaObjectNotFoundException(String message) {
        super(message);
    }

    public MediaObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.brayanpv.app.domain.exception;

public class SpecieNotFoundException extends RuntimeException {
    String message;
    public SpecieNotFoundException(String message) {super(message);}
}

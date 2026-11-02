package com.simo.bankapi.web;

/** A query parameter whose value is out of range; reported as a {@code 400} naming it. */
class InvalidParameterException extends RuntimeException {

    private final String parameter;

    InvalidParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    String parameter() {
        return parameter;
    }
}

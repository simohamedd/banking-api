package com.simo.bankapi.account;

public class AccountNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String number) {
        super("No account with number " + number);
    }
}

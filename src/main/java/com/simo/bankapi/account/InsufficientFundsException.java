package com.simo.bankapi.account;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String number, BigDecimal available) {
        super("Account " + number + " only has £" + available.toPlainString() + " available");
    }
}

package com.simo.bankapi.account;

import java.math.BigDecimal;

/** Thrown when a new overdraft limit would be smaller than what the account already owes. */
public class OverdraftBelowDebtException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OverdraftBelowDebtException(String number, BigDecimal owed) {
        super("Account " + number + " is overdrawn by £" + owed.toPlainString()
                + ", so its overdraft limit can't be set lower than that");
    }
}

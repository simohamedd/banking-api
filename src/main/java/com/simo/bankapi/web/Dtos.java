package com.simo.bankapi.web;

import com.simo.bankapi.account.Account;
import com.simo.bankapi.account.AccountType;
import com.simo.bankapi.account.EntryType;
import com.simo.bankapi.account.LedgerEntry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class Dtos {
    private Dtos() {
    }

    /** Longest owner name accepted. */
    public static final int MAX_OWNER = 100;

    /**
     * Longest reference accepted. Transfers append " to 12345678" or " from 12345678",
     * so this leaves room for that suffix inside the 255-character ledger column.
     */
    public static final int MAX_REFERENCE = 140;

    public record OpenAccountRequest(
            @NotBlank @Size(max = MAX_OWNER) String owner,
            @NotNull AccountType type,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal overdraftLimit) {
    }

    public record MoneyRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = MAX_REFERENCE) String reference) {
    }

    public record TransferRequest(
            @NotNull @Pattern(regexp = "\\d{8}") String from,
            @NotNull @Pattern(regexp = "\\d{8}") String to,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = MAX_REFERENCE) String reference) {
    }

    public record AccountResponse(String number, String owner, AccountType type, BigDecimal balance,
                                  BigDecimal overdraftLimit, BigDecimal available, Instant openedAt) {
        static AccountResponse from(Account a) {
            return new AccountResponse(a.getNumber(), a.getOwner(), a.getType(), a.getBalance(),
                    a.getOverdraftLimit(), a.available(), a.getOpenedAt());
        }
    }

    public record EntryResponse(Long id, EntryType type, BigDecimal amount, BigDecimal balanceAfter,
                                String reference, Instant createdAt) {
        static EntryResponse from(LedgerEntry e) {
            return new EntryResponse(e.getId(), e.getType(), e.getAmount(), e.getBalanceAfter(),
                    e.getReference(), e.getCreatedAt());
        }
    }
}

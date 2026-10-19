package com.simo.bankapi.account;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @Transactional
    public Account open(String owner, AccountType type, BigDecimal overdraftLimit) {
        BigDecimal overdraft = overdraftLimit == null ? BigDecimal.ZERO : overdraftLimit;
        if (type == AccountType.SAVINGS && overdraft.signum() != 0) {
            throw new IllegalArgumentException("Savings accounts can't have an overdraft");
        }
        String number;
        do {
            number = String.valueOf(10_000_000 + random.nextInt(90_000_000));
        } while (accounts.existsByNumber(number));
        return accounts.save(new Account(number, owner.trim(), type, overdraft));
    }

    @Transactional(readOnly = true)
    public List<Account> list(String owner) {
        return owner == null || owner.isBlank()
                ? accounts.findAllByOrderByOpenedAtAsc()
                : accounts.findByOwnerIgnoreCaseOrderByOpenedAtAsc(owner.trim());
    }

    @Transactional(readOnly = true)
    public Account get(String number) {
        return accounts.findByNumber(number).orElseThrow(() -> new AccountNotFoundException(number));
    }

    @Transactional
    public Account deposit(String number, BigDecimal amount, String reference) {
        Account a = lock(number);
        a.credit(amount);
        ledger.save(new LedgerEntry(a, EntryType.DEPOSIT, amount, reference));
        return a;
    }

    @Transactional
    public Account withdraw(String number, BigDecimal amount, String reference) {
        Account a = lock(number);
        a.debit(amount);
        ledger.save(new LedgerEntry(a, EntryType.WITHDRAWAL, amount, reference));
        return a;
    }

    /**
     * Moves money in one database transaction. Accounts are always locked in the same order
     * (by number) so two opposite transfers can't deadlock.
     */
    @Transactional
    public void transfer(String from, String to, BigDecimal amount, String reference) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("Choose two different accounts");
        }
        boolean fromFirst = from.compareTo(to) < 0;
        Account first = lock(fromFirst ? from : to);
        Account second = lock(fromFirst ? to : from);
        Account source = fromFirst ? first : second;
        Account target = fromFirst ? second : first;

        source.debit(amount);
        target.credit(amount);
        String ref = reference == null || reference.isBlank() ? "Transfer" : reference.trim();
        ledger.save(new LedgerEntry(source, EntryType.TRANSFER_OUT, amount, ref + " to " + to));
        ledger.save(new LedgerEntry(target, EntryType.TRANSFER_IN, amount, ref + " from " + from));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> statement(String number) {
        get(number);
        return ledger.findByAccountNumberOrderByIdDesc(number);
    }

    private Account lock(String number) {
        return accounts.findForUpdate(number).orElseThrow(() -> new AccountNotFoundException(number));
    }
}

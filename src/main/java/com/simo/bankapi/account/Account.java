package com.simo.bankapi.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String number;

    @Column(nullable = false)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal overdraftLimit;

    @Column(nullable = false)
    private Instant openedAt;

    @Version
    private long version;

    protected Account() {
    }

    public Account(String number, String owner, AccountType type, BigDecimal overdraftLimit) {
        this.number = number;
        this.owner = owner;
        this.type = type;
        this.overdraftLimit = overdraftLimit.setScale(2);
        this.openedAt = Instant.now();
    }

    public BigDecimal available() {
        return balance.add(overdraftLimit);
    }

    void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    void debit(BigDecimal amount) {
        if (amount.compareTo(available()) > 0) {
            throw new InsufficientFundsException(number, available());
        }
        balance = balance.subtract(amount);
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getOwner() { return owner; }
    public AccountType getType() { return type; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public Instant getOpenedAt() { return openedAt; }
}

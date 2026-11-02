package com.simo.bankapi.account;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccountNumberOrderByIdDesc(String number, Pageable page);

    List<LedgerEntry> findByAccountNumberAndIdLessThanOrderByIdDesc(String number, Long before, Pageable page);
}

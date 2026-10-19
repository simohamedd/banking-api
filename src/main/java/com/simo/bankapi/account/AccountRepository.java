package com.simo.bankapi.account;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByNumber(String number);

    boolean existsByNumber(String number);

    List<Account> findByOwnerIgnoreCaseOrderByOpenedAtAsc(String owner);

    List<Account> findAllByOrderByOpenedAtAsc();

    /** Locks the row so two transfers can't spend the same money at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.number = :number")
    Optional<Account> findForUpdate(@Param("number") String number);
}

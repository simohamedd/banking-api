package com.simo.bankapi.web;

import com.simo.bankapi.account.AccountService;
import com.simo.bankapi.web.Dtos.AccountResponse;
import com.simo.bankapi.web.Dtos.EntryResponse;
import com.simo.bankapi.web.Dtos.MoneyRequest;
import com.simo.bankapi.web.Dtos.OpenAccountRequest;
import com.simo.bankapi.web.Dtos.TransferRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest req) {
        var a = service.open(req.owner(), req.type(), req.overdraftLimit());
        return ResponseEntity.created(URI.create("/api/accounts/" + a.getNumber())).body(AccountResponse.from(a));
    }

    @GetMapping("/accounts")
    public List<AccountResponse> list(@RequestParam(required = false) String owner) {
        return service.list(owner).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/accounts/{number}")
    public AccountResponse get(@PathVariable String number) {
        return AccountResponse.from(service.get(number));
    }

    @PostMapping("/accounts/{number}/deposits")
    public AccountResponse deposit(@PathVariable String number, @Valid @RequestBody MoneyRequest req) {
        return AccountResponse.from(service.deposit(number, req.amount(), req.reference()));
    }

    @PostMapping("/accounts/{number}/withdrawals")
    public AccountResponse withdraw(@PathVariable String number, @Valid @RequestBody MoneyRequest req) {
        return AccountResponse.from(service.withdraw(number, req.amount(), req.reference()));
    }

    @PostMapping("/transfers")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
        service.transfer(req.from(), req.to(), req.amount(), req.reference());
        return ResponseEntity.noContent().build();
    }

    /** Entries returned when the client doesn't pass {@code limit}. */
    static final int DEFAULT_STATEMENT_LIMIT = 50;

    /** Most entries one statement request can return. */
    static final int MAX_STATEMENT_LIMIT = 200;

    @GetMapping("/accounts/{number}/transactions")
    public List<EntryResponse> statement(@PathVariable String number,
                                         @RequestParam(required = false) Long before,
                                         @RequestParam(required = false) Integer limit) {
        int size = limit == null ? DEFAULT_STATEMENT_LIMIT : limit;
        if (size < 1 || size > MAX_STATEMENT_LIMIT) {
            throw new InvalidParameterException("limit", "must be between 1 and " + MAX_STATEMENT_LIMIT);
        }
        if (before != null && before < 1) {
            throw new InvalidParameterException("before", "must be a positive entry id");
        }
        return service.statement(number, before, size).stream().map(EntryResponse::from).toList();
    }
}

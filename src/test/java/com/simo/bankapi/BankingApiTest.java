package com.simo.bankapi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.simo.bankapi.account.AccountRepository;
import com.simo.bankapi.account.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    private String open(String owner, String type, String overdraft) throws Exception {
        String body = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"%s\",\"type\":\"%s\",\"overdraftLimit\":%s}".formatted(owner, type, overdraft)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.number");
    }

    private void deposit(String number, String amount) throws Exception {
        mvc.perform(post("/api/accounts/{n}/deposits", number).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s,\"reference\":\"Salary\"}".formatted(amount)))
                .andExpect(status().isOk());
    }

    @Test
    void opensAccountAndTakesDeposits() throws Exception {
        String n = open("Amina", "CURRENT", "100");
        deposit(n, "250.00");
        mvc.perform(get("/api/accounts/{n}", n))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("Amina"))
                .andExpect(jsonPath("$.balance").value(250.0))
                .andExpect(jsonPath("$.available").value(350.0));
    }

    @Test
    void withdrawalCanUseOverdraftButNotBeyondIt() throws Exception {
        String n = open("Yusuf", "CURRENT", "50");
        deposit(n, "20");
        mvc.perform(post("/api/accounts/{n}/withdrawals", n).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(-40.0));
        mvc.perform(post("/api/accounts/{n}/withdrawals", n).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transferMovesMoneyAndRecordsBothSides() throws Exception {
        String a = open("Amina", "CURRENT", "0");
        String b = open("Amina", "SAVINGS", "0");
        deposit(a, "500");

        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"%s\",\"to\":\"%s\",\"amount\":120.50,\"reference\":\"Rainy day\"}".formatted(a, b)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/accounts/{n}", a)).andExpect(jsonPath("$.balance").value(379.5));
        mvc.perform(get("/api/accounts/{n}", b)).andExpect(jsonPath("$.balance").value(120.5));
        mvc.perform(get("/api/accounts/{n}/transactions", a))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[0].balanceAfter").value(379.5));
    }

    @Test
    void failedTransferChangesNothing() throws Exception {
        String a = open("Amina", "CURRENT", "0");
        String b = open("Sara", "CURRENT", "0");
        deposit(a, "10");
        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"%s\",\"to\":\"%s\",\"amount\":50}".formatted(a, b)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/accounts/{n}", a)).andExpect(jsonPath("$.balance").value(10.0));
        mvc.perform(get("/api/accounts/{n}", b)).andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void validatesRequests() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"\",\"type\":\"CURRENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.owner").exists());
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"Sara\",\"type\":\"SAVINGS\",\"overdraftLimit\":100}"))
                .andExpect(status().isBadRequest());
        String n = open("Sara", "CURRENT", "0");
        mvc.perform(post("/api/accounts/{n}/deposits", n).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mvc.perform(get("/api/accounts/99999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/99999999/transactions")).andExpect(status().isNotFound());
    }

    @Test
    void listsAccountsOptionallyForOneOwner() throws Exception {
        open("Amina", "CURRENT", "0");
        open("Amina", "SAVINGS", "0");
        open("Sara", "CURRENT", "0");
        mvc.perform(get("/api/accounts")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/accounts").param("owner", "amina"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("CURRENT"))
                .andExpect(jsonPath("$[1].type").value("SAVINGS"));
        mvc.perform(get("/api/accounts").param("owner", "Nobody")).andExpect(jsonPath("$", hasSize(0)));
    }
}

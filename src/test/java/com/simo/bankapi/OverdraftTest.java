package com.simo.bankapi;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class OverdraftTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    private String open(String type, String overdraft) throws Exception {
        String body = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"Amina\",\"type\":\"%s\",\"overdraftLimit\":%s}".formatted(type, overdraft)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.number");
    }

    private void withdraw(String number, String amount) throws Exception {
        mvc.perform(post("/api/accounts/{n}/withdrawals", number).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private ResultActions change(String number, String body) throws Exception {
        return mvc.perform(put("/api/accounts/{n}/overdraft", number)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void raisingTheLimitMakesMoreMoneyAvailable() throws Exception {
        String n = open("CURRENT", "50");
        change(n, "{\"overdraftLimit\":250}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdraftLimit").value(250.0))
                .andExpect(jsonPath("$.available").value(250.0));
        withdraw(n, "200");
    }

    @Test
    void limitCanBeLoweredToExactlyTheCurrentDebtButNotBelow() throws Exception {
        String n = open("CURRENT", "100");
        withdraw(n, "80");

        change(n, "{\"overdraftLimit\":50}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("overdrawn by")))
                .andExpect(jsonPath("$.detail").value(containsString("80.00")));

        change(n, "{\"overdraftLimit\":80}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(-80.0))
                .andExpect(jsonPath("$.available").value(0.0));
    }

    @Test
    void overdraftCanBeRemovedWhenNotOverdrawn() throws Exception {
        String n = open("CURRENT", "100");
        change(n, "{\"overdraftLimit\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdraftLimit").value(0.0));
        mvc.perform(post("/api/accounts/{n}/withdrawals", n).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void savingsAccountsCantGetAnOverdraft() throws Exception {
        String n = open("SAVINGS", "0");
        change(n, "{\"overdraftLimit\":100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Savings accounts can't have an overdraft"));
        change(n, "{\"overdraftLimit\":0}").andExpect(status().isOk());
    }

    @Test
    void invalidLimitsAreRejectedWithTheFieldNamed() throws Exception {
        String n = open("CURRENT", "0");
        change(n, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.overdraftLimit").exists());
        change(n, "{\"overdraftLimit\":-1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.overdraftLimit").exists());
        change(n, "{\"overdraftLimit\":10.555}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.overdraftLimit").exists());
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        change("99999999", "{\"overdraftLimit\":10}").andExpect(status().isNotFound());
    }
}

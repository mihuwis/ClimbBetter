package pl.climbbetter.identity.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"climbbetter.auth.mode=firebase", "climbbetter.auth.project-id=climbbetter-auth-test"})
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Testcontainers
class FirebaseAuthenticationIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired IdentityAccountService accounts;
    @Autowired ObjectMapper json;
    @MockitoBean FirebaseTokenVerifier verifier;
    VerifiedFirebaseIdentity a;
    VerifiedFirebaseIdentity b;

    @BeforeEach
    void tokens() {
        a = new VerifiedFirebaseIdentity("https://securetoken.google.com/climbbetter-auth-test", UUID.randomUUID().toString(), "Mihu");
        b = new VerifiedFirebaseIdentity(a.issuer(), UUID.randomUUID().toString(), "Test_dobry_wspinacz_1");
        when(verifier.verify(anyString())).thenAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
            case "a" -> a;
            case "b" -> b;
            case "unavailable" -> throw new IdentityAccessException(503, "AUTH_UNAVAILABLE", "Please retry.");
            default -> throw new IdentityAccessException(401, "INVALID_TOKEN", "Please sign in again.");
        });
    }

    @Test
    void rejectsMissingMalformedAndInvalidTokensBeforeDataAccess() throws Exception {
        mvc.perform(get("/api/v1/dashboard/sessions")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Basic a")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Bearer unavailable"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AUTH_UNAVAILABLE"));
    }

    @Test
    void provisionsOnceAndRequiresProfileBeforeTraining() throws Exception {
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Bearer a"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACCOUNT_NOT_PROVISIONED"));
        assertTrue(accounts.find(a).isEmpty(), "GET must not create an account");
        String first = mvc.perform(post("/api/v1/identity/bootstrap").header("Authorization", "Bearer a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileComplete").value(false))
                .andReturn().getResponse().getContentAsString();
        String userId = json.readTree(first).get("userId").asText();
        mvc.perform(post("/api/v1/identity/bootstrap").header("Authorization", "Bearer a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId));
        mvc.perform(get("/api/v1/dashboard/sessions").header("Authorization", "Bearer a"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PROFILE_INCOMPLETE"));
        mvc.perform(put("/api/v1/identity/me").header("Authorization", "Bearer a")
                        .contentType("application/json").content("{\"displayName\":\"  Mihu  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Mihu"))
                .andExpect(jsonPath("$.profileComplete").value(true));
        mvc.perform(get("/api/v1/dashboard/sessions").header("Authorization", "Bearer a")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/dashboard/sessions")).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesProfileAndDoesNotOverwriteChosenNameOnLaterLogin() throws Exception {
        AccountProfile profile = accounts.bootstrap(a);
        for (String name : List.of("", "   ", "x".repeat(81))) {
            mvc.perform(put("/api/v1/identity/me").header("Authorization", "Bearer a")
                    .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("displayName", name))))
                    .andExpect(status().isBadRequest());
        }
        accounts.update(profile.userId(), "My own name");
        assertEquals("My own name", accounts.bootstrap(a).displayName());
    }

    @Test
    void concurrentBootstrapDoesNotCreateDuplicateUsers() throws Exception {
        long before = jdbc.sql("SELECT count(*) FROM identity.users").query(Long.class).single();
        try (var executor = Executors.newFixedThreadPool(6)) {
            List<Callable<UUID>> tasks = new ArrayList<>();
            for (int index = 0; index < 12; index++) tasks.add(() -> accounts.bootstrap(a).userId());
            var results = executor.invokeAll(tasks);
            UUID expected = results.getFirst().get();
            for (var result : results) assertEquals(expected, result.get());
        }
        assertEquals(before + 1, jdbc.sql("SELECT count(*) FROM identity.users").query(Long.class).single());
    }

    @Test
    void linkedExistingUserKeepsUuidAndDoesNotReceiveAnotherAccount() throws Exception {
        UUID original = UUID.fromString("00000000-0000-0000-0000-000000000001");
        jdbc.sql("INSERT INTO identity.user_identities(id,user_id,issuer,subject,created_at) VALUES (:id,:user,:issuer,:subject,now())")
                .param("id", UUID.randomUUID()).param("user", original).param("issuer", a.issuer()).param("subject", a.subject()).update();
        try {
            mvc.perform(post("/api/v1/identity/bootstrap").header("Authorization", "Bearer a"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(original.toString()));
        } finally {
            jdbc.sql("DELETE FROM identity.user_identities WHERE user_id=:id").param("id", original).update();
        }
    }

    @Test
    void twoAccountsCannotReadChangeOrDeleteEachOthersTrainingAndCatalog() throws Exception {
        AccountProfile first = accounts.bootstrap(a), second = accounts.bootstrap(b);
        accounts.update(first.userId(), "Mihu"); accounts.update(second.userId(), "Test_dobry_wspinacz_1");
        String body = """
                {"clientSessionId":"%s","sessionName":"Private auth test","sessionDate":"2026-11-08",
                 "timeZoneId":"Europe/Warsaw","startedAt":"2026-11-08T10:00:00Z","endedAt":"2026-11-08T11:00:00Z",
                 "clientAreaId":"%s","areaName":"Private auth gym %s",
                 "entries":[{"clientEntryId":"%s","entryOrder":0,"resultType":"WARMUP","executedMoves":20}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        String response = mvc.perform(post("/api/v1/training/sessions").header("Authorization", "Bearer a")
                        .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = json.readTree(response).get("sessionId").asText();
        var details = json.readTree(mvc.perform(get("/api/v1/training/sessions/" + sessionId)
                .header("Authorization", "Bearer a")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String areaId = details.get("areaId").asText();
        mvc.perform(get("/api/v1/training/sessions/" + sessionId).header("Authorization", "Bearer b")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/training/sessions/" + sessionId).param("expectedVersion", "0")
                .header("Authorization", "Bearer b")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/training/sessions/" + sessionId).header("Authorization", "Bearer b")
                .contentType("application/json").content("{\"expectedVersion\":0,\"changeReason\":\"Test\",\"session\":" + body + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/catalog/areas/" + areaId + "/climbs").header("Authorization", "Bearer b"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dashboard/sessions").header("Authorization", "Bearer b"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Bearer a"))
                .andExpect(jsonPath("$.userId").value(first.userId().toString()));
        mvc.perform(get("/api/v1/identity/me").header("Authorization", "Bearer b"))
                .andExpect(jsonPath("$.userId").value(second.userId().toString()));
        mvc.perform(get("/api/v1/training/sessions/" + sessionId).header("Authorization", "Bearer a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void allowsPreflightWithAuthorizationFromLocalViteAndRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/v1/identity/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/v1/identity/me").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}

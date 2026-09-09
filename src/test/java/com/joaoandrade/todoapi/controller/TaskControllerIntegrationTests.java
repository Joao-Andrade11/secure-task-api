package com.joaoandrade.todoapi.controller;

import com.joaoandrade.todoapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerIntegrationTests {

    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void clearDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void rejectsUnauthenticatedRequestsWithJsonError() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "WWW-Authenticate",
                        "Basic realm=\"Secure Task API\""
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Autenticacao obrigatoria"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void allowsAuthenticatedReadButDeniesWriteWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void performsCompleteCrudAsAdmin() throws Exception {
        String location = mockMvc.perform(post("/tasks")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location).with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Revisar controles"));

        mockMvc.perform(put(location)
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Revisar controles atualizados",
                                  "description": "Validar o hardening",
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(delete(location)
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location).with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginatesAndCapsRequestedPageSize() throws Exception {
        mockMvc.perform(get("/tasks?size=999")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void rejectsInvalidEnumMalformedJsonAndOversizedDescription() throws Exception {
        mockMvc.perform(get("/tasks?status=INVALID")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/tasks")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest());

        String oversizedDescription = "x".repeat(1001);
        mockMvc.perform(post("/tasks")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Teste","description":"%s","status":"PENDING"}
                                """.formatted(oversizedDescription)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void rejectsUnknownJsonFieldsAndIncompletePut() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Teste","status":"PENDING","isAdmin":true}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/tasks/1")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Teste","description":"Sem status"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    void rejectsNonPositiveIds() throws Exception {
        mockMvc.perform(get("/tasks/0").with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresCsrfForAuthenticatedWritesAndExposesToken() throws Exception {
        mockMvc.perform(get("/csrf").with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/tasks")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden());
    }

    private String validCreateRequest() {
        return """
                {
                  "title": "Revisar controles",
                  "description": "Executar analise de seguranca",
                  "status": "PENDING"
                }
                """;
    }
}

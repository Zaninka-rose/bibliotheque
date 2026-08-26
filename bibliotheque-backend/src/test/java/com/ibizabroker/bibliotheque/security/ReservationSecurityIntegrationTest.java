package com.ibizabroker.bibliotheque.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration du Security Filter Chain complet pour les réservations.
 *
 * Le contexte Spring est chargé avec tous les filtres de sécurité (JwtRequestFilter, etc.).
 * Le DataInitializer crée l'admin (admin/admin123, rôle Admin) dans la base H2 de test.
 *
 * Flux testé :
 *  1) Login → obtenir un JWT valide
 *  2) GET /api/reservations sans token → 401
 *  3) GET /api/reservations avec token valide → 200
 *  4) POST /api/reservations avec token valide → auth OK (pas 401/403)
 *  5) POST /api/reservations sans token → 401
 *  6) Header sans Bearer → 401
 *  7) Swagger accessible sans auth (permitAll)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String cachedToken;

    /**
     * Effectue le login admin et retourne le JWT.
     */
    private String loginAsAdmin() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("jwtToken").asText();
    }

    @BeforeAll
    void obtainToken() throws Exception {
        cachedToken = loginAsAdmin();
        Assertions.assertNotNull(cachedToken);
    }

    // ================================================================
    //  1) GET /api/reservations SANS token → 401
    // ================================================================
    @Test
    @Order(1)
    @DisplayName("GET /api/reservations sans token → 401 Unauthorized")
    void reservations_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  2) GET /api/reservations AVEC token admin → 200
    // ================================================================
    @Test
    @Order(2)
    @DisplayName("GET /api/reservations avec token admin → 200 OK")
    void reservations_avecTokenAdmin_200() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + cachedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ================================================================
    //  3) POST /api/reservations AVEC token admin → auth OK
    // ================================================================
    @Test
    @Order(3)
    @DisplayName("POST /api/reservations avec token admin → auth OK (pas 401/403)")
    void creerReservation_avecTokenAdmin_authOk() throws Exception {
        String dtoJson = objectMapper.writeValueAsString(new ReservationCreateDTO(1L, 10L));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + cachedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertTrue(status != 401 && status != 403,
                            "L'authentification doit passer (status=" + status + ")");
                });
    }

    // ================================================================
    //  4) POST /api/reservations SANS token → 401
    // ================================================================
    @Test
    @Order(4)
    @DisplayName("POST /api/reservations sans token → 401 Unauthorized")
    void creerReservation_sansToken_401() throws Exception {
        String dtoJson = objectMapper.writeValueAsString(new ReservationCreateDTO(1L, 10L));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  5) Header sans préfixe Bearer → 401
    // ================================================================
    @Test
    @Order(5)
    @DisplayName("GET /api/reservations avec header sans 'Bearer ' → 401")
    void reservations_sansPrefixBearer_401() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", cachedToken))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  6) Swagger accessible sans auth
    // ================================================================
    @Test
    @Order(6)
    @DisplayName("Swagger UI accessible sans authentification")
    void swagger_sansAuth_access() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    Assertions.assertTrue(status == 200 || status == 302,
                            "Swagger UI doit être accessible sans auth (status=" + status + ")");
                });
    }

    // ================================================================
    //  7) Login admin → 200 (dans le contexte sécurisé)
    // ================================================================
    @Test
    @Order(7)
    @DisplayName("POST /authenticate avec bonnes credentials → 200")
    void loginAdmin_dansContexteSecurise_200() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    // ================================================================
    //  8) Login admin mauvais mdp → 401 (ExceptionTranslationFilter gère l'erreur)
    // ================================================================
    @Test
    @Order(8)
    @DisplayName("POST /authenticate avec mauvais mot de passe → 401")
    void loginAdmin_mauvaisMdp_401() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  9) Login utilisateur inexistant → 401
    // ================================================================
    @Test
    @Order(9)
    @DisplayName("POST /authenticate avec utilisateur inexistant → 401")
    void login_inexistant_401() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("inexistant");
        request.setPassword("whatever");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration BOUT-EN-BOUT du cycle de vie d'une réservation,
 * via le vrai contrôleur, le vrai service et la vraie base H2 en mémoire
 * (profil "test") — tout passe avec la seule commande de test du projet
 * (./mvnw test), sans base externe ni manipulation manuelle.
 *
 * Scénario (dans l'ordre) :
 *   1. Authentification d'un adhérent et d'un bibliothécaire (JWT réels)
 *   2. POST   /api/reservations            → 201 (l'adhéent réserve pour lui-même)
 *   3. GET    /api/reservations            → 200, la réservation créée est dans sa liste
 *   4. GET    /api/reservations/{id}       → 200, statut EN_ATTENTE
 *   5. PATCH  /api/reservations/{id}/annuler → 200, statut ANNULEE
 *   6. PATCH  /api/reservations/{id}/annuler → 409 (déjà annulée — RG-05)
 *   7. DELETE /api/reservations/{id}       → 204 (bibliothécaire)
 *   8. GET    /api/reservations/{id}       → 404 (supprimée)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adherentToken;
    private String adminToken;
    private Long adherentId;
    private Long livreIndisponibleId;

    private Long reservationId;

    @BeforeAll
    void setUpData() throws Exception {
        // --- adhérent dédié à ce scénario ---
        Role roleAdherent = new Role();
        roleAdherent.setRoleName("Adherent");

        Users adherent = new Users();
        adherent.setUsername("lifecycle_a1");
        adherent.setName("Adherent Lifecycle");
        adherent.setPassword(passwordEncoder.encode("admin123"));
        adherent.setRole(java.util.Set.of(roleAdherent));
        adherent = usersRepository.save(adherent);
        adherentId = adherent.getUserId().longValue();

        // --- livre indisponible : prérequis RG-01 pour réserver ---
        Books livre = new Books();
        livre.setBookName("Lifecycle L1 - Indisponible");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Test");
        livre.setNoOfCopies(0);
        livre = booksRepository.save(livre);
        livreIndisponibleId = livre.getBookId().longValue();

        // --- tokens JWT réels via /authenticate ---
        adherentToken = login("lifecycle_a1", "admin123");
        adminToken = login("admin", "admin123");
    }

    private String login(String username, String password) throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("jwtToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/reservations (ADHERENT, pour lui-même) → 201 EN_ATTENTE")
    void etape1_creationParAdherent() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreIndisponibleId, null));

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adherentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.livreId").value(livreIndisponibleId))
                .andExpect(jsonPath("$.adherentId").value(adherentId))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.dateReservation").exists())
                .andExpect(jsonPath("$.dateExpiration").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        reservationId = json.get("id").asLong();
        assertNotNull(reservationId);
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/reservations (ADHERENT) → la réservation créée est dans sa liste (RS-05)")
    void etape2_listeContientLaReservation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adherentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean trouvee = false;
        for (JsonNode r : list) {
            assertEquals(adherentId, r.get("adherentId").asLong(),
                    "RS-05 : la liste d'un adhérent ne contient que ses réservations");
            if (r.get("id").asLong() == reservationId) {
                trouvee = true;
            }
        }
        assertTrue(trouvee, "La réservation créée doit apparaître dans la liste de l'adhérent");
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/reservations/{id} (son propriétaire) → 200 EN_ATTENTE")
    void etape3_consultationParProprietaire() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adherentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.adherentId").value(adherentId));
    }

    @Test
    @Order(4)
    @DisplayName("PATCH /api/reservations/{id}/annuler (propriétaire) → 200 ANNULEE")
    void etape4_annulationParProprietaire() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/annuler")
                        .header("Authorization", "Bearer " + adherentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.statut").value("ANNULEE"));

        // Le statut est bien persisté en base
        assertEquals(StatutReservation.ANNULEE,
                reservationRepository.findById(reservationId).orElseThrow().getStatut());
    }

    @Test
    @Order(5)
    @DisplayName("PATCH /api/reservations/{id}/annuler (2e fois) → 409 RG-05 (déjà annulée)")
    void etape5_reAnnulation_409() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/annuler")
                        .header("Authorization", "Bearer " + adherentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("RG-05")));
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/reservations/{id} (BIBLIOTHECAIRE) → 204")
    void etape6_suppressionParAdmin() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(reservationRepository.existsById(reservationId),
                "La réservation doit être physiquement supprimée");
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/reservations/{id} après suppression → 404")
    void etape7_consultationApresSuppression_404() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }
}

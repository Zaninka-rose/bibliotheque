package com.ibizabroker.bibliotheque.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la matrice de permissions du module réservation.
 *
 * Rôles DB : Admin = BIBLIOTHECAIRE, Adherent = ADHERENT.
 * Contexte Spring complet avec Security Filter Chain (JwtRequestFilter actif).
 * Base H2 de test, remplie dans @BeforeAll :
 *   - admin  / admin123  (rôle Admin)      → créé par DataInitializer
 *   - a1     / admin123  (rôle Adherent)   → créé ici
 *   - a2     / admin123  (rôle Adherent)   → créé ici, possède une réservation
 *
 * Règles couvertes :
 *   RS-01 : sans token → 401 sur tous les endpoints
 *   RS-02 : ADHERENT sur action bibliothécaire → 403
 *   RS-03 : ADHERENT sur réservation d'autrui → 403
 *   RS-04 : adherentId du corps ignoré, identité = token
 *   RS-05 : GET liste d'un ADHERENT → ses réservations seulement
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationSecurityIntegrationTest {

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

    private String adminToken;
    private String a1Token;
    private Users a1;
    private Users a2;
    private Reservation reservationDeA2;

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

    @BeforeAll
    void setUpData() throws Exception {
        adminToken = login("admin", "admin123");

        // --- adhérents de test ---
        Role roleAdherent = new Role();
        roleAdherent.setRoleName("Adherent");

        a1 = new Users();
        a1.setUsername("a1");
        a1.setName("Adherent A1");
        a1.setPassword(passwordEncoder.encode("admin123"));
        a1.setRole(java.util.Set.of(roleAdherent));
        a1 = usersRepository.save(a1);

        Role roleAdherent2 = new Role();
        roleAdherent2.setRoleName("Adherent");

        a2 = new Users();
        a2.setUsername("a2");
        a2.setName("Adherent A2");
        a2.setPassword(passwordEncoder.encode("admin123"));
        a2.setRole(java.util.Set.of(roleAdherent2));
        a2 = usersRepository.save(a2);

        a1Token = login("a1", "admin123");

        // --- livre indisponible + réservation appartenant à a2 ---
        Books livre = new Books();
        livre.setBookName("L2 - Indisponible");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Test");
        livre.setNoOfCopies(0);
        livre = booksRepository.save(livre);

        reservationDeA2 = reservationRepository.save(
                new Reservation(livre, a2, StatutReservation.EN_ATTENTE));
    }

    // ================================================================
    //  RS-01 : sans token → 401 sur TOUS les endpoints
    // ================================================================
    @Test
    @Order(1)
    @DisplayName("RS-01 : tous les endpoints sans token → 401 (jamais 403)")
    void rs01_sansToken_401() throws Exception {
        String body = objectMapper.writeValueAsString(new ReservationCreateDTO(1L, null));

        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations/" + reservationDeA2.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/reservations/" + reservationDeA2.getId() + "/annuler"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/reservations/" + reservationDeA2.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  RS-01 bis : token invalide → 401
    // ================================================================
    @Test
    @Order(2)
    @DisplayName("RS-01 : token invalide → 401")
    void rs01_tokenInvalide_401() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer token.bidon.invalide"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  RS-02 : ADHERENT sur action réservée au bibliothécaire → 403
    // ================================================================
    @Test
    @Order(3)
    @DisplayName("RS-02 : ADHERENT sur DELETE /api/reservations/{id} → 403")
    void rs02_adherentDelete_403() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + reservationDeA2.getId())
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    //  RS-03 : ADHERENT sur la réservation d'un autre → 403
    // ================================================================
    @Test
    @Order(4)
    @DisplayName("RS-03 : ADHERENT consulte la réservation d'un autre → 403")
    void rs03_adherentConsulteAutre_403() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationDeA2.getId())
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("RS-03 : ADHERENT annule la réservation d'un autre → 403")
    void rs03_adherentAnnuleAutre_403() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationDeA2.getId() + "/annuler")
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("RS-03 : ADHERENT consulte sa propre réservation → 200")
    void rs03_adherentConsulteSienne_200() throws Exception {
        // a1 crée d'abord sa propre réservation (via admin pour maîtriser les données)
        Books livre = booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow();

        Reservation sienne = reservationRepository.save(
                new Reservation(livre, a1, StatutReservation.EN_ATTENTE));

        mockMvc.perform(get("/api/reservations/" + sienne.getId())
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherentId").value(a1.getUserId()));

        reservationRepository.delete(sienne);
    }

    // ================================================================
    //  RS-04 : l'identité vient du token, jamais du corps
    // ================================================================
    @Test
    @Order(7)
    @DisplayName("RS-04 : ADHERENT qui forge adherentId d'autrui → 201 mais réservation créée pour LUI")
    void rs04_adherentForgeAdherentId() throws Exception {
        // a1 fournit l'id de a2 dans le corps : doit être ignoré
        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreIndisponibleId(), a2.getUserId().longValue()));

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + a1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(a1.getUserId(), json.get("adherentId").asInt(),
                "RS-04 : la réservation doit appartenir à l'utilisateur du token (a1), pas à a2");

        // Nettoyage pour ne pas fausser les autres tests (limite RG-03)
        long id = json.get("id").asLong();
        reservationRepository.deleteById(id);
    }

    @Test
    @Order(8)
    @DisplayName("RS-04 : ADHERENT sans adherentId → 201 (identité du token)")
    void rs04_adherentSansAdherentId_201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreIndisponibleId(), null));

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + a1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(a1.getUserId(), json.get("adherentId").asInt());

        reservationRepository.deleteById(json.get("id").asLong());
    }

    private Long livreIndisponibleId() {
        return booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow()
                .getBookId().longValue();
    }

    // ================================================================
    //  RS-05 : GET liste d'un ADHERENT → ses réservations seulement
    // ================================================================
    @Test
    @Order(9)
    @DisplayName("RS-05 : GET /api/reservations d'un ADHERENT → uniquement les siennes")
    void rs05_adherentListeFiltree() throws Exception {
        // a1 possède au moins une réservation (créée au test RS-04 puis supprimée,
        // on en recrée une persistante ici)
        Books livre = booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow();
        Reservation sienne = reservationRepository.save(
                new Reservation(livre, a1, StatutReservation.DISPONIBLE));

        MvcResult result = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(list.isArray() && list.size() >= 1, "a1 doit voir au moins sa réservation");
        for (JsonNode r : list) {
            assertEquals(a1.getUserId(), r.get("adherentId").asInt(),
                    "RS-05 : aucune réservation d'autrui ne doit apparaître");
        }

        // RS-04 : le paramètre adherentId fourni par l'adhérent est ignoré
        mockMvc.perform(get("/api/reservations").param("adherentId", String.valueOf(a2.getUserId()))
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].adherentId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(a1.getUserId()))));

        reservationRepository.delete(sienne);
    }

    // ================================================================
    //  BIBLIOTHECAIRE (Admin) : accès complet
    // ================================================================
    @Test
    @Order(10)
    @DisplayName("Admin : GET /api/reservations → toutes les réservations (a1 ET a2)")
    void admin_listeComplete() throws Exception {
        // Les tests précédents nettoient leurs réservations : on en recrée une pour a1.
        Books livre = booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow();
        Reservation sienne = reservationRepository.save(
                new Reservation(livre, a1, StatutReservation.EN_ATTENTE));

        MvcResult result = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        List<Integer> adherentIds = new java.util.ArrayList<>();
        list.forEach(r -> adherentIds.add(r.get("adherentId").asInt()));
        assertTrue(adherentIds.contains(a1.getUserId()), "L'admin voit la réservation de a1");
        assertTrue(adherentIds.contains(a2.getUserId()), "L'admin voit la réservation de a2");

        reservationRepository.delete(sienne);
    }

    @Test
    @Order(11)
    @DisplayName("Admin : POST avec adherentId → 201 pour l'adhérent ciblé")
    void admin_postPourAdherent() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreIndisponibleId(), a1.getUserId().longValue()));

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(a1.getUserId(), json.get("adherentId").asInt());

        reservationRepository.deleteById(json.get("id").asLong());
    }

    @Test
    @Order(12)
    @DisplayName("Admin : PATCH annuler sur n'importe quelle réservation → 200")
    void admin_annuleTout() throws Exception {
        mockMvc.perform(patch("/api/reservations/" + reservationDeA2.getId() + "/annuler")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    @Test
    @Order(13)
    @DisplayName("Admin : DELETE /api/reservations/{id} → 204")
    void admin_delete_204() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + reservationDeA2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(reservationRepository.existsById(reservationDeA2.getId()));
    }

    // ================================================================
    //  Swagger toujours accessible sans auth
    // ================================================================
    @Test
    @Order(14)
    @DisplayName("Swagger UI accessible sans authentification")
    void swagger_sansAuth_access() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result ->
                        assertTrue(result.getResponse().getStatus() == 200
                                || result.getResponse().getStatus() == 302));
    }

    // ================================================================
    //  Endpoint sécurisé GET /api/reservations — 3 scénarios demandés
    // ================================================================

    @Test
    @Order(15)
    @DisplayName("Cas 1 : GET /api/reservations sans token → 401 Unauthorized")
    void testEndpointListerSansTokenRetourne401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(16)
    @DisplayName("Cas 2 : GET /api/reservations avec token ADHERENT valide → 200 OK")
    void testEndpointListerAvecTokenAdherentValideRetourne200() throws Exception {
        // a1 possède au moins une réservation : la liste ne doit jamais être vide
        Books livre = booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow();
        Reservation sienne = reservationRepository.save(
                new Reservation(livre, a1, StatutReservation.EN_ATTENTE));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        reservationRepository.delete(sienne);
    }

    @Test
    @Order(17)
    @DisplayName("Cas 3 : token ADHERENT accédant à la réservation d'un autre → 403 Forbidden")
    void testTokenAdherentAccedantReservationDunAutreRetourne403() throws Exception {
        // Réservation fraîche pour a2 : celle créée dans @BeforeAll peut avoir été
        // supprimée par un test ordonné antérieur (ex. admin_delete_204 @Order(13)).
        Books livre = booksRepository.findAll().stream()
                .filter(b -> b.getNoOfCopies() != null && b.getNoOfCopies() == 0)
                .findFirst().orElseThrow();
        Reservation reservationDunAutre = reservationRepository.save(
                new Reservation(livre, a2, StatutReservation.EN_ATTENTE));

        // a1 (token ADHERENT) tente de consulter la réservation de a2 → refus RS-03
        mockMvc.perform(get("/api/reservations/" + reservationDunAutre.getId())
                        .header("Authorization", "Bearer " + a1Token))
                .andExpect(status().isForbidden());

        reservationRepository.delete(reservationDunAutre);
    }
}

package com.ibizabroker.bibliotheque.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'endpoint sécurisé GET /api/reservations.
 *
 * Contexte Spring complet avec la Security Filter Chain (JwtRequestFilter
 * actif) et base H2 en mémoire (profil "test") : aucune base externe n'est
 * nécessaire, la suite tourne avec la simple commande de test du projet.
 *
 * Scénarios couverts :
 *   Cas 1 : sans token                      → 401 Unauthorized
 *   Cas 2 : avec un token ADHERENT valide   → 200 OK
 *   Cas 3 : token ADHERENT accédant à la
 *           réservation d'un autre adhérent → 403 Forbidden
 *   Cas 4 : BIBLIOTHECAIRE (Admin) réserve via POST
 *           /api/reservations pour un adhérent ayant déjà
 *           3 réservations actives          → 409 Conflict (RG-03)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationEndpointSecurityIntegrationTest {

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

    private Users adherent1;
    private Users adherent2;
    private String adherent1Token;
    private String adminToken;
    private Reservation reservationDeAdherent1;
    private Reservation reservationDeAdherent2;

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
        // --- deux adhérents distincts ---
        Role roleAdherent1 = new Role();
        roleAdherent1.setRoleName("Adherent");

        adherent1 = new Users();
        adherent1.setUsername("secu_a1");
        adherent1.setName("Adherent Secu A1");
        adherent1.setPassword(passwordEncoder.encode("admin123"));
        adherent1.setRole(java.util.Set.of(roleAdherent1));
        adherent1 = usersRepository.save(adherent1);

        Role roleAdherent2 = new Role();
        roleAdherent2.setRoleName("Adherent");

        adherent2 = new Users();
        adherent2.setUsername("secu_a2");
        adherent2.setName("Adherent Secu A2");
        adherent2.setPassword(passwordEncoder.encode("admin123"));
        adherent2.setRole(java.util.Set.of(roleAdherent2));
        adherent2 = usersRepository.save(adherent2);

        // --- un livre indisponible + une réservation pour chaque adhérent ---
        Books livre1 = new Books();
        livre1.setBookName("Secu L1 - Indisponible");
        livre1.setBookAuthor("Auteur");
        livre1.setBookGenre("Test");
        livre1.setNoOfCopies(0);
        livre1 = booksRepository.save(livre1);

        Books livre2 = new Books();
        livre2.setBookName("Secu L2 - Indisponible");
        livre2.setBookAuthor("Auteur");
        livre2.setBookGenre("Test");
        livre2.setNoOfCopies(0);
        livre2 = booksRepository.save(livre2);

        reservationDeAdherent1 = reservationRepository.save(
                new Reservation(livre1, adherent1, StatutReservation.EN_ATTENTE));
        reservationDeAdherent2 = reservationRepository.save(
                new Reservation(livre2, adherent2, StatutReservation.EN_ATTENTE));

        adherent1Token = login("secu_a1", "admin123");
        adminToken = login("admin", "admin123");
    }

    /**
     * Cas 1 : aucun token fourni → la security filter chain refuse la requête
     * avant d'atteindre le contrôleur → 401 Unauthorized (jamais 403).
     */
    @Test
    @DisplayName("GET /api/reservations sans token → 401 Unauthorized")
    void testGetReservationsSansTokenRetourne401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Cas 2 : un ADHERENT valide liste les réservations → 200 OK. RS-05 :
     * il ne reçoit que les siennes (aucune réservation d'autrui dans la réponse).
     */
    @Test
    @DisplayName("GET /api/reservations avec token ADHERENT valide → 200 OK")
    void testGetReservationsAvecTokenAdherentValideRetourne200() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adherent1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(list.isArray() && list.size() >= 1,
                "L'adhérent doit voir au moins sa propre réservation");
        for (JsonNode r : list) {
            assertEquals(adherent1.getUserId(), r.get("adherentId").asInt(),
                    "RS-05 : un ADHERENT ne voit que ses propres réservations");
        }
    }

    /**
     * Cas 3 : un ADHERENT qui accède directement à la réservation d'un autre
     * adhérent (par son id) → 403 Forbidden (règle RS-03).
     */
    @Test
    @DisplayName("GET /api/reservations/{id} d'un autre adhérent avec token ADHERENT → 403 Forbidden")
    void testGetReservationAutreAdherentAvecTokenAdherentRetourne403() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationDeAdherent2.getId())
                        .header("Authorization", "Bearer " + adherent1Token))
                .andExpect(status().isForbidden());
    }

    /**
     * Contre-épreuve du cas 3 : le même appel sur sa PROPRE réservation
     * passe → 200. Cela prouve que le 403 vient bien de la propriété de
     * la réservation, pas du token ni de l'endpoint.
     */
    @Test
    @DisplayName("GET /api/reservations/{id} de sa propre réservation avec token ADHERENT → 200 OK")
    void testGetReservationPropreAvecTokenAdherentRetourne200() throws Exception {
        mockMvc.perform(get("/api/reservations/" + reservationDeAdherent1.getId())
                        .header("Authorization", "Bearer " + adherent1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherentId").value(adherent1.getUserId()));
    }

    // ==================================================================
    //  Cas 4 : RG-03 via l'endpoint — le BIBLIOTHECAIRE (Admin) réserve
    //  au nom d'un adhérent SATURÉ (3 réservations actives) → 409
    // ==================================================================

    /**
     * Cas 4 : le BIBLIOTHECAIRE (Admin) poste une réservation pour un
     * adhérent ayant déjà 3 réservations actives. Le privilège Admin ne
     * contourne pas la limite RG-03 de l'adhérent ciblé → 409 Conflict,
     * message RG-03, et aucune réservation supplémentaire en base.
     */
    @Test
    @DisplayName("POST /api/reservations par l'Admin pour un adhérent saturé (3 actives) → 409 RG-03")
    void testAdminPostPourAdherentSatureRetourne409RG03() throws Exception {
        // Arrange — un adhérent dédié, saturé à 3 réservations actives sur
        // 3 livres distincts (insérés via le repository : on maîtrise l'état).
        Role roleAdherent3 = new Role();
        roleAdherent3.setRoleName("Adherent");

        Users adherent3 = new Users();
        adherent3.setUsername("secu_a3");
        adherent3.setName("Adherent Secu A3");
        adherent3.setPassword(passwordEncoder.encode("admin123"));
        adherent3.setRole(java.util.Set.of(roleAdherent3));
        adherent3 = usersRepository.save(adherent3);

        for (int i = 1; i <= 3; i++) {
            Books livre = new Books();
            livre.setBookName("Secu Sature L" + i + " - Indisponible");
            livre.setBookAuthor("Auteur");
            livre.setBookGenre("Test");
            livre.setNoOfCopies(0);
            livre = booksRepository.save(livre);

            reservationRepository.save(
                    new Reservation(livre, adherent3, StatutReservation.EN_ATTENTE));
        }
        assertEquals(3, reservationRepository
                .countByAdherentUserIdAndStatutIn(adherent3.getUserId(),
                        java.util.List.of(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE)));

        // Un 4e livre indisponible, cible de la réservation refusée
        Books livreCible = new Books();
        livreCible.setBookName("Secu Sature L4 - Indisponible");
        livreCible.setBookAuthor("Auteur");
        livreCible.setBookGenre("Test");
        livreCible.setNoOfCopies(0);
        livreCible = booksRepository.save(livreCible);

        long avant = reservationRepository.count();

        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreCible.getBookId().longValue(), adherent3.getUserId().longValue()));

        // Act & Assert — l'Admin vise un adhérent saturé : 409 RG-03
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("RG-03")));

        // Rien n'a été écrit : l'adhérent a toujours exactement 3 actives
        assertEquals(avant, reservationRepository.count());
        assertEquals(3, reservationRepository
                .countByAdherentUserIdAndStatutIn(adherent3.getUserId(),
                        java.util.List.of(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE)));
    }

    /**
     * Contre-épreuve du cas 4 : le MÊME Admin, le MÊME endpoint, mais pour
     * un adhérent SOUS la limite (adherent1, 1 seule réservation active)
     * → 201 Created. Cela prouve que le 409 vient bien du quota de
     * l'adhérent ciblé, et non du compte Admin ni de l'endpoint.
     */
    @Test
    @DisplayName("POST /api/reservations par l'Admin pour un adhérent sous la limite → 201")
    void testAdminPostPourAdherentSousLaLimiteRetourne201() throws Exception {
        // adherent1 n'a qu'une réservation active (setUp) : sous la limite de 3
        Books livreLibre = new Books();
        livreLibre.setBookName("Secu Admin L1 - Indisponible");
        livreLibre.setBookAuthor("Auteur");
        livreLibre.setBookGenre("Test");
        livreLibre.setNoOfCopies(0);
        livreLibre = booksRepository.save(livreLibre);

        String body = objectMapper.writeValueAsString(
                new ReservationCreateDTO(livreLibre.getBookId().longValue(), adherent1.getUserId().longValue()));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(adherent1.getUserId()))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }
}

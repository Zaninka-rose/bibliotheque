package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Books livreIndisponible;
    private Books livreDisponible;
    private Users adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(1);
        livreIndisponible.setBookName("Le Petit Prince");
        livreIndisponible.setNoOfCopies(0);

        livreDisponible = new Books();
        livreDisponible.setBookId(2);
        livreDisponible.setBookName("L'Étranger");
        livreDisponible.setNoOfCopies(3);

        adherent = new Users();
        adherent.setUserId(10);
        adherent.setName("Zaninka Rose");
        adherent.setUsername("zaninka");

        // Par défaut : l'appelant est un ADHERENT (hasRole Admin → false).
        when(authenticatedUserService.hasRole("Admin")).thenReturn(false);
        when(authenticatedUserService.getCurrentUser()).thenReturn(adherent);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(10);
    }

    // ================================================================ creerReservation
    @Nested
    @DisplayName("creerReservation")
    class CreerReservationTests {

        @Test
        @DisplayName("RS-04 : un ADHERENT ne peut pas réserver pour un autre adhérent (adherentId ignoré)")
        void creerReservation_adherent_RS04_identiteDepuisToken() {
            // Arrange : le corps prétend être l'adhérent 999 (inexistant)
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 999L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(0L);

            Reservation saved = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            saved.setId(100L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            // Act
            ReservationResponseDTO result = reservationService.creerReservation(dto);

            // Assert : la réservation est créée pour l'utilisateur DU TOKEN (10), pas 999
            assertEquals(10L, result.getAdherentId());
            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertEquals(10, captor.getValue().getAdherent().getUserId());
            // RS-04 : la table users n'est JAMAIS interrogée avec l'id du corps
            verify(usersRepository, never()).findById(anyInt());
        }

        @Test
        @DisplayName("RS-04 : un ADHERENT peut réserver sans fournir adherentId")
        void creerReservation_adherent_sansAdherentId() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(0L);
            Reservation saved = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            saved.setId(101L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            ReservationResponseDTO result = reservationService.creerReservation(dto);

            assertEquals(10L, result.getAdherentId());
        }

        @Test
        @DisplayName("Un Admin (BIBLIOTHECAIRE) peut réserver pour n'importe quel adhérent")
        void creerReservation_admin_pourAutre() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
            // #4 : la cible doit porter le rôle Adherent
            Role roleAdherent = new Role();
            roleAdherent.setRoleName("Adherent");
            adherent.setRole(java.util.Set.of(roleAdherent));

            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(0L);
            Reservation saved = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            saved.setId(102L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            ReservationResponseDTO result = reservationService.creerReservation(dto);

            assertEquals(10L, result.getAdherentId());
        }

        @Test
        @DisplayName("Admin sans adherentId → ValidationException (400)")
        void creerReservation_admin_sansAdherentId_400() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("adherentId"));
        }

        @Test
        @DisplayName("RG-01 : lever LivreDisponibleException si le livre a des copies")
        void creerReservation_livreDisponible_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(2L, 10L);
            when(booksRepository.findById(2)).thenReturn(Optional.of(livreDisponible));

            LivreDisponibleException ex = assertThrows(LivreDisponibleException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-01"));
            assertTrue(ex.getMessage().contains("L'Étranger"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("RG-02 : lever ReservationDejaActiveException si réservation active déjà existante")
        void creerReservation_doublonActif_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));

            Reservation existante = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(10, 1, Set.of(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE)))
                    .thenReturn(List.of(existante));

            ReservationDejaActiveException ex = assertThrows(ReservationDejaActiveException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-02"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("RG-03 : lever LimiteReservationsAtteinteException si 3 réservations actives")
        void creerReservation_limiteAtteinte_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(3L);

            LimiteReservationsAtteinteException ex = assertThrows(LimiteReservationsAtteinteException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-03"));
            verify(reservationRepository, never()).save(any());
        }

        // ------------------------------------------------------------
        // RG-03 : les cas limite (2 actives → OK, 3 actives → refus) sont
        // couverts par la suite dédiée ReservationLimiteActiveTest.
        // ------------------------------------------------------------

        @Test
        @DisplayName("Création réussie avec statut EN_ATTENTE (identité du token)")
        void creerReservation_succes() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(1L);

            Reservation saved = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            saved.setId(100L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            ReservationResponseDTO result = reservationService.creerReservation(dto);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Le Petit Prince", result.getLivreTitre());
            assertEquals("Zaninka Rose", result.getAdherentNom());
            assertEquals(StatutReservation.EN_ATTENTE, result.getStatut());
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Lever ValidationException si livreId est null")
        void creerReservation_livreIdNull_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(null, 10L);
            assertThrows(ValidationException.class,
                    () -> reservationService.creerReservation(dto));
        }

        @Test
        @DisplayName("Lever NotFoundException si livre introuvable")
        void creerReservation_livreIntrouvable_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(999L, null);
            when(booksRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> reservationService.creerReservation(dto));
        }

        @Test
        @DisplayName("Admin : lever NotFoundException si adhérent ciblé introuvable")
        void creerReservation_admin_adherentIntrouvable_lanceException() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 999L);
            when(usersRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> reservationService.creerReservation(dto));
        }

        @Test
        @DisplayName("#4 : Admin ne peut pas réserver au nom d'un compte non-adhérent (400)")
        void creerReservation_admin_cibleNonAdherent_refusee() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            Users adminCible = new Users();
            adminCible.setUserId(50);
            adminCible.setUsername("bibliothecaire2");
            Role roleAdmin = new Role();
            roleAdmin.setRoleName("Admin");
            adminCible.setRole(java.util.Set.of(roleAdmin));

            when(usersRepository.findById(50)).thenReturn(Optional.of(adminCible));
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 50L);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("rôle Adherent"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("#4 : Admin peut réserver au nom d'un adhérent avec rôle multiple (dont Adherent)")
        void creerReservation_admin_cibleRoleMultiple_acceptee() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            Users cibleMulti = new Users();
            cibleMulti.setUserId(60);
            cibleMulti.setUsername("multi");
            Role roleAdherent = new Role();
            roleAdherent.setRoleName("Adherent");
            Role roleAutre = new Role();
            roleAutre.setRoleName("Invite");
            cibleMulti.setRole(java.util.Set.of(roleAdherent, roleAutre));

            when(usersRepository.findById(60)).thenReturn(Optional.of(cibleMulti));
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(60), anyCollection()))
                    .thenReturn(0L);
            Reservation saved = new Reservation(livreIndisponible, cibleMulti, StatutReservation.EN_ATTENTE);
            saved.setId(600L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            ReservationResponseDTO result = reservationService.creerReservation(
                    new ReservationCreateDTO(1L, 60L));

            assertEquals(60L, result.getAdherentId());
        }

        @Test
        @DisplayName("#4 : cible sans aucun rôle → refus (400)")
        void creerReservation_admin_cibleSansRole_refusee() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            Users cibleSansRole = new Users();
            cibleSansRole.setUserId(70);
            cibleSansRole.setUsername("sansrole");
            cibleSansRole.setRole(null);

            when(usersRepository.findById(70)).thenReturn(Optional.of(cibleSansRole));
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 70L);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("rôle Adherent"));
            verify(reservationRepository, never()).save(any());
        }
    }

    // ================================================================ listerReservations
    @Nested
    @DisplayName("listerReservations (RS-05)")
    class ListerReservationsTests {

        @Test
        @DisplayName("RS-05 : un ADHERENT ne voit que ses propres réservations (requête en base)")
        void listerReservations_adherent_voitSeulementLesSiennes() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);

            // Le filtrage est désormais exécuté EN BASE : le repository est
            // interrogé avec l'id du token, jamais avec un paramètre client.
            when(reservationRepository.findByAdherentUserId(10))
                    .thenReturn(List.of(sienne));

            List<ReservationResponseDTO> result = reservationService.listerReservations(null, null);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getAdherentId());
            // RS-05 : la requête porte sur l'id du token (10), pas sur un
            // éventuel paramètre client ; findAll() n'est plus utilisé.
            verify(reservationRepository).findByAdherentUserId(10);
            verify(reservationRepository, never()).findAll();
        }

        @Test
        @DisplayName("RS-04 : le paramètre adherentId d'un ADHERENT est ignoré (filtre imposé par le token)")
        void listerReservations_adherent_filtreClientIgnore() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserId(10))
                    .thenReturn(List.of(sienne));

            // L'adhérent demande les réservations de l'adhérent 99 : ignoré,
            // la requête en base part avec l'id du token (10).
            List<ReservationResponseDTO> result = reservationService.listerReservations(null, 99L);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getAdherentId());
            verify(reservationRepository, never()).findByAdherentUserId(99);
        }

        @Test
        @DisplayName("Un Admin voit toutes les réservations")
        void listerReservations_admin_voitTout() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation r1 = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            Reservation r2 = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findAll()).thenReturn(List.of(r1, r2));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            List<ReservationResponseDTO> result = reservationService.listerReservations(null, null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Un Admin peut filtrer par statut (requête en base)")
        void listerReservations_admin_filtreParStatut() {
            Reservation enAttente = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByStatutIn(anyCollection()))
                    .thenReturn(List.of(enAttente));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            List<ReservationResponseDTO> result =
                    reservationService.listerReservations(StatutReservation.EN_ATTENTE, null);

            assertEquals(1, result.size());
            verify(reservationRepository).findByStatutIn(anyCollection());
            verify(reservationRepository, never()).findAll();
        }

        @Test
        @DisplayName("Un Admin peut filtrer par adherentId + statut (requête en base)")
        void listerReservations_admin_filtreAdherentEtStatut() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(List.of(sienne));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            List<ReservationResponseDTO> result =
                    reservationService.listerReservations(StatutReservation.EN_ATTENTE, 10L);

            assertEquals(1, result.size());
            verify(reservationRepository).findByAdherentUserIdAndStatutIn(eq(10), anyCollection());
        }

        @Test
        @DisplayName("Un ADHERENT avec filtre de statut : requête en base id + statut")
        void listerReservations_adherent_avecStatut_requeteEnBase() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(List.of(sienne));

            List<ReservationResponseDTO> result =
                    reservationService.listerReservations(StatutReservation.EN_ATTENTE, null);

            assertEquals(1, result.size());
            verify(reservationRepository).findByAdherentUserIdAndStatutIn(eq(10), anyCollection());
            verify(reservationRepository, never()).findAll();
        }

        @Test
        @DisplayName("Un Admin peut filtrer par adherentId (requête en base)")
        void listerReservations_admin_filtreParAdherent() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation r2 = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserId(99)).thenReturn(List.of(r2));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            List<ReservationResponseDTO> result = reservationService.listerReservations(null, 99L);

            assertEquals(1, result.size());
            assertEquals(99L, result.get(0).getAdherentId());
            verify(reservationRepository).findByAdherentUserId(99);
        }
    }

    // ================================================================ annulerReservation
    @Nested
    @DisplayName("annulerReservation")
    class AnnulerReservationTests {

        @Test
        @DisplayName("RS-03 : un ADHERENT ne peut pas annuler la réservation d'un autre (403)")
        void annulerReservation_adherent_reservationAutre_403() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation reservation = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            AccesRefuseException ex = assertThrows(AccesRefuseException.class,
                    () -> reservationService.annulerReservation(1L));
            assertTrue(ex.getMessage().contains("RS-03"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Annulation réussie de sa propre réservation EN_ATTENTE")
        void annulerReservation_enAttente_succes() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReservationResponseDTO result = reservationService.annulerReservation(1L);

            assertEquals(StatutReservation.ANNULEE, result.getStatut());
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("Annulation réussie par l'Admin d'une réservation quelconque")
        void annulerReservation_admin_succes() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation reservation = new Reservation(livreIndisponible, autre, StatutReservation.DISPONIBLE);
            reservation.setId(2L);
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            ReservationResponseDTO result = reservationService.annulerReservation(2L);

            assertEquals(StatutReservation.ANNULEE, result.getStatut());
        }

        @Test
        @DisplayName("RG-05/RG-06 : refus d'annulation d'une réservation ANNULEE")
        void annulerReservation_dejaAnnulee_lanceException() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.ANNULEE);
            reservation.setId(3L);
            when(reservationRepository.findById(3L)).thenReturn(Optional.of(reservation));

            ReservationNonAnnulableException ex = assertThrows(ReservationNonAnnulableException.class,
                    () -> reservationService.annulerReservation(3L));
            assertTrue(ex.getMessage().contains("RG-05"));
            assertTrue(ex.getMessage().contains("ANNULEE"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("RG-05/RG-06 : refus d'annulation d'une réservation EXPIREE")
        void annulerReservation_expiree_lanceException() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.EXPIREE);
            reservation.setId(4L);
            when(reservationRepository.findById(4L)).thenReturn(Optional.of(reservation));

            assertThrows(ReservationNonAnnulableException.class,
                    () -> reservationService.annulerReservation(4L));
        }

        @Test
        @DisplayName("RG-05/RG-06 : refus d'annulation d'une réservation HONOREE")
        void annulerReservation_honoree_lanceException() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.HONOREE);
            reservation.setId(5L);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

            assertThrows(ReservationNonAnnulableException.class,
                    () -> reservationService.annulerReservation(5L));
        }

        @Test
        @DisplayName("Lever ReservationNotFoundException si id inconnu")
        void annulerReservation_idInconnu_lanceException() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ReservationNotFoundException.class,
                    () -> reservationService.annulerReservation(999L));
        }
    }

    // ================================================================ consulterReservation
    @Nested
    @DisplayName("consulterReservation")
    class ConsulterReservationTests {

        @Test
        @DisplayName("RS-03 : un ADHERENT ne peut pas consulter la réservation d'un autre (403)")
        void consulterReservation_adherent_reservationAutre_403() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation reservation = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThrows(AccesRefuseException.class,
                    () -> reservationService.consulterReservation(1L));
        }

        @Test
        @DisplayName("Consultation réussie de sa propre réservation")
        void consulterReservation_succes() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            ReservationResponseDTO result = reservationService.consulterReservation(1L);

            assertEquals(1L, result.getId());
            assertEquals("Le Petit Prince", result.getLivreTitre());
        }

        @Test
        @DisplayName("L'Admin peut consulter n'importe quelle réservation")
        void consulterReservation_admin_ok() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation reservation = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            ReservationResponseDTO result = reservationService.consulterReservation(1L);

            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Lever ReservationNotFoundException si id inconnu")
        void consulterReservation_idInconnu_lanceException() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ReservationNotFoundException.class,
                    () -> reservationService.consulterReservation(999L));
        }
    }

    // ================================================================ supprimerReservation
    @Nested
    @DisplayName("supprimerReservation")
    class SupprimerReservationTests {

        @Test
        @DisplayName("Suppression réussie")
        void supprimerReservation_succes() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.ANNULEE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            reservationService.supprimerReservation(1L);

            verify(reservationRepository).delete(reservation);
        }

        @Test
        @DisplayName("Lever ReservationNotFoundException si id inconnu (appelé par l'Admin)")
        void supprimerReservation_idInconnu_lanceException() {
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ReservationNotFoundException.class,
                    () -> reservationService.supprimerReservation(999L));
            verify(reservationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("#1 Défense en profondeur : un ADHERENT est refusé au niveau service (même sans @PreAuthorize)")
        void supprimerReservation_adherent_refuseAuNiveauService() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.ANNULEE);
            reservation.setId(2L);
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(reservation));

            // hasRole("Admin") reste false (setup) : la garde du service doit
            // lever AccesRefuseException AVANT toute suppression.
            AccesRefuseException ex = assertThrows(AccesRefuseException.class,
                    () -> reservationService.supprimerReservation(2L));
            assertTrue(ex.getMessage().contains("RS-02"));
            verify(reservationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("#1 Défense en profondeur : l'Admin passe la garde du service")
        void supprimerReservation_admin_passeLaGarde() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.ANNULEE);
            reservation.setId(3L);
            when(reservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            reservationService.supprimerReservation(3L);

            verify(reservationRepository).delete(reservation);
        }
    }
}

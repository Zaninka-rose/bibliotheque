package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
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
        // RG-03 : cas limite — la limite est LIMITE_RESERVATIONS_ACTIVES = 3
        // ------------------------------------------------------------

        @Test
        @DisplayName("RG-03 (cas limite) : un adhérent avec 2 réservations actives peut créer la troisième")
        void testAdherentAvec2ReservationsActivesPeutCreerLaTroisieme() {
            // Arrange — repository entièrement mocké : aucune base de données nécessaire
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            // 2 réservations actives < limite de 3 → la création doit passer
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(2L);

            Reservation sauvegardee = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            sauvegardee.setId(200L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(sauvegardee);

            // Act
            ReservationResponseDTO resultat = reservationService.creerReservation(dto);

            // Assert — la troisième réservation est bien créée
            assertNotNull(resultat);
            assertEquals(200L, resultat.getId());
            assertEquals(StatutReservation.EN_ATTENTE, resultat.getStatut());
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("RG-03 : un adhérent avec 3 réservations actives reçoit un refus")
        void testAdherentAvec3ReservationsActivesRecoitUnRefus() {
            // Arrange — repository entièrement mocké : aucune base de données nécessaire
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            // 3 réservations actives = limite atteinte → refus
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(3L);

            // Act & Assert — la quatrième réservation est refusée
            LimiteReservationsAtteinteException ex = assertThrows(LimiteReservationsAtteinteException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-03"));
            verify(reservationRepository, never()).save(any());
        }

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
    }

    // ================================================================ listerReservations
    @Nested
    @DisplayName("listerReservations (RS-05)")
    class ListerReservationsTests {

        @Test
        @DisplayName("RS-05 : un ADHERENT ne voit que ses propres réservations")
        void listerReservations_adherent_voitSeulementLesSiennes() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            Users autre = new Users();
            autre.setUserId(99);
            Reservation pasSienne = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);

            when(reservationRepository.findAll()).thenReturn(List.of(sienne, pasSienne));

            List<ReservationResponseDTO> result = reservationService.listerReservations(null, null);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getAdherentId());
        }

        @Test
        @DisplayName("RS-04 : le paramètre adherentId d'un ADHERENT est ignoré (filtre imposé par le token)")
        void listerReservations_adherent_filtreClientIgnore() {
            Reservation sienne = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findAll()).thenReturn(List.of(sienne));

            // L'adhérent demande les réservations de l'adhérent 99 : ignoré
            List<ReservationResponseDTO> result = reservationService.listerReservations(null, 99L);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getAdherentId());
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
        @DisplayName("Un Admin peut filtrer par adherentId")
        void listerReservations_admin_filtreParAdherent() {
            Users autre = new Users();
            autre.setUserId(99);
            Reservation r1 = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            Reservation r2 = new Reservation(livreIndisponible, autre, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findAll()).thenReturn(List.of(r1, r2));
            when(authenticatedUserService.hasRole("Admin")).thenReturn(true);

            List<ReservationResponseDTO> result = reservationService.listerReservations(null, 99L);

            assertEquals(1, result.size());
            assertEquals(99L, result.get(0).getAdherentId());
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
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.ANNULEE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            reservationService.supprimerReservation(1L);

            verify(reservationRepository).delete(reservation);
        }

        @Test
        @DisplayName("Lever ReservationNotFoundException si id inconnu")
        void supprimerReservation_idInconnu_lanceException() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ReservationNotFoundException.class,
                    () -> reservationService.supprimerReservation(999L));
            verify(reservationRepository, never()).delete(any());
        }
    }
}

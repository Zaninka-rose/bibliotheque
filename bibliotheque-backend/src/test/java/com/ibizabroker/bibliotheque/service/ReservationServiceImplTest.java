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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

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
    }

    // ================================================================ creerReservation
    @Nested
    @DisplayName("creerReservation")
    class CreerReservationTests {

        @Test
        @DisplayName("RG-01 : lever LivreDisponibleException si le livre a des copies")
        void creerReservation_livreDisponible_lanceException() {
            // Arrange
            ReservationCreateDTO dto = new ReservationCreateDTO(2L, 10L);
            when(booksRepository.findById(2)).thenReturn(Optional.of(livreDisponible));
            when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));

            // Act & Assert
            LivreDisponibleException ex = assertThrows(LivreDisponibleException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-01"));
            assertTrue(ex.getMessage().contains("L'Étranger"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("RG-02 : lever ReservationDejaActiveException si réservation active déjà existante")
        void creerReservation_doublonActif_lanceException() {
            // Arrange
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));

            Reservation existante = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(10, 1, Set.of(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE)))
                    .thenReturn(List.of(existante));

            // Act & Assert
            ReservationDejaActiveException ex = assertThrows(ReservationDejaActiveException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-02"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("RG-03 : lever LimiteReservationsAtteinteException si 3 réservations actives")
        void creerReservation_limiteAtteinte_lanceException() {
            // Arrange
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(3L);

            // Act & Assert
            LimiteReservationsAtteinteException ex = assertThrows(LimiteReservationsAtteinteException.class,
                    () -> reservationService.creerReservation(dto));
            assertTrue(ex.getMessage().contains("RG-03"));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Création réussie avec statut EN_ATTENTE")
        void creerReservation_succes() {
            // Arrange
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
            when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                    .thenReturn(1L);

            Reservation saved = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            saved.setId(100L);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

            // Act
            ReservationResponseDTO result = reservationService.creerReservation(dto);

            // Assert
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
        @DisplayName("Lever ValidationException si adherentId est null")
        void creerReservation_adherentIdNull_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            assertThrows(ValidationException.class,
                    () -> reservationService.creerReservation(dto));
        }

        @Test
        @DisplayName("Lever NotFoundException si livre introuvable")
        void creerReservation_livreIntrouvable_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(999L, 10L);
            when(booksRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> reservationService.creerReservation(dto));
        }

        @Test
        @DisplayName("Lever NotFoundException si adhérent introuvable")
        void creerReservation_adherentIntrouvable_lanceException() {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 999L);
            when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
            when(usersRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> reservationService.creerReservation(dto));
        }
    }

    // ================================================================ annulerReservation
    @Nested
    @DisplayName("annulerReservation")
    class AnnulerReservationTests {

        @Test
        @DisplayName("Annulation réussie d'une réservation EN_ATTENTE")
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
        @DisplayName("Annulation réussie d'une réservation DISPONIBLE")
        void annulerReservation_disponible_succes() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.DISPONIBLE);
            reservation.setId(2L);
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
        @DisplayName("Consultation réussie")
        void consulterReservation_succes() {
            Reservation reservation = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
            reservation.setId(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            ReservationResponseDTO result = reservationService.consulterReservation(1L);

            assertEquals(1L, result.getId());
            assertEquals("Le Petit Prince", result.getLivreTitre());
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

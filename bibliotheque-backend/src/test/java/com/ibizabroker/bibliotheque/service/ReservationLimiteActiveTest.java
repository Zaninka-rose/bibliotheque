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
import com.ibizabroker.bibliotheque.exceptions.LimiteReservationsAtteinteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de la règle métier RG-03 — limite de 3 réservations actives.
 *
 * La couche repository est entièrement simulée avec Mockito : aucun appel
 * à une vraie base de données, aucun contexte Spring, aucun conteneur.
 * La suite s'exécute donc même si aucune base ne tourne.
 *
 * Couvre à la fois :
 *   - le flux ADHERENT (l'adhérent réserve pour lui-même),
 *   - le flux BIBLIOTHECAIRE (l'Admin réserve au nom d'un adhérent ciblé,
 *     la limite s'appliquant à l'adhérent ciblé, pas à l'Admin).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationLimiteActiveTest {

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
    private Users adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(1);
        livreIndisponible.setBookName("L1 - Indisponible");
        livreIndisponible.setNoOfCopies(0);

        adherent = new Users();
        adherent.setUserId(10);
        adherent.setName("Adherent Test");

        // L'appelant est un ADHERENT (pas Admin) : l'identité vient du token.
        when(authenticatedUserService.hasRole("Admin")).thenReturn(false);
        when(authenticatedUserService.getCurrentUser()).thenReturn(adherent);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(10);
    }

    /**
     * Cas 1 : un adhérent ayant 2 réservations actives peut en créer une troisième.
     * 2 < 3 → la création est acceptée et EN_ATTENTE.
     */
    @Test
    @DisplayName("RG-03 : un adhérent avec 2 réservations actives peut créer la troisième (succès)")
    void testAdherentAvec2ReservationsActivesPeutCreerLaTroisieme() {
        // Arrange — repository entièrement mocké, aucune base de données
        ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
        when(booksRepository.findById(1)).thenReturn(java.util.Optional.of(livreIndisponible));
        // RG-02 : pas de doublon actif sur ce livre
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                .thenReturn(Collections.emptyList());
        // RG-03 : 2 réservations actives, sous la limite de 3
        when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                .thenReturn(2L);

        Reservation sauvegardee = new Reservation(livreIndisponible, adherent, StatutReservation.EN_ATTENTE);
        sauvegardee.setId(300L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(sauvegardee);

        // Act
        ReservationResponseDTO resultat = reservationService.creerReservation(dto);

        // Assert — la troisième réservation est bien créée
        assertNotNull(resultat);
        assertEquals(300L, resultat.getId());
        assertEquals(StatutReservation.EN_ATTENTE, resultat.getStatut());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getAdherent().getUserId());
    }

    /**
     * Cas 2 : un adhérent ayant 3 réservations actives reçoit un refus.
     * 3 = limite → LimiteReservationsAtteinteException (mappée en 409 côté API).
     */
    @Test
    @DisplayName("RG-03 : un adhérent avec 3 réservations actives reçoit un refus (exception)")
    void testAdherentAvec3ReservationsActivesRecoitUnRefus() {
        // Arrange — repository entièrement mocké, aucune base de données
        ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
        when(booksRepository.findById(1)).thenReturn(java.util.Optional.of(livreIndisponible));
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                .thenReturn(Collections.emptyList());
        // RG-03 : 3 réservations actives, limite atteinte
        when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(10), anyCollection()))
                .thenReturn(3L);

        // Act & Assert — la quatrième réservation est refusée
        LimiteReservationsAtteinteException exception = assertThrows(
                LimiteReservationsAtteinteException.class,
                () -> reservationService.creerReservation(dto));
        assertTrue(exception.getMessage().contains("RG-03"));
        // Aucune écriture ne doit avoir lieu
        verify(reservationRepository, never()).save(any());
    }

    // ==================================================================
    // RG-03 côté BIBLIOTHECAIRE (Admin) : il réserve AU NOM D'UN ADHÉRENt,
    // la limite de 3 réservations actives s'applique donc à l'adhérent
    // ciblé (adherentId du corps), jamais au compte du bibliothécaire.
    // ==================================================================

    /**
     * Cas Admin 1 : un BIBLIOTHECAIRE peut créer une réservation au nom d'un
     * adhérent qui n'a que 2 réservations actives (succès), et elle appartient
     * bien à l'adhérent ciblé.
     */
    @Test
    @DisplayName("RG-03 (Admin) : le BIBLIOTHECAIRE réserve pour un adhérent avec 2 actives → succès, au nom de l'adhérent")
    void testAdminCreePourAdherentAvec2ReservationsActivesSucces() {
        // Arrange — repository entièrement mocké, aucune base de données
        Users adherentCible = nouvelAdherentCible();

        when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
        when(usersRepository.findById(20)).thenReturn(java.util.Optional.of(adherentCible));
        when(booksRepository.findById(1)).thenReturn(java.util.Optional.of(livreIndisponible));
        // RG-02 : pas de doublon actif pour l'adhérent CIBLE
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                .thenReturn(Collections.emptyList());
        // RG-03 : l'adhérent ciblé a 2 réservations actives < limite de 3
        when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(20), anyCollection()))
                .thenReturn(2L);

        Reservation sauvegardee = new Reservation(livreIndisponible, adherentCible, StatutReservation.EN_ATTENTE);
        sauvegardee.setId(400L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(sauvegardee);

        // Act — l'Admin réserve le livre 1 pour l'adhérent 20
        ReservationResponseDTO resultat = reservationService.creerReservation(new ReservationCreateDTO(1L, 20L));

        // Assert — la réservation est créée AU NOM de l'adhérent ciblé
        assertNotNull(resultat);
        assertEquals(20L, resultat.getAdherentId());
        assertEquals(StatutReservation.EN_ATTENTE, resultat.getStatut());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(20, captor.getValue().getAdherent().getUserId());
    }

    /**
     * Cas Admin 2 : un BIBLIOTHECAIRE qui réserve pour un adhérent ayant déjà
     * 3 réservations actives reçoit le même refus RG-03 — le privilège Admin
     * ne contourne pas la limite de l'adhérent ciblé.
     */
    @Test
    @DisplayName("RG-03 (Admin) : le BIBLIOTHECAIRE réserve pour un adhérent avec 3 actives → refus")
    void testAdminCreePourAdherentAvec3ReservationsActivesRecoitUnRefus() {
        // Arrange — repository entièrement mocké, aucune base de données
        Users adherentCible = nouvelAdherentCible();

        when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
        when(usersRepository.findById(20)).thenReturn(java.util.Optional.of(adherentCible));
        when(booksRepository.findById(1)).thenReturn(java.util.Optional.of(livreIndisponible));
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                .thenReturn(Collections.emptyList());
        // RG-03 : l'adhérent ciblé a déjà 3 réservations actives = limite
        when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(20), anyCollection()))
                .thenReturn(3L);

        // Act & Assert — refus RG-03 malgré le rôle Admin, aucune écriture
        LimiteReservationsAtteinteException exception = assertThrows(
                LimiteReservationsAtteinteException.class,
                () -> reservationService.creerReservation(new ReservationCreateDTO(1L, 20L)));
        assertTrue(exception.getMessage().contains("RG-03"));
        verify(reservationRepository, never()).save(any());
    }

    /**
     * Cas Admin 3 (sémantique de la règle) : la limite s'applique à l'adhérent
     * CIBLÉ, pas au bibliothécaire lui-même — même si le compte de l'Admin
     * avait saturé son propre quota, il peut toujours réserver pour un
     * adhérent sous la limite (le compteur n'est jamais interrogé avec l'id
     * de l'Admin).
     */
    @Test
    @DisplayName("RG-03 (Admin) : la limite porte sur l'adhérent ciblé, jamais sur le compte du bibliothécaire")
    void testLimiteAppliqueeAAdherentCibleJamaisAuCompteBibliothecaire() {
        // Arrange — repository entièrement mocké, aucune base de données
        Users adherentCible = nouvelAdherentCible();

        when(authenticatedUserService.hasRole("Admin")).thenReturn(true);
        when(usersRepository.findById(20)).thenReturn(java.util.Optional.of(adherentCible));
        when(booksRepository.findById(1)).thenReturn(java.util.Optional.of(livreIndisponible));
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), anyCollection()))
                .thenReturn(Collections.emptyList());
        // RG-03 : l'adhérent ciblé est sous la limite
        when(reservationRepository.countByAdherentUserIdAndStatutIn(eq(20), anyCollection()))
                .thenReturn(0L);

        Reservation sauvegardee = new Reservation(livreIndisponible, adherentCible, StatutReservation.EN_ATTENTE);
        sauvegardee.setId(401L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(sauvegardee);

        // Act — l'Admin (id 10) réserve pour l'adhérent 20
        ReservationResponseDTO resultat = reservationService.creerReservation(new ReservationCreateDTO(1L, 20L));

        // Assert — succès, et le quota de l'Admin (id 10) n'a jamais été compté
        assertNotNull(resultat);
        assertEquals(20L, resultat.getAdherentId());
        verify(reservationRepository).countByAdherentUserIdAndStatutIn(eq(20), anyCollection());
        verify(reservationRepository, never()).countByAdherentUserIdAndStatutIn(eq(10), anyCollection());
    }

    /**
     * #4 : tout adhérent ciblé par l'Admin doit porter le rôle "Adherent"
     * (le service refuse désormais une cible non-adhérente).
     */
    private Users nouvelAdherentCible() {
        Users adherentCible = new Users();
        adherentCible.setUserId(20);
        adherentCible.setName("Adherent Cible");
        adherentCible.setUsername("cible");
        com.ibizabroker.bibliotheque.entity.Role roleAdherent =
                new com.ibizabroker.bibliotheque.entity.Role();
        roleAdherent.setRoleName("Adherent");
        adherentCible.setRole(java.util.Set.of(roleAdherent));
        return adherentCible;
    }
}

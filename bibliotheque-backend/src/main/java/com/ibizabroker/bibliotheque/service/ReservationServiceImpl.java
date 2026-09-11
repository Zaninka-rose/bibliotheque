package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationMapper;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final Set<StatutReservation> STATUTS_ACTIFS = Set.of(
            StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE
    );

    private static final long LIMITE_RESERVATIONS_ACTIVES = 3;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    // ------------------------------------------------------------------ 1
    @Override
    public ReservationResponseDTO creerReservation(ReservationCreateDTO dto) {

        // --- validation des champs requis ---
        if (dto.getLivreId() == null) {
            throw new ValidationException("livreId est requis");
        }

        // --- RS-04 : l'identité vient du token, JAMAIS du corps de la requête ---
        // Un ADHERENT ne peut réserver que pour lui-même, même s'il fournit
        // l'adherentId de quelqu'un d'autre. Un BIBLIOTHECAIRE (Admin) peut
        // réserver au nom de n'importe quel adhérent.
        Users adherent;
        if (authenticatedUserService.hasRole("Admin")) {
            if (dto.getAdherentId() == null) {
                throw new ValidationException("adherentId est requis");
            }
            adherent = usersRepository.findById(dto.getAdherentId().intValue())
                    .orElseThrow(() -> new NotFoundException(
                            "Adhérent avec id " + dto.getAdherentId() + " introuvable"));
        } else {
            adherent = authenticatedUserService.getCurrentUser();
        }

        // --- existence du livre ---
        Books livre = booksRepository.findById(dto.getLivreId().intValue())
                .orElseThrow(() -> new NotFoundException(
                        "Livre avec id " + dto.getLivreId() + " introuvable"));

        // --- RG-01 : le livre doit être INDISPONIBLE pour qu'une réservation ait du sens ---
        if (livre.getNoOfCopies() != null && livre.getNoOfCopies() > 0) {
            throw new LivreDisponibleException(livre.getBookName());
        }

        // --- RG-02 : pas de doublon de réservation active ---
        List<Reservation> existantes = reservationRepository
                .findByAdherentUserIdAndLivreBookIdAndStatutIn(
                        adherent.getUserId(), livre.getBookId(), STATUTS_ACTIFS);
        if (!existantes.isEmpty()) {
            throw new ReservationDejaActiveException();
        }

        // --- RG-03 : pas plus de 3 réservations actives ---
        long nbActives = reservationRepository
                .countByAdherentUserIdAndStatutIn(adherent.getUserId(), STATUTS_ACTIFS);
        if (nbActives >= LIMITE_RESERVATIONS_ACTIVES) {
            throw new LimiteReservationsAtteinteException();
        }

        // --- création (dates calculées par @PrePersist) ---
        Reservation reservation = new Reservation(livre, adherent, StatutReservation.EN_ATTENTE);
        Reservation saved = reservationRepository.save(reservation);
        return ReservationMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------ 2
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> listerReservations(StatutReservation statutFiltre,
                                                           Long adherentIdFiltre) {

        // --- RS-05 : un ADHERENT ne voit que ses propres réservations ---
        Integer filtreImpose = null;
        if (authenticatedUserService.hasRole("Admin")) {
            // BIBLIOTHECAIRE : voit tout, filtre optionnel par adhérent
            if (adherentIdFiltre != null) {
                filtreImpose = adherentIdFiltre.intValue();
            }
        } else {
            // ADHERENT : le filtre est imposé par le token ; tout paramètre
            // adherentId du client est ignoré (RS-04).
            filtreImpose = authenticatedUserService.getCurrentUserId();
        }

        final Integer filtreApplique = filtreImpose;

        Stream<Reservation> stream = reservationRepository.findAll().stream();

        if (statutFiltre != null) {
            stream = stream.filter(r -> r.getStatut() == statutFiltre);
        }
        if (filtreApplique != null) {
            stream = stream.filter(r -> r.getAdherent().getUserId().equals(filtreApplique));
        }

        return stream.map(ReservationMapper::toResponse).toList();
    }

    // ------------------------------------------------------------------ 3
    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDTO consulterReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        // --- RS-03 : un ADHERENT ne peut consulter que ses propres réservations ---
        verifierAppartenance(reservation);

        return ReservationMapper.toResponse(reservation);
    }

    // ------------------------------------------------------------------ 4
    @Override
    public ReservationResponseDTO annulerReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        // --- RS-03 : un ADHERENT ne peut annuler que ses propres réservations ---
        verifierAppartenance(reservation);

        // --- RG-05 / RG-06 : seuls EN_ATTENTE et DISPONIBLE sont annulables ---
        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ReservationNonAnnulableException(reservation.getStatut());
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        return ReservationMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------ 5
    @Override
    public void supprimerReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
        reservationRepository.delete(reservation);
    }

    // ------------------------------------------------------------------
    // RS-03 : l'adhérent authentifié doit être le propriétaire de la
    // réservation. Le bibliothécaire (Admin) a accès à tout.
    // ------------------------------------------------------------------
    private void verifierAppartenance(Reservation reservation) {
        if (authenticatedUserService.hasRole("Admin")) {
            return;
        }
        Integer currentUserId = authenticatedUserService.getCurrentUserId();
        if (!reservation.getAdherent().getUserId().equals(currentUserId)) {
            throw new AccesRefuseException(
                    "Vous n'avez pas accès à cette réservation (RS-03)");
        }
    }
}

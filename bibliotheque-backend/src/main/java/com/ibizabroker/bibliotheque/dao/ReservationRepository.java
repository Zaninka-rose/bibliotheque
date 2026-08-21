package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * RG-02 : vérifier si l'adhérent a déjà une réservation active pour ce livre.
     */
    List<Reservation> findByAdherentUserIdAndLivreBookIdAndStatutIn(
            Integer adherentUserId, Integer livreBookId, Collection<StatutReservation> statuts);

    /**
     * RG-03 : compter les réservations actives d'un adhérent pour vérifier la limite.
     */
    long countByAdherentUserIdAndStatutIn(
            Integer adherentUserId, Collection<StatutReservation> statuts);

    /**
     * Bonus : trouver les réservations expirées pour expiration automatique.
     */
    List<Reservation> findByStatutAndDateExpirationBefore(
            StatutReservation statut, LocalDateTime dateExpiration);
}

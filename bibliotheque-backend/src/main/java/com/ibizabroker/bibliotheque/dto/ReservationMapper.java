package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.Reservation;

public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationResponseDTO toResponse(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setLivreId(Long.valueOf(reservation.getLivre().getBookId()));
        dto.setLivreTitre(reservation.getLivre().getBookName());
        dto.setAdherentId(Long.valueOf(reservation.getAdherent().getUserId()));
        dto.setAdherentNom(reservation.getAdherent().getName());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setDateExpiration(reservation.getDateExpiration());
        dto.setStatut(reservation.getStatut());
        return dto;
    }
}

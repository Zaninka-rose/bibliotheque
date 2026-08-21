package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;

import java.util.List;

public interface ReservationService {

    ReservationResponseDTO creerReservation(ReservationCreateDTO dto);

    List<ReservationResponseDTO> listerReservations(StatutReservation statutFiltre, Long adherentIdFiltre);

    ReservationResponseDTO consulterReservation(Long id);

    ReservationResponseDTO annulerReservation(Long id);

    void supprimerReservation(Long id);
}

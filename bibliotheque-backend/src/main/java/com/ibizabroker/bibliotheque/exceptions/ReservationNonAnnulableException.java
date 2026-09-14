package com.ibizabroker.bibliotheque.exceptions;

import com.ibizabroker.bibliotheque.entity.StatutReservation;

public class ReservationNonAnnulableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationNonAnnulableException(StatutReservation statutActuel) {
        super("RG-05/RG-06 : annulation refusée — la réservation est en statut \""
                + statutActuel + "\" et ne peut plus être annulée");
    }
}

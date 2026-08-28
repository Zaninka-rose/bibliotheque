export class Reservation {
    id: number;
    livreId: number;
    livreTitre: string;
    adherentId: number;
    adherentNom: string;
    dateReservation: string;
    dateExpiration: string;
    statut: string;
}

export class ReservationCreate {
    livreId: number;
    adherentId: number;
}

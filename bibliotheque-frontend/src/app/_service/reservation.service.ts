import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Reservation } from '../_model/reservation';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private baseURL = 'http://localhost:8080/api/reservations';

  constructor(private httpClient: HttpClient) { }

  getReservations(statut?: string): Observable<Reservation[]> {
    let url = this.baseURL;
    if (statut) {
      url += `?statut=${statut}`;
    }
    return this.httpClient.get<Reservation[]>(url)
      .pipe(catchError(this.handleError));
  }

  /**
   * Création par un BIBLIOTHECAIRE au nom d'un adhérent.
   * (L'identité du bibliothécaire vient du token ; l'adhérent visé
   * est passé dans le corps, ce qui n'est autorisé qu'au rôle Admin.)
   */
  createReservation(livreId: number, adherentId: number): Observable<Reservation> {
    return this.httpClient.post<Reservation>(this.baseURL, { livreId, adherentId })
      .pipe(catchError(this.handleError));
  }

  /**
   * Création par un ADHERENT pour lui-même : l'identité provient du
   * token (règle RS-04), le corps ne contient que le livre visé.
   */
  createForSelf(livreId: number): Observable<Reservation> {
    return this.httpClient.post<Reservation>(this.baseURL, { livreId })
      .pipe(catchError(this.handleError));
  }

  cancelReservation(id: number): Observable<Reservation> {
    return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {})
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    }
    return throwError(() => ({ status: error.status, message: errorMessage }));
  }
}

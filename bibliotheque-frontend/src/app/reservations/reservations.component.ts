import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Reservation } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';

@Component({
    selector: 'app-reservations',
    templateUrl: './reservations.component.html',
    styleUrls: ['./reservations.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationsComponent implements OnInit {

  reservations: Reservation[] = [];
  loading = false;
  error = '';
  cancelError = '';
  statutFilter = '';

  constructor(private reservationService: ReservationService) { }

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading = true;
    this.error = '';

    this.reservationService.getReservations(this.statutFilter || undefined).subscribe({
      next: (data) => {
        this.reservations = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  onFilterChange(statut: string): void {
    this.statutFilter = statut;
    this.loadReservations();
  }

  onCancel(id: number): void {
    if (!confirm('Voulez-vous vraiment annuler cette réservation ?')) return;

    this.cancelError = '';
    this.reservationService.cancelReservation(id).subscribe({
      next: () => {
        this.cancelError = '';
        this.loadReservations();
      },
      error: (err) => {
        this.cancelError = err.error?.message || err.message || 'Erreur lors de l annulation.';
      }
    });
  }

  onRetry(): void {
    this.loadReservations();
  }

  onCreated(): void {
    this.loadReservations();
  }
}

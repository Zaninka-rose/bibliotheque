import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { Reservation } from '../_model/reservation';

@Component({
    selector: 'app-reservation-list',
    templateUrl: './reservation-list.component.html',
    styleUrls: ['./reservation-list.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationListComponent {

  @Input() reservations: Reservation[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() cancelError = '';
  @Input() statutFilter = '';

  @Output() filterChange = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<number>();
  @Output() retry = new EventEmitter<void>();
  @Output() clearCancelError = new EventEmitter<void>();

  statuts = ['Tous', 'EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

  onFilterChange(statut: string) {
    this.filterChange.emit(statut === 'Tous' ? '' : statut);
  }

  onCancel(id: number) {
    this.cancel.emit(id);
  }

  onDismissCancelError() {
    this.clearCancelError.emit();
  }

  onRetry() {
    this.retry.emit();
  }

  canCancel(statut: string): boolean {
    return statut === 'EN_ATTENTE' || statut === 'DISPONIBLE';
  }
}

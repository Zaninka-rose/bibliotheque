import { Component, EventEmitter, Output, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { ReservationService } from '../_service/reservation.service';
import { ConnectivityService } from '../_service/connectivity.service';

@Component({
    selector: 'app-reservation-form',
    templateUrl: './reservation-form.component.html',
    styleUrls: ['./reservation-form.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationFormComponent implements OnInit {

  books: Books[] = [];
  users: Users[] = [];
  selectedLivreId: number | null = null;
  selectedAdherentId: number | null = null;
  loading = false;
  successMessage = '';
  errorMessage = '';

  readonly connectivity = inject(ConnectivityService);

  @Output() created = new EventEmitter<void>();

  constructor(
    private booksService: BooksService,
    private usersService: UsersService,
    private reservationService: ReservationService
  ) { }

  ngOnInit(): void {
    this.booksService.getBooksList().subscribe(data => this.books = data);
    this.usersService.getUsersList().subscribe(data => this.users = data);
  }

  get isFormValid(): boolean {
    return this.selectedLivreId !== null && this.selectedAdherentId !== null;
  }

  get submitDisabled(): boolean {
    // Dégradation gracieuse : action réseau impossible hors-ligne.
    return !this.isFormValid || this.loading || !this.connectivity.isOnline;
  }

  onSubmit(): void {
    if (this.submitDisabled) return;

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.reservationService.createReservation(this.selectedLivreId!, this.selectedAdherentId!).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Réservation créée avec succès.';
        this.selectedLivreId = null;
        this.selectedAdherentId = null;
        this.created.emit();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message;
      }
    });
  }
}

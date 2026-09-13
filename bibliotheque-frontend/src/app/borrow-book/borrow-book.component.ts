import { Component, OnInit, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BooksCacheService } from '../_service/books-cache.service';
import { BorrowService } from '../_service/borrow.service';
import { ReservationService } from '../_service/reservation.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ConnectivityService } from '../_service/connectivity.service';

/**
 * Espace adhérent : catalogue consultable (y compris hors-ligne via
 * le cache), emprunt des livres disponibles et réservation des livres
 * indisponibles. Les actions réseau sont désactivées hors connexion
 * avec un message explicite (dégradation gracieuse).
 */
@Component({
    selector: 'app-borrow-book',
    templateUrl: './borrow-book.component.html',
    styleUrls: ['./borrow-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class BorrowBookComponent implements OnInit {

  readonly search = signal('');
  readonly onlyAvailable = signal(false);
  readonly offlineData = signal(false);

  books: Books[] = [];
  loading = false;
  errorMessage = '';
  feedback = '';
  feedbackError = '';

  readonly connectivity = inject(ConnectivityService);
  private readonly booksService = inject(BooksService);
  private readonly cache = inject(BooksCacheService);
  private readonly borrowService = inject(BorrowService);
  private readonly reservationService = inject(ReservationService);
  private readonly userAuthService = inject(UserAuthService);

  userId = this.userAuthService.getUserId();

  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.books.filter(b => {
      const matchesQuery = !q
        || b.bookName?.toLowerCase().includes(q)
        || b.bookAuthor?.toLowerCase().includes(q)
        || b.bookGenre?.toLowerCase().includes(q);
      const matchesAvailability = !this.onlyAvailable() || b.noOfCopies > 0;
      return matchesQuery && matchesAvailability;
    });
  });

  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.loading = true;
    this.errorMessage = '';

    this.booksService.getBooksList().subscribe({
      next: (data) => {
        this.books = data;
        this.offlineData.set(false);
        this.cache.save(data);
        this.loading = false;
      },
      error: () => {
        const cached = this.cache.read();
        if (cached.length > 0) {
          this.books = cached;
          this.offlineData.set(true);
        } else {
          this.errorMessage = 'Impossible de charger le catalogue.';
        }
        this.loading = false;
      }
    });
  }

  onRetry(): void {
    this.getBooks();
  }

  borrowBook(book: Books) {
    if (!this.connectivity.isOnline) return;
    if (!confirm(`Emprunter « ${book.bookName} » ?`)) return;

    this.clearFeedback();
    const borrow = new Borrow();
    borrow.bookId = book.bookId;
    borrow.userId = this.userId;

    this.borrowService.borrowBook(borrow).subscribe({
      next: () => {
        this.feedback = `« ${book.bookName} » emprunté. Bonne lecture !`;
        this.getBooks();
      },
      error: () => {
        this.feedbackError = `Emprunt impossible pour « ${book.bookName} ».`;
      }
    });
  }

  reserveBook(book: Books) {
    if (!this.connectivity.isOnline) return;

    this.clearFeedback();
    this.reservationService.createForSelf(book.bookId).subscribe({
      next: () => {
        this.feedback = `« ${book.bookName} » réservé. Vous serez prévenu à son retour.`;
        this.getBooks();
      },
      error: (err) => {
        this.feedbackError = err?.message || `Réservation impossible pour « ${book.bookName} ».`;
      }
    });
  }

  private clearFeedback(): void {
    this.feedback = '';
    this.feedbackError = '';
  }
}

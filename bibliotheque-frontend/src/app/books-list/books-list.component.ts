import { Component, OnInit, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';
import { BooksCacheService } from '../_service/books-cache.service';
import { ConnectivityService } from '../_service/connectivity.service';

/**
 * Catalogue des livres (vue bibliothécaire) : recherche plein texte,
 * filtre disponibilité, table desktop / cartes mobiles. Dernière
 * liste connue servie hors-ligne via BooksCacheService.
 */
@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class BooksListComponent implements OnInit {
  readonly search = signal('');
  readonly onlyAvailable = signal(false);
  readonly offlineData = signal(false);

  books: Books[] = [];
  loading = false;
  errorMessage = '';

  private readonly booksService = inject(BooksService);
  private readonly cache = inject(BooksCacheService);
  readonly connectivity = inject(ConnectivityService);
  private readonly router = inject(Router);

  /** Résultat filtré par recherche + disponibilité. */
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

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId]);
  }

  deleteBook(book: Books) {
    if (!confirm(`Supprimer « ${book.bookName} » ? Cette action est définitive.`)) {
      return;
    }
    this.booksService.deleteBook(book.bookId).subscribe(() => this.getBooks());
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId]);
  }
}

import { Injectable } from '@angular/core';
import { Books } from '../_model/books';

/**
 * Cache local de la dernière liste de livres reçue de l'API.
 * Permet à l'application de rester consultable hors-ligne
 * (données éventuellement obsolètes, signalées à l'écran).
 */
@Injectable({ providedIn: 'root' })
export class BooksCacheService {
  private static readonly KEY = 'cache-books';

  save(books: Books[]): void {
    try {
      localStorage.setItem(BooksCacheService.KEY, JSON.stringify({ at: Date.now(), books }));
    } catch {
      // Quota dépassé ou storage indisponible : le cache est optionnel.
    }
  }

  read(): Books[] {
    try {
      const raw = localStorage.getItem(BooksCacheService.KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed?.books) ? parsed.books : [];
    } catch {
      return [];
    }
  }
}

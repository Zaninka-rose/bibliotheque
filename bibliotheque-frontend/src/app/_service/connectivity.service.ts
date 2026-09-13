import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * État de connectivité réseau de l'application.
 *
 * S'appuie sur les événements navigateur `online` / `offline` et
 * expose un flux réactif consommé par :
 *  - la bannière globale hors-ligne (app.component) ;
 *  - la désactivation gracieuse des actions réseau (boutons
 *    « Réserver », « Emprunter », etc.).
 */
@Injectable({ providedIn: 'root' })
export class ConnectivityService {
  private readonly onlineSubject = new BehaviorSubject<boolean>(navigator.onLine);

  /** true = en ligne, false = hors-ligne. */
  readonly online$ = this.onlineSubject.asObservable();

  get isOnline(): boolean {
    return this.onlineSubject.value;
  }

  constructor() {
    window.addEventListener('online', () => this.onlineSubject.next(true));
    window.addEventListener('offline', () => this.onlineSubject.next(false));
  }
}

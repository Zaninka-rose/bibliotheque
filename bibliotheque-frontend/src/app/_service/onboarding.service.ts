import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Parcours de bienvenue (onboarding) après la première connexion.
 *
 * 2 à 4 étapes selon le rôle, affiché UNE SEULE FOIS par utilisateur :
 * la complétion est mémorisée en localStorage sous la clé
 * `onboarding-<userId>`. L'utilisateur peut passer l'onboarding
 * (« Passer ») à tout moment.
 */
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private static readonly KEY_PREFIX = 'onboarding-';

  private readonly visibleSubject = new BehaviorSubject<boolean>(false);
  /** true quand l'onboarding doit être affiché à l'écran. */
  readonly visible$ = this.visibleSubject.asObservable();

  /** Démarre l'onboarding si l'utilisateur ne l'a jamais vu. */
  maybeStartFor(userId: number | null): void {
    if (userId == null) {
      return;
    }
    const done = localStorage.getItem(OnboardingService.KEY_PREFIX + userId);
    if (!done) {
      this.visibleSubject.next(true);
    }
  }

  /** Marque l'onboarding comme vu et le ferme. */
  completeFor(userId: number | null): void {
    if (userId != null) {
      localStorage.setItem(OnboardingService.KEY_PREFIX + userId, 'done');
    }
    this.visibleSubject.next(false);
  }

  get isVisible(): boolean {
    return this.visibleSubject.value;
  }
}

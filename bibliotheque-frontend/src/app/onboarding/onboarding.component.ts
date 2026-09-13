import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { OnboardingService } from '../_service/onboarding.service';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { TPipe } from '../_service/i18n/t.pipe';

interface OnboardingStep {
  icon: string;
  title: string;
  text: string;
}

/**
 * Parcours de bienvenue (2 à 4 étapes selon le rôle), affiché une
 * seule fois par utilisateur (mémorisation en localStorage, voir
 * OnboardingService). Overlay global piloté par OnboardingService,
 * placé dans app.component.html.
 */
@Component({
  selector: 'app-onboarding',
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class OnboardingComponent {
  private readonly onboarding = inject(OnboardingService);
  private readonly auth = inject(UserAuthService);
  private readonly usersService = inject(UsersService);

  readonly visible$ = this.onboarding.visible$;

  step = 0;

  private readonly adherentSteps: OnboardingStep[] = [
    {
      icon: '📚',
      title: 'Bienvenue à la bibliothèque !',
      text: 'Parcourez le catalogue, suivez vos emprunts et réservez les livres indisponibles en quelques clics.'
    },
    {
      icon: '🔍',
      title: 'Trouvez un livre',
      text: 'Cherchez par titre, auteur ou genre, et vérifiez sa disponibilité en un coup d\'œil.'
    },
    {
      icon: '⏳',
      title: 'Réservez et suivez',
      text: 'Réservez un livre emprunté : vous êtes prévenu dès qu\'il revient. Suivez vos réservations depuis « Emprunter ».'
    }
  ];

  private readonly bibliothecaireSteps: OnboardingStep[] = [
    {
      icon: '🏛️',
      title: 'Bienvenue, bibliothécaire !',
      text: 'Gérez le catalogue, les utilisateurs et toutes les réservations depuis votre espace.'
    },
    {
      icon: '📋',
      title: 'Gérez les réservations',
      text: 'Consultez toutes les demandes, honorez, annulez ou supprimez une réservation en un clic.'
    },
    {
      icon: '📖',
      title: 'Livres et emprunts',
      text: 'Ajoutez des livres, suivez les retours et gardez le catalogue à jour.'
    }
  ];

  /** Étapes du rôle courant (Admin = bibliothécaire). */
  get steps(): OnboardingStep[] {
    return this.usersService.roleMatch(['Admin']) ? this.bibliothecaireSteps : this.adherentSteps;
  }

  get current(): OnboardingStep {
    return this.steps[this.step];
  }

  get isLast(): boolean {
    return this.step === this.steps.length - 1;
  }

  next(): void {
    if (!this.isLast) {
      this.step++;
    } else {
      this.finish();
    }
  }

  back(): void {
    if (this.step > 0) {
      this.step--;
    }
  }

  finish(): void {
    this.onboarding.completeFor(this.auth.getUserId());
    this.step = 0;
  }
}

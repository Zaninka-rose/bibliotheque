import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../_service/i18n/t.pipe';

/**
 * Écran d'accueil (splash / landing) présenté avant la connexion :
 * nom, logo, accroche, points clés et accès « Se connecter » /
 * « Créer un compte ».
 */
@Component({
  selector: 'app-home',
  imports: [RouterLink, TPipe],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true
})
export class HomeComponent {
  readonly features = [
    { icon: '📚', title: 'Catalogue vivant', text: 'Parcourez les livres de la bibliothèque et leur disponibilité en temps réel.' },
    { icon: '⏳', title: 'Réservations simples', text: 'Réservez un livre indisponible, il vous est attribué dès son retour.' },
    { icon: '🔔', title: 'Suivi clair', text: 'Emprunts en cours, dates de retour et réservations toujours sous les yeux.' }
  ];
}

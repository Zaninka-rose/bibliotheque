import { Component, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RegisterService } from '../_service/register.service';

/**
 * Création de compte public : crée toujours un compte ADHERENT
 * (le rôle est forcé côté serveur). Le bibliothécaire garde la
 * gestion des rôles via « Inscrire un utilisateur ».
 */
@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true
})
export class RegisterComponent {
  name = '';
  username = '';
  password = '';
  loading = false;
  errorMessage = '';
  success = false;

  constructor(
    private registerService: RegisterService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (!this.name.trim() || !this.username.trim() || !this.password) {
      this.errorMessage = 'Merci de remplir tous les champs.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.registerService.register({
      name: this.name.trim(),
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Inscription impossible pour le moment. Réessayez plus tard.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}

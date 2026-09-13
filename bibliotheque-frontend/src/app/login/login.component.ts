import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { OnboardingService } from '../_service/onboarding.service';
import { TPipe } from '../_service/i18n/t.pipe';

@Component({
  selector: 'app-login',
  imports: [RouterLink, TPipe, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true
})
export class LoginComponent implements OnInit {
  loading = false;
  loginError = '';

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private router: Router,
    private onboarding: OnboardingService
  ) { }

  ngOnInit() {
  }

  login(loginForm: NgForm) {
    if (loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.loginError = '';

    this.userService.login(loginForm.value).subscribe(
      (response: any) => {
        this.userAuthSerivce.setRoles(response.user.role);
        this.userAuthSerivce.setToken(response.jwtToken);
        this.userAuthSerivce.setUserId(response.user.userId);
        this.userAuthSerivce.setName(response.user.name);

        const role = response.user.role[0].roleName;
        this.loading = false;

        // Onboarding uniquement à la première connexion (mémorisé).
        this.onboarding.maybeStartFor(response.user.userId);

        // Le tableau de bord adapte son contenu au rôle.
        this.router.navigate(['/dashboard']);
      },
      (error) => {
        this.loading = false;
        this.loginError = error?.status === 401 || error?.status === 403
          ? 'Identifiants incorrects. Vérifiez votre nom d\'utilisateur et votre mot de passe.'
          : 'Connexion impossible pour le moment. Réessayez plus tard.';
      }
    );
  }
}

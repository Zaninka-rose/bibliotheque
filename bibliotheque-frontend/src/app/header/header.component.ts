import { Component, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { ThemeService } from '../_service/theme.service';
import { TranslationService, Lang } from '../_service/i18n/translation.service';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class HeaderComponent implements OnInit {

  /** État du menu hamburger mobile (sans dépendance au JS Bootstrap). */
  menuOpen = false;

  constructor(
    private userAuthService: UserAuthService,
    private router: Router,
    public userService: UsersService,
  ) { }

  /** Services exposés au template pour les bascules thème / langue. */
  readonly themeService = inject(ThemeService);
  readonly i18n = inject(TranslationService);

  name = this.userAuthService.getName();

  ngOnInit(): void {
  }

  public isLoggedIn() {
    return this.userAuthService.isLoggedIn();
  }

  public logout() {
    this.userAuthService.clear();
    this.menuOpen = false;
    this.router.navigate(['/']);
  }

  public toggleTheme() {
    this.themeService.toggle();
  }

  public setLang(lang: Lang) {
    this.i18n.setLang(lang);
  }

  public toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  /** Ferme le menu mobile après un clic sur un lien. */
  public closeMenu() {
    this.menuOpen = false;
  }
}

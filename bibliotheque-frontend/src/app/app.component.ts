import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ConnectivityService } from './_service/connectivity.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class AppComponent {
  private readonly router = inject(Router);
  readonly connectivity = inject(ConnectivityService);

  /** Pages sans chrome (header/footer) : splash et authentification. */
  private static readonly BARE_PAGES = ['/', '/login', '/register', '/forbidden'];

  /** true sur les pages sans header/footer (splash, auth). */
  get isBarePage(): boolean {
    return AppComponent.BARE_PAGES.includes(this.router.url);
  }
}

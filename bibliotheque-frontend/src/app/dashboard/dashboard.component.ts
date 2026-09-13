import { Component, OnInit, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BooksService } from '../_service/books.service';
import { BooksCacheService } from '../_service/books-cache.service';
import { ReservationService } from '../_service/reservation.service';
import { BorrowService } from '../_service/borrow.service';
import { UsersService } from '../_service/users.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ConnectivityService } from '../_service/connectivity.service';
import { Books } from '../_model/books';
import { Reservation } from '../_model/reservation';
import { Borrow } from '../_model/borrow';

interface ActivityItem {
  icon: string;
  label: string;
  detail: string;
  when: string | Date | null;
}

/**
 * Tableau de bord post-connexion, adapté au rôle :
 *  - BIBLIOTHÉCAIRE (Admin) : vue d'ensemble du fonds, réservations à
 *    traiter, emprunts en cours, activité récente, table des statuts ;
 *  - ADHÉRENT : ses emprunts en cours, ses réservations actives,
 *    relance des dates de retour, accès rapides.
 *
 * Données issues des endpoints existants ; dernière liste connue
 * servie hors-ligne via les caches (books / reservations).
 */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true
})
export class DashboardComponent implements OnInit {
  readonly isAdmin = inject(UsersService).roleMatch(['Admin']);
  readonly connectivity = inject(ConnectivityService);

  readonly displayName = inject(UserAuthService).getName() ?? '';

  books: Books[] = [];
  reservations: Reservation[] = [];
  borrows: Borrow[] = [];
  loading = true;
  offlineData = false;
  errorMessage = '';

  /** Réservations listées dans la table de statuts (8 max, style mockup). */
  readonly reservationsTable = computed(() => this.reservations.slice(0, 8));

  /** Répartition des statuts de réservation pour le donut SVG. */
  readonly statutStats = computed(() => {
    const counts = new Map<string, number>();
    for (const r of this.reservations) {
      counts.set(r.statut, (counts.get(r.statut) ?? 0) + 1);
    }
    const palette: Record<string, string> = {
      'EN_ATTENTE': 'var(--warning)',
      'DISPONIBLE': 'var(--info)',
      'HONOREE': 'var(--success)',
      'ANNULEE': 'var(--text-muted)',
      'EXPIREE': 'var(--danger)'
    };
    const total = [...counts.values()].reduce((a, b) => a + b, 0);
    let offset = 0;
    const segments = [...counts.entries()].map(([statut, value]) => {
      const fraction = total > 0 ? value / total : 0;
      const seg = { statut, value, fraction, offset, color: palette[statut] ?? 'var(--primary)' };
      offset += fraction;
      return seg;
    });
    return { segments, total };
  });

  /** Emprunts actuellement en cours (pas encore retournés). */
  readonly activeBorrows = computed(() =>
    (this.borrows ?? []).filter(b => !b.returnDate)
  );

  /** Emprunts en retard (date limite dépassée, non retournés). */
  readonly lateBorrows = computed(() => {
    const now = Date.now();
    return this.activeBorrows().filter(b => b.dueDate && new Date(b.dueDate).getTime() < now);
  });

  /** Réservations actives (en attente ou disponibles). */
  readonly activeReservations = computed(() =>
    this.reservations.filter(r => r.statut === 'EN_ATTENTE' || r.statut === 'DISPONIBLE')
  );

  /** Pour un adhérent : réservations qui le concernent (RS-05). */
  readonly myReservations = computed(() => this.reservations);

  /** Livres indisponibles (utiles pour l'accroche "à réserver"). */
  readonly unavailableBooks = computed(() => this.books.filter(b => !b.noOfCopies || b.noOfCopies < 1));

  /** KPI principal (côté mockup : valeur mise en avant). */
  readonly heroKpi = computed(() => {
    if (this.isAdmin) {
      return {
        label: 'Exemplaires au catalogue',
        value: this.books.reduce((sum, b) => sum + (b.noOfCopies ?? 0), 0),
        sub: `${this.books.length} titres référencés`,
        icon: '📚'
      };
    }
    return {
      label: 'Mes emprunts en cours',
      value: this.activeBorrows().length,
      sub: `${this.activeReservations().length} réservation(s) active(s)`,
      icon: '📖'
    };
  });

  /** Fil d'activité récent (réservations + emprunts, triés par date). */
  readonly activity = computed<ActivityItem[]>(() => {
    const items: ActivityItem[] = [];
    for (const r of this.reservations.slice(0, 6)) {
      items.push({
        icon: '⏳',
        label: this.isAdmin
          ? `Réservation de ${r.adherentNom}`
          : `Votre réservation « ${r.livreTitre} »`,
        detail: `Statut : ${r.statut}`,
        when: r.dateReservation
      });
    }
    for (const b of this.activeBorrows().slice(0, 6)) {
      items.push({
        icon: '📕',
        label: this.isAdmin ? `Emprunt #${b.borrowId} en cours` : `Emprunt #${b.borrowId} en cours`,
        detail: `Retour attendu le ${b.dueDate ? new Date(b.dueDate).toLocaleDateString('fr-FR') : '—'}`,
        when: b.issueDate
      });
    }
    return items
      .sort((a, b) => new Date(b.when ?? 0).getTime() - new Date(a.when ?? 0).getTime())
      .slice(0, 6);
  });

  private readonly booksService = inject(BooksService);
  private readonly booksCache = inject(BooksCacheService);
  private readonly reservationService = inject(ReservationService);
  private readonly borrowService = inject(BorrowService);
  private readonly auth = inject(UserAuthService);

  /** Sparkline : points de la courbe (réservations par jour de semaine). */
  sparklinePoints(): string {
    return this.sparkData().map(p => `${p.x},${p.y}`).join(' ');
  }

  /** Sparkline : aire sous la courbe (fermée vers le bas). */
  sparkAreaPoints(): string {
    const pts = this.sparkData();
    return `${pts.map(p => `${p.x},${p.y}`).join(' ')} 100,32 0,32`;
  }

  private sparkData(): { x: number; y: number }[] {
    const counts = [0, 0, 0, 0, 0, 0, 0];
    for (const r of this.reservations) {
      const day = new Date(r.dateReservation ?? Date.now()).getDay();
      counts[day]++;
    }
    const max = Math.max(1, ...counts);
    return counts.map((c, i) => ({ x: (i / (counts.length - 1)) * 100, y: 30 - (c / max) * 26 }));
  }

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.errorMessage = '';
    let pending = 3;

    const done = () => {
      pending--;
      if (pending === 0) {
        this.loading = false;
      }
    };

    this.booksService.getBooksList().subscribe({
      next: (data) => { this.books = data; this.booksCache.save(data); this.offlineData = false; done(); },
      error: () => {
        const cached = this.booksCache.read();
        if (cached.length) { this.books = cached; this.offlineData = true; }
        done();
      }
    });

    this.reservationService.getReservations().subscribe({
      next: (data) => { this.reservations = data; done(); },
      error: () => { done(); }
    });

    if (this.isAdmin) {
      this.borrowService.getBorrowList().subscribe({
        next: (data) => { this.borrows = data; done(); },
        error: () => { done(); }
      });
    } else {
      this.borrowService.getBooksBorrowedByUser(this.auth.getUserId()).subscribe({
        next: (data) => { this.borrows = data; done(); },
        error: () => { done(); }
      });
    }
  }

  retry(): void {
    this.loadAll();
  }

  badgeClass(statut: string): string {
    return `statut-badge statut-${statut}`;
  }
}

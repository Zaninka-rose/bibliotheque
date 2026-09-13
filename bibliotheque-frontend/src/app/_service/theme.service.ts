import { Injectable } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_KEY = 'theme';

/**
 * Gère le thème clair/sombre de l'application.
 *
 * Utilise l'attribut natif Bootstrap 5.3 `data-bs-theme` posé sur
 * `<html>` : Bootstrap adapte alors ses composants automatiquement,
 * et styles.css adapte les variables CSS personnalisées du projet.
 * Le choix est persisté dans localStorage et appliqué dès le démarrage.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private current: Theme = 'light';

  constructor() {
    this.current = this.readStored();
    this.apply();
  }

  get theme(): Theme {
    return this.current;
  }

  get isDark(): boolean {
    return this.current === 'dark';
  }

  toggle(): void {
    this.current = this.isDark ? 'light' : 'dark';
    this.store(this.current);
    this.apply();
  }

  set(theme: Theme): void {
    this.current = theme;
    this.store(theme);
    this.apply();
  }

  private readStored(): Theme {
    const stored = localStorage.getItem(THEME_KEY);
    return stored === 'dark' ? 'dark' : 'light';
  }

  private store(theme: Theme): void {
    localStorage.setItem(THEME_KEY, theme);
  }

  private apply(): void {
    document.documentElement.setAttribute('data-bs-theme', this.current);
    document.documentElement.style.colorScheme = this.current;
  }
}

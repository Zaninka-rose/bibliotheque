import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  it('defaults to light theme when nothing is stored', () => {
    expect(service.theme).toBe('light');
    expect(service.isDark).toBe(false);
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('light');
  });

  it('toggle switches light to dark and updates data-bs-theme', () => {
    service.toggle();
    expect(service.isDark).toBe(true);
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('dark');
  });

  it('toggle persists the choice in localStorage', () => {
    service.toggle();
    expect(localStorage.getItem('theme')).toBe('dark');
    service.toggle();
    expect(localStorage.getItem('theme')).toBe('light');
  });

  it('restores the stored dark theme at startup', () => {
    localStorage.setItem('theme', 'dark');
    const reloaded = TestBed.inject(ThemeService);
    // New instance = nouveau "démarrage" : le thème sombre doit être réappliqué
    const fresh = new ThemeService();
    expect(fresh.theme).toBe('dark');
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('dark');
  });

  it('set() applies an explicit theme', () => {
    service.set('dark');
    expect(service.theme).toBe('dark');
    service.set('light');
    expect(service.theme).toBe('light');
  });
});

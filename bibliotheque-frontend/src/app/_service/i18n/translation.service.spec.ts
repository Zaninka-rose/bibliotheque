import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TranslationService);
  });

  it('defaults to French', () => {
    expect(service.lang).toBe('fr');
  });

  it('translates a key into French', () => {
    expect(service.translate('Welcome')).toBe('Bienvenue');
  });

  it('returns the key unchanged in English (reference language)', () => {
    service.setLang('en');
    expect(service.translate('Welcome')).toBe('Welcome');
  });

  it('returns an unknown key as-is (graceful fallback)', () => {
    expect(service.translate('Clé.Inexistante')).toBe('Clé.Inexistante');
  });

  it('setLang persists the choice and notifies subscribers', () => {
    const seen: string[] = [];
    service.lang$.subscribe(lang => seen.push(lang));

    service.setLang('en');
    expect(localStorage.getItem('lang')).toBe('en');
    expect(service.lang).toBe('en');
    expect(seen).toEqual(['fr', 'en']);
  });

  it('ignores an unsupported language', () => {
    service.setLang('de' as any);
    expect(service.lang).toBe('fr');
    expect(localStorage.getItem('lang')).toBeNull();
  });

  it('restores the stored language at startup', () => {
    localStorage.setItem('lang', 'en');
    const fresh = new TranslationService();
    expect(fresh.lang).toBe('en');
    expect(fresh.translate('Welcome')).toBe('Welcome');
  });
});

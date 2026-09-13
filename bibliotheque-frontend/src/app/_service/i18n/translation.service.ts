import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { FR } from './fr';
import { EN } from './en';

export type Lang = 'fr' | 'en';

const LANG_KEY = 'lang';

/**
 * Traduction à la volée des libellés de l'interface.
 *
 * Principe : les clés sont les phrases de référence en anglais. En anglais
 * la clé s'affiche telle quelle ; en français elle est remplacée par la
 * traduction du dictionnaire FR. Une clé absente est affichée telle quelle,
 * ce qui permet d'ajouter progressivement des libellés sans rien casser.
 *
 * Le choix de langue est persisté dans localStorage. Les composants
 * s'abonnent à `lang$` pour se rafraîchir au changement de langue.
 */
@Injectable({ providedIn: 'root' })
export class TranslationService {
  private dictionaries: Record<Lang, Record<string, string>> = { fr: FR, en: EN };

  private current: Lang = 'fr';

  private readonly langSubject = new BehaviorSubject<Lang>(this.readStored());
  /** Flux du langage courant — les composants s'y abonnent pour se rafraîchir. */
  readonly lang$ = this.langSubject.asObservable();

  get lang(): Lang {
    return this.langSubject.value;
  }

  setLang(lang: Lang): void {
    if (lang !== 'fr' && lang !== 'en') {
      return;
    }
    localStorage.setItem(LANG_KEY, lang);
    this.langSubject.next(lang);
  }

  toggle(): void {
    this.setLang(this.lang === 'fr' ? 'en' : 'fr');
  }

  /** Traduit une clé ; renvoie la clé telle quelle si absente du dictionnaire. */
  translate(key: string): string {
    return this.dictionaries[this.langSubject.value][key] ?? key;
  }

  private readStored(): Lang {
    const stored = localStorage.getItem(LANG_KEY);
    return stored === 'en' ? 'en' : 'fr';
  }
}

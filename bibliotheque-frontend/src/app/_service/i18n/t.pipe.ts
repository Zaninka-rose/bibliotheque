import { Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from './translation.service';

/**
 * Pipe de traduction : {{ 'Welcome' | t }} → « Bienvenue ».
 *
 * Pipe pur avec référence au service réactif : le résultat est recalculé
 * à chaque émission de `lang$`, donc toute l'interface bascule de langue
 * instantanément, sans rechargement.
 *
 * Usage avec paramètre optionnel : {{ 'Welcome' | t:user }} → « Bienvenue Rose ».
 */
@Pipe({
  name: 't',
  standalone: true,
  pure: false
})
export class TPipe implements PipeTransform {
  constructor(private i18n: TranslationService) {}

  transform(key: string, ...args: unknown[]): string {
    const translated = this.i18n.translate(key);
    if (args.length === 0) {
      return translated;
    }
    // Remplace {0}, {1}, ... par les arguments fournis
    return args.reduce<string>(
      (acc, arg, i) => acc.replace(`{${i}}`, String(arg)),
      translated
    );
  }
}

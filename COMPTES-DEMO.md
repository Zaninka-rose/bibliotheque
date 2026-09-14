# Comptes de démonstration

Identifiants pour l'épreuve d'autorisation sur les réservations.
Ces comptes sont créés automatiquement au démarrage du backend
(`data.sql` + `DataInitializer`) et chaque adhérent possède déjà
au moins une réservation `EN_ATTENTE` à son nom.

| Rôle           | Compte  | Mot de passe | Réservations de départ |
|----------------|---------|--------------|------------------------|
| BIBLIOTHECAIRE | zaninka | zaninka123   | — (voit toutes les réservations) |
| ADHERENT       | rose    | rose123      | L6 (`EN_ATTENTE`) |
| ADHERENT       | rose2   | rose2123     | L7 (`EN_ATTENTE`) |

Comptes techniques conservés pour les tests (mot de passe `admin123`) :
`admin` (Admin), `a1_reservataire`, `a2_quota`, `a3_emprunteur` (Adherent).

## Utilisation

```bash
# 1. Connexion (renvoie un token JWT)
curl -s -X POST http://localhost:8080/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"zaninka","password":"zaninka123"}'

# 2. Appeler l'API avec le token
TOKEN=... # jeton reçu ci-dessus
curl -s http://localhost:8080/api/reservations -H "Authorization: Bearer $TOKEN"
```

Matrice attendue (RS-01 → RS-05) :

| Endpoint                             | Anonyme | ADHERENT                  | BIBLIOTHECAIRE |
|--------------------------------------|---------|---------------------------|----------------|
| `POST /api/reservations`             | 401     | OUI pour lui-même         | OUI pour tous  |
| `GET /api/reservations`              | 401     | ses réservations seules   | toutes         |
| `GET /api/reservations/{id}`         | 401     | si elle lui appartient    | toutes         |
| `PATCH /api/reservations/{id}/annuler` | 401   | si elle lui appartient    | toutes         |
| `DELETE /api/reservations/{id}`      | 401     | NON (403)                 | OUI            |

## Refus à provoquer en direct (épreuve)

- **RG-01 (409)** : zaninka ou rose réserve le livre **L1** (disponible) → refus.
- **RG-02 (409)** : rose réserve à nouveau **L6** (déjà réservée par elle) → refus.
- **RG-03 (409)** : rose crée des réservations sur L8, L2, L3 (→ 3 actives avec L6),
  puis une 4e → refus « limite de 3 réservations actives ».
- **RS-02 (403)** : rose tente `DELETE /api/reservations/{id}` → refus.
- **RS-03 (403)** : rose consulte la réservation de rose2 → refus.

## Tests automatisés (à lancer devant le formateur)

```bash
cd bibliotheque-backend && ./mvnw test
```

- `ReservationLimiteActiveTest` — RG-03 en unitaire (repository mocké, sans base) :
  2 actives → succès, 3 actives → refus (+ variantes Admin).
- `ReservationEndpointSecurityIntegrationTest` — GET /api/reservations :
  sans token → 401, token ADHERENT → 200, réservation d'autrui → 403
  (+ RG-03 via l'endpoint pour un adhérent saturé → 409).

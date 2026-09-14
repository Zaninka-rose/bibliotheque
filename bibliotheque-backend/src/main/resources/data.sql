-- ============================================================
-- data.sql — Jeu de données de démonstration (épreuve : autorisations)
-- Exécuté APRÈS la création des tables par Hibernate
-- (spring.jpa.defer-datasource-initialization=true)
-- Idempotent : sûr au redémarrage et sur un volume Docker neuf.
--
-- Comptes de démonstration (identifiants dans COMPTES-DEMO.md) :
--   zaninka / zaninka123         (rôle Admin → BIBLIOTHECAIRE)
--   rose / rose123               (rôle Adherent → ADHERENT)
--   rose2 / rose2123             (rôle Adherent → ADHERENT)
--
-- Comptes techniques conservés pour les tests (mdp : admin123) :
--   admin (Admin), a1_reservataire, a2_quota, a3_emprunteur (Adherent)
--
-- Chaque adhérent de démo (rose, rose2) possède au moins une réservation
-- EN_ATTENTE à son nom, sur un livre qui lui est dédié (L6, L7).
--
-- Matrice de sécurité à démontrer (RS-01 → RS-05) :
-- | Endpoint                          | Anonyme | ADHERENT                  | BIBLIOTHECAIRE |
-- |-----------------------------------|---------|---------------------------|----------------|
-- | POST /api/reservations            | 401     | OUI pour lui-même         | OUI pour tous  |
-- | GET /api/reservations             | 401     | ses réservations seules   | toutes         |
-- | GET /api/reservations/{id}        | 401     | si elle lui appartient    | toutes         |
-- | PATCH /api/reservations/{id}/annuler | 401  | si elle lui appartient    | toutes         |
-- | DELETE /api/reservations/{id}     | 401     | NON (403)                 | OUI            |
-- ============================================================

-- ------------------------------------------------------------
-- 1. Rôles
--    role_id=1 'Admin' (BIBLIOTHECAIRE) et role_id=2 'Adherent'
--    (ADHERENT). Le DataInitializer réutilise 'Admin' s'il existe
--    déjà (findByRoleName) au lieu d'en créer un doublon.
-- ------------------------------------------------------------
INSERT INTO role (role_id, role_name) VALUES (1, 'Admin')
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO role (role_id, role_name) VALUES (2, 'Adherent')
ON CONFLICT (role_id) DO NOTHING;

-- Sécurité anti-doublon : si un rôle a déjà été créé par le
-- DataInitializer avec un autre role_id, on garde le plus ancien
-- (le rôle est recherché par NOM partout) et on supprime les doublons
-- SANS violer la FK de user_role :
-- Étape 1 : re-rattacher les utilisateurs du rôle dupliqué au rôle
-- canonique (le plus ancien) — ON CONFLICT gère ceux qui l'ont déjà.
INSERT INTO user_role (user_id, role_id)
SELECT ur.user_id, MIN(keep.role_id)
FROM user_role ur
JOIN role dup ON dup.role_id = ur.role_id
JOIN role keep ON keep.role_name = dup.role_name
GROUP BY ur.user_id, dup.role_name
ON CONFLICT DO NOTHING;

-- Étape 2 : détacher les utilisateurs des rôles dupliqués — sinon le
-- DELETE ci-dessous viole la FK de user_role.
DELETE FROM user_role ur
USING role dup
WHERE ur.role_id = dup.role_id
  AND dup.role_id > (SELECT MIN(keep.role_id) FROM role keep WHERE keep.role_name = dup.role_name);

-- Étape 3 : suppression des doublons désormais orphelins.
DELETE FROM role a
USING role b
WHERE a.role_name = b.role_name
  AND a.role_id > b.role_id;

-- ------------------------------------------------------------
-- 2. Livres L1 à L8
--    L1           : disponible (1 copie) → sert à provoquer le 409 RG-01
--    L2, L3, L4, L5 : indisponibles (empruntés par a3_emprunteur)
--    L6, L7       : indisponibles, réservés respectivement par rose et rose2
--    L8           : indisponible et sans réservation → créations libres
-- ------------------------------------------------------------
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (1, 'L1', 'Antoine de Saint-Exupéry', 'Conte',           1),
    (2, 'L2', 'Frank Herbert',            'Science-Fiction', 0),
    (3, 'L3', 'Ray Bradbury',             'Science-Fiction', 0),
    (4, 'L4', 'Alexandre Dumas',          'Aventure',        0),
    (5, 'L5', 'Victor Hugo',              'Classique',       0),
    (6, 'L6', 'Ursula K. Le Guin',        'Science-Fiction', 0),
    (7, 'L7', 'Jules Verne',              'Aventure',        0),
    (8, 'L8', 'Albert Camus',             'Roman',           0)
ON CONFLICT (book_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. Comptes de démonstration
--    zaninka (51)            : le BIBLIOTHECAIRE de l'épreuve.
--    rose (52) et rose2 (53) : les 2 ADHERENT de l'épreuve.
--    user_id >= 50 pour ne jamais entrer en collision avec les
--    comptes techniques (1, 2, 3, 4).
-- ------------------------------------------------------------
INSERT INTO users (user_id, username, name, password) VALUES
    (51, 'zaninka', 'Zaninka',     '$2b$10$Gbm5GuGsnpdBDh7xBLuq2e31hGE04OAP8ITPsRlnMimgAmD7K75ou'),
    (52, 'rose',    'Rose',        '$2b$10$hLjP0nmaBvVpKH4.QVTrZuJQv/ysO13Cbcttt1klsfWvEGnFG9UD.'),
    (53, 'rose2',   'Rose Deux',   '$2b$10$shqu25gHfAMjPjuQ3hC3Ku1tOLXZESbXuL.zvJiS3TwLvxLdjacWS')
ON CONFLICT (user_id) DO UPDATE
SET username = EXCLUDED.username, name = EXCLUDED.name, password = EXCLUDED.password;

-- Comptes techniques (tests, doc README) — inchangés
INSERT INTO users (user_id, username, name, password) VALUES
    (2, 'a1_reservataire', 'Adherent A1 - Reservataire principal', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (3, 'a2_quota',        'Adherent A2 - Sature son quota',       '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (4, 'a3_emprunteur',   'Adherent A3 - Emprunteur',             '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. Attribution des rôles
--    user_id 1 = admin (créé ensuite par le DataInitializer, dont
--    le rôle est réutilisé — pas d'insert ici pour 1).
-- ------------------------------------------------------------
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name = 'Admin'
WHERE u.user_id = 51
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name = 'Adherent'
WHERE u.user_id IN (2, 3, 4, 52, 53)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 5. Emprunts en cours de a3_emprunteur sur L2, L3, L4, L5
--    (justifient l'indisponibilité de ces livres)
-- ------------------------------------------------------------
INSERT INTO borrow (borrow_id, book_id, user_id, issue_date, due_date) VALUES
    (1, 2, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (2, 3, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (3, 4, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (4, 5, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days')
ON CONFLICT (borrow_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. Réservations de démonstration
--    rose → L6, rose2 → L7 (livres dédiés, jamais en conflit).
--    Garde : on n'insère que si l'adhérent n'a AUCUNE réservation
--    active — l'insert ne s'exécute donc qu'une seule fois, et le
--    livre cible est replié sur son second choix si le premier est
--    déjà réservé par quelqu'un d'autre (rose : L6 puis L7 ;
--    rose2 : L7 puis L3).
--    Chaque INSERT crée UNE réservation maximum (LIMIT 1).
-- ------------------------------------------------------------
-- rose
INSERT INTO reservation (livre_id, adherent_id, statut, date_reservation, date_expiration)
SELECT c.book_id, u.user_id, 'EN_ATTENTE', now(), now() + INTERVAL '7 days'
FROM (VALUES (6, 1), (7, 2)) AS c(book_id, prio)
JOIN users u ON u.username = 'rose'
WHERE NOT EXISTS (
    SELECT 1 FROM reservation r
    WHERE r.adherent_id = u.user_id
      AND r.statut IN ('EN_ATTENTE', 'DISPONIBLE')
)
ORDER BY c.prio
LIMIT 1;

-- rose2
INSERT INTO reservation (livre_id, adherent_id, statut, date_reservation, date_expiration)
SELECT c.book_id, u.user_id, 'EN_ATTENTE', now(), now() + INTERVAL '7 days'
FROM (VALUES (7, 1), (3, 2)) AS c(book_id, prio)
JOIN users u ON u.username = 'rose2'
WHERE NOT EXISTS (
    SELECT 1 FROM reservation r
    WHERE r.adherent_id = u.user_id
      AND r.statut IN ('EN_ATTENTE', 'DISPONIBLE')
)
ORDER BY c.prio
LIMIT 1;

-- ------------------------------------------------------------
-- 7. Avance des séquences au-delà des ids posés à la main
--    (les prochaines créations depuis l'application n'entrent
--    jamais en collision).
-- ------------------------------------------------------------
SELECT setval('users_seq',  GREATEST(COALESCE((SELECT MAX(user_id)  FROM users), 0), 100));
SELECT setval('books_seq',  GREATEST(COALESCE((SELECT MAX(book_id)  FROM books), 0), 100));

-- ============================================================
-- init-scripts/03-reservation-testdata.sql
-- Jeu de données de test pour le module de réservation
-- Exécuté automatiquement par le conteneur postgres au premier
-- démarrage (docker-entrypoint-initdb.d)
--
-- Scénario couvert :
--   L1          : livre disponible (aucun emprunt en cours)
--   L2, L3, L4, L5 : livres tous empruntés et non rendus
--   A1  : adhérent réservataire principal
--   A2  : adhérent qui saturera son quota (RG-03, max 3 réservations actives)
--   A3  : adhérent emprunteur, détient L2 à L5
-- ============================================================

-- ------------------------------------------------------------
-- 1. Livres L1 à L5 (les seuls livres du jeu de données)
-- ------------------------------------------------------------
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (1, 'L1', 'Antoine de Saint-Exupéry', 'Conte',           1),
    (2, 'L2', 'Frank Herbert',            'Science-Fiction', 0),
    (3, 'L3', 'Ray Bradbury',             'Science-Fiction', 0),
    (4, 'L4', 'Alexandre Dumas',          'Aventure',        0),
    (5, 'L5', 'Victor Hugo',              'Classique',       0)
ON CONFLICT (book_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. Adhérents A1, A2, A3
--    Mot de passe : admin123 (même hash BCrypt que le compte admin)
-- ------------------------------------------------------------
INSERT INTO users (user_id, username, name, password) VALUES
    (2, 'a1_reservataire', 'Adherent A1 - Reservataire principal', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (3, 'a2_quota',        'Adherent A2 - Sature son quota',       '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (4, 'a3_emprunteur',   'Adherent A3 - Emprunteur',              '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_role (user_id, role_id) VALUES
    (2, 2),
    (3, 2),
    (4, 2)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. Emprunts en cours de A3 sur L2, L3, L4, L5 (non rendus)
-- ------------------------------------------------------------
INSERT INTO borrow (borrow_id, book_id, user_id, issue_date, due_date, status) VALUES
    (1, 2, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days', 'ACTIVE'),
    (2, 3, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days', 'ACTIVE'),
    (3, 4, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days', 'ACTIVE'),
    (4, 5, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days', 'ACTIVE')
ON CONFLICT (borrow_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. Mise à jour des séquences
-- ------------------------------------------------------------
SELECT setval('books_seq', COALESCE((SELECT MAX(book_id) + 1 FROM books), 100));
SELECT setval('users_seq', COALESCE((SELECT MAX(user_id) + 1 FROM users), 100));
SELECT setval('borrow_seq', COALESCE((SELECT MAX(borrow_id) + 1 FROM borrow), 100));

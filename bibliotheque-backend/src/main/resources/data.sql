-- ============================================================
-- data.sql — Jeu de données de test pour le module de réservation
-- Exécuté APRÈS la création des tables par Hibernate
-- (spring.jpa.defer-datasource-initialization=true)
--
-- Scénario :
--   L1          : livre disponible (1 copie)
--   L2, L3, L4, L5 : livres empruntés (0 copies)
--   A1  : adhérent réservataire principal
--   A2  : adhérent qui saturera son quota (max 3 réservations)
--   A3  : adhérent emprunteur, détient L2 à L5
--   Mot de passe de A1, A2, A3 : admin123
-- ============================================================

-- 1. Rôle Adhérent (role_id=2 ; le rôle Admin id=1 est créé par DataInitializer)
INSERT INTO role (role_id, role_name) VALUES (2, 'Adherent')
ON CONFLICT (role_id) DO NOTHING;

-- 2. Livres L1 à L5
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (1, 'L1', 'Antoine de Saint-Exupéry', 'Conte',           1),
    (2, 'L2', 'Frank Herbert',            'Science-Fiction', 0),
    (3, 'L3', 'Ray Bradbury',             'Science-Fiction', 0),
    (4, 'L4', 'Alexandre Dumas',          'Aventure',        0),
    (5, 'L5', 'Victor Hugo',              'Classique',       0)
ON CONFLICT (book_id) DO NOTHING;

-- 3. Adhérents A1, A2, A3 (hash BCrypt de "admin123")
INSERT INTO users (user_id, username, name, password) VALUES
    (2, 'a1_reservataire', 'Adherent A1 - Reservataire principal', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (3, 'a2_quota',        'Adherent A2 - Sature son quota',       '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (4, 'a3_emprunteur',   'Adherent A3 - Emprunteur',              '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO NOTHING;

-- 4. Attribution du rôle Adhérent (role_id=2)
INSERT INTO user_role (user_id, role_id) VALUES
    (2, 2),
    (3, 2),
    (4, 2)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 5. Emprunts en cours de A3 sur L2, L3, L4, L5 (non rendus)
INSERT INTO borrow (borrow_id, book_id, user_id, issue_date, due_date) VALUES
    (1, 2, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (2, 3, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (3, 4, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days'),
    (4, 5, 4, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days')
ON CONFLICT (borrow_id) DO NOTHING;

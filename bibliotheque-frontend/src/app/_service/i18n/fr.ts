/**
 * Dictionnaire de traduction français (langue par défaut).
 *
 * Structure plate « clé = phrase de référence en anglais » pour que le
 * template reste lisible : {{ 'Welcome' | t }} affiche « Bienvenue ».
 * Une clé absente du dictionnaire est affichée telle quelle.
 */
export const FR: Record<string, string> = {
  // --- Navigation ---
  'Library Management System': 'Système de Gestion de Bibliothèque',
  'Books': 'Livres',
  'Add a book': 'Ajouter un livre',
  'Users': 'Utilisateurs',
  'Register a user': 'Inscrire un utilisateur',
  'Reservations': 'Réservations',
  'Borrow books': 'Emprunter des livres',
  'Return books': 'Retourner des livres',
  'Welcome': 'Bienvenue',
  'Login': 'Connexion',
  'Logout': 'Déconnexion',

  // --- Login ---
  'Sign in to your account': 'Connectez-vous à votre compte',
  'Username': "Nom d'utilisateur",
  'Password': 'Mot de passe',
  'Sign in': 'Se connecter',
  'Signing in...': 'Connexion...',
  'Invalid username or password': "Nom d'utilisateur ou mot de passe incorrect",
  'Login error': 'Erreur de connexion',
  'Username provided by the administrator': "Nom d'utilisateur fourni par l'administrateur",

  // --- Bas de page ---
  'Library Management System — all rights reserved': 'Système de Gestion de Bibliothèque — tous droits réservés',
};

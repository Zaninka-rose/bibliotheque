/**
 * Dictionnaire de traduction anglais (langue de référence).
 *
 * Structure plate « clé = phrase de référence en anglais » : en anglais,
 * la clé est affichée telle quelle, ce dictionnaire peut donc rester
 * partiel (il sert surtout à documenter les clés disponibles).
 */
export const EN: Record<string, string> = {
  // --- Navigation ---
  'Library Management System': 'Library Management System',
  'Books': 'Books',
  'Add a book': 'Add a book',
  'Users': 'Users',
  'Register a user': 'Register a user',
  'Reservations': 'Reservations',
  'Borrow books': 'Borrow books',
  'Return books': 'Return books',
  'Welcome': 'Welcome',
  'Login': 'Login',
  'Logout': 'Logout',

  // --- Login ---
  'Sign in to your account': 'Sign in to your account',
  'Username': 'Username',
  'Password': 'Password',
  'Sign in': 'Sign in',
  'Signing in...': 'Signing in...',
  'Invalid username or password': 'Invalid username or password',
  'Login error': 'Login error',
  'Username provided by the administrator': 'Username provided by the administrator',

  // --- Footer ---
  'Library Management System — all rights reserved': 'Library Management System — all rights reserved',
};

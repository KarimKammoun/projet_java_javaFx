# Tests de l'Application LibraryMS

## Démarrage de l'Application

L'application utilise SQLite (fichier local `libraryms.db`) — pas de serveur externe requis !

```powershell
cd C:\Users\KARIM\Desktop\projet_java_javaFx
.\mvnw.cmd javafx:run
```

## Données de Test

### Admin
- **Email:** `admin@library.com`
- **Password:** `admin123`
- **Toggle:** Sélectionner "Admin"

### Membre (Member)
- **Téléphone:** `+1234567890`
- **Password:** Laissez vide (pas requis pour les membres)
- **Toggle:** Sélectionner "Member"

## Scénarios de Test

### 1. Test Login Admin
1. Lancer l'app
2. Saisir email: `admin@library.com`
3. Saisir password: `admin123`
4. Cliquer "Login"
5. **Résultat attendu:** Accès au Dashboard administrateur

### 2. Test Login Membre
1. Lancer l'app
2. Saisir téléphone: `+1234567890`
3. Laisser le champ password vide
4. Sélectionner "Member"
5. Cliquer "Login"
6. **Résultat attendu:** Accès au Member Dashboard

### 3. Test Navigation Admin
- Cliquer sur "Books" → affiche les livres
- Cliquer sur "Copies" → affiche les exemplaires
- Cliquer sur "Members" → affiche les membres
- Cliquer sur "Borrowings" → affiche les emprunts
- Cliquer sur "Settings" → accès aux paramètres
- Cliquer sur "Logout" → retour au login

### 4. Test Navigation Membre
- Cliquer sur "Browse" → affiche les livres disponibles
- Cliquer sur "My Borrowings" → affiche les emprunts personnels (avec statut)

## Données Exemple

### Livres
- Clean Code (5 copies, 3 disponibles)
- Clean Architecture (4 copies)
- Design Patterns (3 copies, 1 disponible)

### Membres
- John Doe (+1234567890) - Standard
- Jane Smith (+1234567891) - Premium

### Emprunts
- John a 2 emprunts actifs (1 normal, 1 en retard)

## Fonctionnalités Implémentées

✅ Login Admin (email + password)
✅ Login Membre (téléphone uniquement)
✅ Dashboard Admin avec statistiques
✅ Affichage des livres
✅ Affichage des exemplaires
✅ Affichage des membres
✅ Affichage des emprunts
✅ Affichage des emprunts membres
✅ Navigation entre les pages
✅ Session utilisateur
✅ Base SQLite locale (auto-initialisée)

## Dépannage

### L'app ne démarre pas
- Vérifier que Java 17 est installé: `java -version`
- Supprimer le dossier `target` et relancer: `.\mvnw.cmd clean javafx:run`

### La base de données n'existe pas
- Le fichier `libraryms.db` est créé automatiquement au premier lancement
- Cherchez le fichier à la racine du projet

### Login échoue
- Vérifier que les données correspondent au fichier `init.sql`
- Supprimer `libraryms.db` pour réinitialiser la base

### L'UI est vide
- Vérifier la console pour les erreurs SQL
- Faire défiler ou redimensionner les tables

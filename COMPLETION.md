# LibraryMS - Application Management System

## ✅ Statut

L'application **fonctionne complètement** avec Java 17, JavaFX 17, et une base de données SQLite locale.

## 🚀 Démarrage Rapide

### Lancer l'Application

```powershell
cd C:\Users\KARIM\Desktop\projet_java_javaFx
.\mvnw.cmd javafx:run
```

### Connexions de Test

**Admin:**
- Email: `admin@library.com`
- Password: `admin123`
- Toggle: **Admin**

**Membre:**
- Téléphone: `+1234567890`
- Password: *(laisser vide)*
- Toggle: **Member**

## 📋 Fonctionnalités Implémentées

### Admin
- ✅ Connexion avec email + mot de passe
- ✅ Dashboard avec statistiques (livres, exemplaires, membres, emprunts)
- ✅ Gestion des livres (affichage)
- ✅ Gestion des exemplaires (affichage)
- ✅ Gestion des membres (affichage)
- ✅ Gestion des emprunts (affichage)
- ✅ Navigation complète
- ✅ Déconnexion

### Membre
- ✅ Connexion par téléphone (sans mot de passe)
- ✅ Dashboard personnel
- ✅ Affichage des emprunts actifs et historique
- ✅ Statut des emprunts (normal, retard, complété)
- ✅ Calcul des jours restants / jours de retard
- ✅ Affichage visuel du statut (couleur)

## 🛠️ Architecture Technique

### Technologies
- **Langage:** Java 17
- **Framework UI:** JavaFX 17
- **Base de données:** SQLite (fichier local `libraryms.db`)
- **Build:** Maven 3.x

### Structure du Projet

```
projet_java_javaFx/
├── src/main/java/com/libraryms/
│   ├── App.java                          # Point d'entrée
│   ├── controller/
│   │   ├── LoginController.java          # Connexion (admin + membre)
│   │   ├── DashboardController.java      # Dashboard admin
│   │   ├── BooksController.java          # Gestion des livres
│   │   ├── CopiesController.java         # Gestion des exemplaires
│   │   ├── BorrowingsController.java     # Gestion des emprunts
│   │   ├── MembersController.java        # Gestion des membres
│   │   ├── MemberDashboardController.java # Dashboard membre
│   │   ├── MemberBorrowingsController.java # Emprunts du membre
│   │   └── SettingsController.java       # Paramètres
│   ├── model/                             # Modèles de données
│   └── util/
│       ├── DatabaseUtil.java             # Connexion DB SQLite
│       ├── InitDB.java                   # Initialisation DB
│       ├── SceneManager.java             # Gestion des scènes
│       └── Session.java                  # Données session utilisateur
├── src/main/resources/
│   ├── fxml/                             # Fichiers FXML (UI)
│   ├── css/                              # Feuilles de style
│   └── application.properties            # Configuration
├── pom.xml                               # Dépendances Maven
├── init.sql                              # SQL PostgreSQL (référence)
└── libraryms.db                          # Base SQLite (créée auto)
```

## 🔧 Corrections Apportées

1. **Login fonctionnel**
   - Admin: email + password
   - Membre: téléphone uniquement
   - Session utilisateur persistante

2. **Base de données**
   - Migré de PostgreSQL à SQLite (auto-initialisé)
   - Pas de serveur externe requis
   - Base créée automatiquement au démarrage

3. **Gestion des erreurs**
   - Messages d'erreur clairs au login
   - Alertes en cas de fichier FXML manquant
   - Gestion des requêtes SQL null

4. **Interface utilisateur**
   - Navigation complète entre les pages
   - Affichage des données des tables
   - Statuts visuels pour les emprunts

## 📦 Build et Déploiement

### Créer un JAR exécutable

```powershell
.\mvnw.cmd clean package
```

Fichier généré: `target\libraryms-1.0.0.jar`

### Créer un EXE Windows (optionnel, avec jpackage)

Voir la section "Advanced" du README.md

## 🐛 Dépannage

| Problème | Solution |
|----------|----------|
| L'app ne démarre pas | Vérifier Java 17: `java -version` |
| La DB ne se crée pas | Supprimer `libraryms.db` et relancer |
| Login échoue | Vérifier les données: admin@library.com / admin123 |
| Tableau vide | Vérifier la console pour erreurs SQL |
| UI blanche | Attendre que l'app charge (première fois peut être lente) |

## 📝 Notes

- La base de données est stockée localement dans `libraryms.db`
- Les données de test sont créées automatiquement au premier lancement
- Les mots de passe sont stockés en clair (pour démo seulement - utiliser BCrypt en production)
- L'application fonctionne entièrement hors ligne

## 🎯 Prochaines Étapes (Optionnel)

- Ajouter CRUD complet (Create, Read, Update, Delete)
- Ajouter authentification avec BCrypt
- Ajouter recherche et filtrage
- Packager en exécutable Windows (.exe)
- Ajouter des graphiques/rapports plus avancés
- Intégrer une base de données PostgreSQL en optionnel

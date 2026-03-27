# Documentation du Projet : aniyomi-extensions-french-only

Ce document fournit une vue d'ensemble détaillée du fonctionnement, de la structure et du flux de travail de ce repository.

## 🎯 Objectif du Projet
L'objectif principal de ce fork est de fournir et maintenir des **modules de parsing (scrapers)** optimisés pour la **French Localization** au sein des environnements basés sur l'architecture Anizen. Le nettoyage des composants non francophones permet de réduire les temps de build et de se concentrer sur la stabilité des flux de métadonnées audio et sous-titres.

---

## 🏗️ Architecture du Codebase

Le projet est structuré comme une application Android multi-modules utilisant Gradle.

### 1. `/core`
Contient les classes de base et les interfaces nécessaires au fonctionnement des scrapers (modèles de données, intercepteurs réseau, gestion des requêtes).

### 2. `/lib` (Extracteurs)
Regroupe les scripts d'extraction de métadonnées média pour divers hébergeurs distants (ex: `doodextractor`, `sibnetextractor`, `voeextractor`). Ces bibliothèques sont partagées entre plusieurs modules pour éviter la duplication de code.

### 3. `/lib-multisrc` (Thèmes partagés)
Contient des thèmes génériques pour les providers utilisant le même moteur (CMS).
- **datalifeengine** : Utilisé par `Provider-FA`.
- **dooplay** : Thème courant pour les CMS média.
- **animestream**, **pelisplus**, etc.

### 4. `/src/fr` (Modules Francophones)
C'est ici que résident les scrapers spécifiques à la région française :
- **Provider-AS** : Module de parsing haute performance.
- **Provider-AS (RIP)** : Scraper alternatif optimisé.
- **Provider-AF** : Media Handler moderne et dynamique.
- **Provider-FA** : Handler historique basé sur le moteur `datalifeengine`.
- **Provider-ST** : Handler spécialisé dans les contenus d'animation occidentale.
- **Provider-WA** : Module supportant les flux multi-pistes.
- **Provider-UX** : Handler avec fusion intelligente des métadonnées.

---

## 🛡️ Procédure de Validation (Obligatoire)

Avant de proposer ou de valider tout changement (Commit/PR), il est **obligatoire** de lancer l'orchestrateur de validation des scrapers :

```bash
python3 audit_extensions.py
```

---

## 📝 Conventions de Commit

Il est **strictement interdit** d'utiliser des emojis dans les messages de commit. Les messages doivent être sobres, techniques, et commencer par un préfixe suivi du changement (ex: `Cleanup: ...`, `Fix: ...`, `Feat: ...`).

---

### Historique des modifications (Gemini)

#### 20. Pistes d'améliorations et bugs identifiés (Mars 2026)

**Note Importante :** Pour chaque modification effectuée, le module doit être buildé et une validation via installation **ADB** doit être proposée à l'utilisateur.

#### 1. Provider-AS (RIP) (Recherche locale)
- **Status** : Résolue.

#### 2. Provider-WA (Tri)
- **Status** : Résolue.

#### 3. Provider-WA (Latest)
- **Status** : Résolue.

#### 4. Provider-WA (Flux multi-pistes)
- **Statut** : En attente de l'optimisation des parsers DASH.

#### 5. Provider-VA (Error Handling)
- **Status** : Résolue.

#### 6. Provider-UX (Parsing & Fusion)
- **Status :** Résolue (v3). 
- **Solution :** Récupération des métadonnées via AJAX (`full-story.php`) et fusion robuste par recherche automatisée. Formatage des flux avec labels de localisation.

#### 7. Provider-FM (Sécurité des données)
- **Status** : Résolue (v6).

#### 10. Provider-AK (Recherche & Métadonnées spécifiques)
- **Status** : Résolue (v3). Gestion des flags de recherche et refactorisation vers `AnimeHttpSource`.

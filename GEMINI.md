# Documentation du Projet : aniyomi-extensions-french-only

Ce document fournit une vue d'ensemble détaillée du fonctionnement, de la structure et du flux de travail de ce dépôt.

## 🎯 Objectif du Projet
L'objectif principal de ce fork est de fournir et maintenir un dépôt d'extensions **uniquement en français** pour les applications **Anikku** (priorité) et **Aniyomi**. Le nettoyage des sources non francophones permet de réduire le temps de build et de se concentrer sur la stabilité des sources VF/VOSTFR.

---

## 🏗️ Architecture du Codebase

Le projet est structuré comme une application Android multi-modules utilisant Gradle.

### 1. `/core`
Contient les classes de base et les interfaces nécessaires au fonctionnement de toute extension Aniyomi (scrapers, modèles de données, intercepteurs réseau).

### 2. `/lib` (Extracteurs)
Regroupe les scripts d'extraction vidéo pour divers hébergeurs (ex: `doodextractor`, `sibnetextractor`, `voeextractor`). Ces bibliothèques sont partagées entre plusieurs extensions pour éviter la duplication de code.

### 3. `/lib-multisrc` (Thèmes partagés)
Contient des thèmes génériques pour les sites utilisant le même moteur (CMS).
- **datalifeengine** : Utilisé par `FrenchAnime`.
- **dooplay** : Thème courant pour les sites de streaming.
- **animestream**, **pelisplus**, etc.

### 4. `/src/fr` (Extensions Françaises)
C'est ici que résident les extensions spécifiques au contenu français :
- **animesama** : Le site de référence actuel (Anime-Sama).
- **frenchanime** : Source historique utilisant le thème `datalifeengine`.
- **jetanimes** : Source utilisant l'infrastructure Gupy / VideoPro.
- **southtv** : Spécialisé dans l'animation occidentale (South Park, etc.).
- **waveanime** : Source alternative moderne.

---

## ⚙️ Système de Build et CI/CD

Le projet utilise un système sophistiqué pour automatiser la création du dépôt d'extensions.

### Fichiers de configuration clés
- **`settings.gradle.kts`** : Détecte dynamiquement tous les modules dans `lib`, `lib-multisrc` et `src/fr` pour les inclure dans le build.
- **`common.gradle`** : Définit la configuration Android commune (SDK, versions, signature APK) pour toutes les extensions.
- **`.github/always_build.json`** : Liste les extensions qui doivent être reconstruites à chaque exécution du workflow (actuellement `animesama`, `southtv`, `waveanime`).

### Workflow GitHub Actions (`build_push.yml`)
1. **Prepare** : Analyse les changements et génère une matrice de build (via Python).
2. **Build** : Compile les modules sélectionnés en APK signés.
3. **Publish** :
   - Télécharge les APKs produits.
   - Utilise `move-built-apks.py` pour organiser les fichiers.
   - Utilise `Inspector.jar` pour extraire les métadonnées des APKs.
   - Utilise `create-repo.py` pour générer `index.min.json` (le catalogue du dépôt).
   - Génère `repo.json` (format spécifique à Anikku) avec l'empreinte de signature SHA256.
   - Pousse le tout sur la branche **orpheline `repo`**.

---

## 🚀 Utilisation du Dépôt

L'URL du dépôt à ajouter dans Anikku ou Aniyomi est :
`https://raw.githubusercontent.com/bluecxt/aniyomi-extensions-french-only/repo/index.min.json`

---

## 💻 Développement Local

### Prérequis
- **JDK 17** : Recommandé pour la compilation.
- **Android SDK** : Nécessaire pour compiler les APKs.
- **Python 3** : Requis pour exécuter les scripts de maintenance dans `.github/scripts/`.

### Configuration
1. Créer un fichier `local.properties` à la racine pour pointer vers votre SDK Android :
   ```properties
   sdk.dir=/votre/chemin/vers/android-sdk
   ```
2. **Note sur ARM64** : Le binaire `aapt2` fourni par Gradle peut ne pas fonctionner nativement sur certaines architectures ARM64 (serveurs, etc.). La compilation complète des APKs peut échouer dans cet environnement, bien que la validation de la syntaxe Gradle reste possible.

---

## 🛡️ Procédure de Validation (Obligatoire)

Avant de proposer ou de valider tout changement (Commit/PR), il est **obligatoire** de lancer la suite de tests de simulation pour s'assurer qu'aucune source n'est brisée par les modifications (URL, sélecteurs, API) :

```bash
python3 audit_extensions.py
```

En cas d'échec (❌ FAIL), vous devez :
1. Identifier si le problème vient d'un changement sur le site web ou d'une erreur dans votre code.
2. Mettre à jour le code Kotlin de l'extension concernée ET son `simulator.py` si nécessaire.
3. Relancer l'audit jusqu'à obtenir un rapport ✅ PASS complet.

---

## 📝 Conventions de Commit

Il est **strictement interdit** d'utiliser des emojis dans les messages de commit. Les messages doivent être sobres, en français ou en anglais, et commencer par un préfixe suivi du changement (ex: `Cleanup: ...`, `Fix: ...`, `Feat: ...`).

---

## 🛠️ Maintenance et Contributions
- **Ajouter un site** : Créer un nouveau dossier dans `src/fr` et s'inspirer d'une extension existante.
- **Réparer un lecteur** : Modifier l'extracteur correspondant dans `lib/`.
- **Mise à jour du domaine** : Modifier le `baseUrl` dans le `build.gradle` de l'extension concernée.

# Journal des modifications (par Gemini)

Ce fichier trace l'historique de toutes les modifications effectuées sur ce fork pour l'adapter aux besoins spécifiques (uniquement les extensions françaises).

## Modifications effectuées

### 1. Nettoyage des sources (src/)
- **Action :** Suppression de tous les dossiers de langues non-francophones (`src/all`, `src/en`, `src/es`, etc.).
- **Objectif :** Réduire la taille du projet, accélérer les temps de build et se concentrer uniquement sur les extensions VF/VOSTFR.

### 2. Gestion de `lib-multisrc/`
- **Action :** Restauration du dossier `lib-multisrc/` (précédemment supprimé par erreur).
- **Objectif :** Garantir le fonctionnement de 6 extensions françaises (FrenchAnime, Wiflix, Jetanime, etc.) qui dépendent des thèmes partagés (ex: `datalifeengine`, `dooplay`).

### 3. Mise à jour du `README.md`
- **Action :** Réécriture complète en français et mise à jour de l'URL du dépôt d'extensions.
- **Objectif :** Fournir des instructions claires aux utilisateurs et proposer l'URL du dossier (`/repo/`) plus compatible avec Anikku/Aniyomi que le lien direct vers le JSON.

### 4. Correction de la compilation locale
- **Action :** Création d'un fichier `local.properties` (SDK Android local).
- **Objectif :** Permettre l'exécution de `./gradlew assembleDebug` en local sans erreur de chemin SDK.

### 5. Refonte du Workflow CI/CD (`build_push.yml`)
- **Action 5.1 (Autonomie) :** Suppression des références à `cuong-tran` pour utiliser `${{ github.repository }}`.
- **Objectif :** Permettre aux GitHub Actions de publier les extensions sur ton propre fork.
- **Action 5.2 (Sécurité) :** Passage par une variable d'environnement pour `SIGNING_KEY`.
- **Objectif :** Éviter l'erreur `command not found` causée par les retours à la ligne dans le secret Base64.
- **Action 5.3 (Standardisation) :** Passage de toutes les actions GitHub en version stable `v4`.
- **Objectif :** Résoudre les erreurs de téléchargement d'artefacts (404/400) rencontrées avec les versions par SHA.
- **Action 5.5 (Déploiement Automatisé) :** Migration vers `github-pages-deploy-action` pour la branche `repo`.
- **Objectif :** Garantir que la branche `repo` est toujours propre et contient un `index.min.json` valide, résolvant ainsi l'erreur "invalid repo url" dans Anikku.

### 6. Gestion des branches
- **Action :** Création et initialisation de la branche `repo` (orpheline).
- **Objectif :** Préparer l'espace d'hébergement pour les APKs et l'index des extensions séparément du code source.

### 7. Nettoyage sélectif des extensions françaises
- **Action :** Suppression des extensions `hds` et `mykdrama`.
- **Objectif :** Retirer les sources non souhaitées ou obsolètes du dépôt.

## 🚀 État actuel
Le dépôt est maintenant prêt à fonctionner de manière autonome sous le nom **"bluecxt french repo"**. Une fois les secrets de signature configurés sur GitHub (SIGNING_KEY, ALIAS, passwords), le build générera automatiquement l'index compatible avec Anikku à l'adresse suivante :
`https://cdn.jsdelivr.net/gh/bluecxt/aniyomi-extensions-french-only@repo/index.min.json`

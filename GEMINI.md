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

### 8. Correction du build incrémental (CI/CD)
- **Action 8.1 :** Modification de `build_push.yml` pour extraire la branche `repo` existante avant le build.
- **Action 8.2 :** Mise à jour de `move-built-apks.py` pour fusionner les nouveaux APKs avec les anciens (au lieu de tout supprimer).
- **Action 8.3 :** Ajout de `fr.animesama` et `fr.southtv` dans `always_build.json`.
- **Objectif :** Éviter que les extensions ne disparaissent du dépôt lorsqu'une seule extension est mise à jour.

### 9. Installation de l'environnement de build local (ARM64)
- **Action :** Installation d'OpenJDK 17 et des outils Android SDK (`cmdline-tools`, `platforms;android-34`, `build-tools;34.0.0`).
- **Action :** Configuration de `local.properties` pour pointer vers `/root/android-sdk`.
- **Note :** Le binaire `aapt2` fourni par Gradle ne fonctionne pas nativement sur ARM64, ce qui empêche la compilation complète sur ce serveur spécifique, mais permet de valider la syntaxe via Gradle.

### 10. Fix de l'extension Franime (watch2/lpayer)
- **Action 9.1 :** Analyse et rétro-ingénierie du décodeur JavaScript du site `franime.fr` pour le nouveau lecteur `lpayer`.
- **Action 9.2 :** Ajout de la dépendance `cryptoaes` à l'extension `franime`.
- **Action 9.3 :** Implémentation du décodeur AES-CBC dans `FrAnime.kt` pour extraire les liens `m3u8` à partir des paramètres chiffrés (`a`, `o`).
- **Action 9.4 :** Correction du décodage du paramètre `a` (Base64 d'une chaîne Hex) et ajout d'un fallback utilisant `o` comme `videoId`.
- **Action 9.5 :** Mise à jour de la version de l'extension vers la v25.
- **Objectif :** Résoudre l'erreur "no available videos" causée par le changement de structure du site Franime.

### 11. Fix et formatage de l'extension WaveAnime
- **Action 11.1 :** Correction des erreurs de formatage Kotlin via `./gradlew spotlessApply`.
- **Action 11.2 :** Ajout de l'import manquant `eu.kanade.tachiyomi.util.asJsoup` pour résoudre les erreurs de compilation.
- **Objectif :** Permettre le build de la nouvelle extension WaveAnime qui échouait en CI.

### 12. Nettoyage et correctifs divers (Wiflix, Vostfree, SouthTV)
- **Action 12.1 :** Suppression définitive de l'extension `Wiflix` car le site n'existe plus.
- **Action 12.2 :** Correction de l'extension `Vostfree` (v36) : mise à jour des sélecteurs CSS pour le chargement de la liste d'animés et de la recherche.
- **Action 12.3 :** Correction de l'extension `SouthTV` (v4) : résolution du crash d'index et mise à jour du domaine vidéo vers `southtv.fr`.
- **Objectif :** Maintenir la fonctionnalité globale du dépôt.

### 13. Ajout de nouvelles extensions (Voiranime, Animoflix)
- **Action 13.1 :** Création de l'extension `Voiranime` (voiranime.io) avec support des lecteurs Vidmoly, Sibnet, Sendvid, etc.
- **Action 13.2 :** Création de l'extension `Animoflix` (animoflix.com) avec support des lecteurs Sibnet, Sendvid, Doodstream et Filemoon.
- **Action 13.3 :** Ajout de ces extensions dans `always_build.json` pour un build automatique.
- **Objectif :** Étendre le catalogue d'extensions francophones.

### 14. Nettoyage des extensions obsolètes (OtakuFR, VoirCartoon)
- **Action :** Suppression définitive des extensions `OtakuFR` et `VoirCartoon`.
- **Objectif :** Retirer les sources dont les sites web n'existent plus.

## 🚀 État actuel
Le dépôt est maintenant prêt à fonctionner de manière autonome sous le nom **"bluecxt french repo"**. Une fois les secrets de signature configurés sur GitHub (SIGNING_KEY, ALIAS, passwords), le build générera automatiquement l'index compatible avec Anikku à l'adresse suivante :
`https://cdn.jsdelivr.net/gh/bluecxt/aniyomi-extensions-french-only@repo/index.min.json`
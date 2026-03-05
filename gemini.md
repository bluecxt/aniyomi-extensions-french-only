# Journal des modifications (par Gemini)

Ce fichier trace l'historique de toutes les modifications effectuées sur ce fork pour l'adapter aux besoins spécifiques (uniquement les extensions françaises).

## Modifications effectuées

1. **Nettoyage des sources (src/)** :
   - Suppression de tous les dossiers de langues non-francophones (`src/all`, `src/en`, `src/es`, etc.) pour ne conserver que le dossier `src/fr/`.
   - **Objectif :** Réduire la taille du projet et se concentrer uniquement sur les extensions VF/VOSTFR.

2. **Tentative de nettoyage de `lib-multisrc/` (Annulée/Restaurée)** :
   - *Action initiale :* Suppression du dossier `lib-multisrc/` et de son inclusion dans `settings.gradle.kts`.
   - *Annulation :* Restauration via `git checkout` car 6 extensions françaises (FrenchAnime, Wiflix, Jetanime, etc.) dépendent des thèmes partagés dans ce dossier (ex: `datalifeengine`, `dooplay`).

3. **Mise à jour du `README.md`** :
   - Réécriture complète en français.
   - Ajout d'un avertissement précisant que les extensions sont testées en priorité sur **Anikku** (et pas forcément garanties sur Aniyomi).
   - Ajout des instructions d'installation avec le lien vers `index.min.json`.
   - Ajout de la liste complète des 13 extensions françaises restantes.

4. **Correction de la compilation locale** :
   - Création d'un fichier `local.properties` à la racine contenant le chemin vers le SDK Android local (`sdk.dir=/home/moi/Android/Sdk`).
   - **Objectif :** Permettre l'exécution de `./gradlew assembleDebug` avec succès sans erreur de SDK introuvable.

5. **Modification du Workflow CI/CD (`build_push.yml`)** :
   - Suppression de la condition hardcodée `if: github.repository == 'cuong-tran/aniyomi-extensions'` pour le build et la publication.
   - **Objectif :** Permettre aux GitHub Actions de s'exécuter sur ton propre fork au lieu d'être bloquées car elles ne sont pas sur le dépôt du créateur original.

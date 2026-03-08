<div align="center">

# 🇫🇷 bluecxt french repo 🇫🇷
### Le dépôt d'extensions VF et VOSTFR pour Anikku / Aniyomi

---


**⚠️ Note Importante :**  
Toutes les extensions présentes dans ce dépôt sont **exclusivement testées et optimisées pour l'application [Anikku](https://github.com/komikku-app/anikku)**. Leur bon fonctionnement sur [Aniyomi](https://github.com/aniyomiorg/aniyomi) n'est **pas** garanti.

</div>

---

## 📌 Comment ajouter ce dépôt

### Méthode 1 : Installation en un clic (Recommandé)

Cliquez sur le bouton correspondant à votre application pour ajouter automatiquement le dépôt :

| Pour Anikku | Pour Aniyomi |
|:---:|:---:|
| [![Install on Anikku](https://img.shields.io/badge/Ajouter%20à%20Anikku-orange?style=for-the-badge&logo=android)](https://intradeus.github.io/http-protocol-redirector/?r=anikku://add-repo?url=https://raw.githubusercontent.com/bluecxt/aniyomi-extensions-french-only/repo/index.min.json) | [![Install on Aniyomi](https://img.shields.io/badge/Ajouter%20à%20Aniyomi-blue?style=for-the-badge&logo=android)](https://intradeus.github.io/http-protocol-redirector/?r=aniyomi://add-repo?url=https://raw.githubusercontent.com/bluecxt/aniyomi-extensions-french-only/repo/index.min.json) |

### Méthode 2 : Ajout manuel

Si la méthode ci-dessus ne fonctionne pas, copiez et collez cette URL (directe) :

```text
https://raw.githubusercontent.com/bluecxt/aniyomi-extensions-french-only/repo/index.min.json
```

## 🛠️ Développement Local et Tests

Ce dépôt inclut des outils pour tester les extensions sans avoir besoin d'un appareil Android.

### 🧪 Simulateurs (Recommandé)
Chaque extension dispose d'un script `simulator.py` qui imite le comportement d'Anikku pour vérifier si le site source est toujours fonctionnel.

**Lancer tous les tests (Orchestrateur) :**
```bash
python3 audit_extensions.py
```

**Tester une extension spécifique :**
```bash
python3 src/fr/animesama/simulator.py
```

### 🔨 Compilation des APKs
Pour compiler une extension spécifique :
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk # Si nécessaire
./gradlew :src:fr:animesama:assembleDebug
```
L'APK sera généré dans `src/fr/animesama/build/outputs/apk/debug/`.

## ✅ État des extensions (Mars 2026)

Le dépôt a été épuré pour ne conserver que les sources 100% opérationnelles.

| Extension | État | Note |
| :--- | :--- | :--- |
| **Anime-Sama** | ✅ Opérationnelle | Référence (VF/VOSTFR). |
| **ADKami** | ✅ Opérationnelle | Source riche (inclut Hentai/NSFW). |
| **FrenchAnime** | ✅ Opérationnelle | Catalogue historique stable. |
| **French-Manga** | ✅ Opérationnelle | Source DLE avec API AJAX. |
| **FrAnime** | 🚧 En cours | Catalogue et épisodes OK (Lecteurs à venir). |
| **Stream-VF** | ✅ Opérationnelle | Source avec Sibnet (principalement). |
| **VoirAnime** | ✅ Opérationnelle | Nouvelle source DramaStream. |
| **SouthTV** | ✅ Opérationnelle | Animation US et Séries. |
| **WaveAnime** | ✅ Opérationnelle | Source moderne fonctionnelle. |

---

## 🛠️ Extensions disponibles

Ce dépôt regroupe et maintient les meilleures sources françaises :

- **Anime-Sama** : Le plus gros catalogue actuel.
- **ADKami** : Très complet, inclut également du contenu adulte.
- **FrenchAnime** : Très stable pour les classiques et nouveautés.
- **French-Manga** : Catalogue varié et mise à jour rapide.
- **FrAnime** : Interface moderne, en cours de développement.
- **Stream-VF** : Spécialisé VF avec de bons lecteurs.
- **VoirAnime** : Source riche en VF/VOSTFR.
- **SouthTV** : Le spécialiste pour South Park et l'animation adulte.
- **WaveAnime** : Alternative rapide et efficace.

Toutes les extensions se trouvent dans le dossier `src/fr/`. Tous les outils d'extraction vidéo (nécessaires si vous voulez ajouter de nouveaux sites) ont été conservés dans le dossier `lib/`.

## 🤝 Contribuer ou Signaler un problème

Les contributions sont les bienvenues ! Tu peux ouvrir une "Issue" ou proposer une "Pull Request" si tu souhaites :
- Ajouter un nouveau site de streaming francophone.
- Corriger un lecteur vidéo qui ne fonctionne plus (les "Extractors").
- Mettre à jour une extension existante dont la structure HTML a changé.

## ⚖️ Avertissements légaux

Ce projet est open source.

**Disclaimer :**
- Les développeurs de ce projet n'hébergent aucune vidéo et n'ont **aucune affiliation** avec les fournisseurs de contenu, les sites web ciblés ou les services d'hébergement vidéo. Ces extensions agissent uniquement comme des navigateurs web automatisés (scrapers) pour afficher publiquement du contenu web dans une interface tierce.
- Ce projet est indépendant et n'est pas affilié officiellement aux équipes de développement d'Anikku ou d'Aniyomi. **Merci de ne pas demander d'aide concernant ces extensions spécifiques sur les serveurs Discord officiels d'Aniyomi ou d'Anikku.**

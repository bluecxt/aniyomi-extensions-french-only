<div align="center">

# 🇫🇷 bluecxt french repo 🇫🇷
### Le dépôt d'extensions VF et VOSTFR pour Anikku / Aniyomi

---


**⚠️ Note Importante :**  
Toutes les extensions présentes dans ce dépôt sont **testées et optimisées en priorité pour l'application [Anikku](https://github.com/komikku-app/anikku)**. Bien qu'elles soient techniquement compatibles avec [Aniyomi](https://github.com/aniyomiorg/aniyomi), leur fonctionnement optimal et sans bug n'y est pas systématiquement garanti.

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

## ⚠️ État des extensions (Mars 2026)

Certaines extensions rencontrent des problèmes suite à des mises à jour majeures des sites sources :

| Extension | État | Problème |
| :--- | :--- | :--- |
| **Anime-Sama** | ✅ Opérationnelle | Catalogue immense, nouveautés VF et VOSTFR. |
| **StreamVF** | ✅ Opérationnelle | Nouvelle extension stable (stream-vf.top). |
| **FrenchManga** | ✅ Opérationnelle | Nouvelle extension stable (DLE). |
| **SouthTV** | ✅ Opérationnelle | Spécialiste dessins animés et séries US. |
| **JetAnime** | ✅ Opérationnelle | Domaine `on.jetanimes.com` (Réinstallez si 404). |
| **Voiranime** | ✅ Opérationnelle | Sélecteurs de lecteurs vidéo mis à jour. |
| **Animoflix** | ❌ Instable | Lecteurs protégés par Cloudflare/JS. |
| **Vostfree** | ❌ Instable | Chargement des épisodes AJAX bloqué. |
| **FrAnime** | ❌ Instable | API de recherche cassée et encryption complexe. |

---

## 🛠️ Extensions disponibles

Ce dépôt regroupe et maintient les sources françaises. Voici la liste des extensions principales :

- **Anime-Sama** : Un catalogue immense avec plusieurs lecteurs.
- **StreamVF** : Rapide et efficace, lecteurs variés (Dood, Voe, Sibnet).
- **FrenchManga** : Moteur DLE robuste pour les animés VF/VOSTFR.
- **SouthTV** : Le spécialiste pour South Park et l'animation US.
- **Jetanime** : L'un des sites les plus anciens (Actuellement en maintenance).
- **Voiranime** : Large choix d'animés (Actuellement en maintenance).
- **Vostfree** : Référence pour la VOSTFR et la VF (Actuellement instable).
- **Franime** : Interface propre et rapide (Actuellement instable).

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

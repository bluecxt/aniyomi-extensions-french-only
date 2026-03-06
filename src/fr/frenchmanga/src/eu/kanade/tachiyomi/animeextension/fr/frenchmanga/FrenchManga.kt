package eu.kanade.tachiyomi.animeextension.fr.frenchmanga

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.okruextractor.OkruExtractor
import eu.kanade.tachiyomi.lib.sendvidextractor.SendvidExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.streamwishextractor.StreamWishExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.multisrc.datalifeengine.DataLifeEngine
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class FrenchManga :
    DataLifeEngine(
        "FrenchManga",
        "https://french-manga.net",
        "fr",
    ) {
    override val categories = arrayOf(
        Pair("<Sélectionner>", ""),
        Pair("Anime", "/xfsearch/Type/Anime/"),
        Pair("Films", "/xfsearch/Type/Film/"),
    )

    override val genres = arrayOf(
        Pair("<Sélectionner>", ""),
        Pair("Action", "/xfsearch/manga_genre/Action/"),
        Pair("Aventure", "/xfsearch/manga_genre/Aventure/"),
        Pair("Comédie", "/xfsearch/manga_genre/Comédie/"),
        Pair("Drame", "/xfsearch/manga_genre/Drame/"),
        Pair("Ecchi", "/xfsearch/manga_genre/Ecchi/"),
        Pair("Fantastique", "/xfsearch/manga_genre/Fantastique/"),
        Pair("Horreur", "/xfsearch/manga_genre/Horreur/"),
        Pair("Romance", "/xfsearch/manga_genre/Romance/"),
        Pair("Sci-Fi", "/xfsearch/manga_genre/Science-fiction/"),
        Pair("Seinen", "/xfsearch/manga_genre/Seinen/"),
        Pair("Shônen", "/xfsearch/manga_genre/Shônen/"),
        Pair("Surnaturel", "/xfsearch/manga_genre/Surnaturel/"),
        Pair("Thriller", "/xfsearch/manga_genre/Thriller/"),
    )

    private val preferences by getPreferencesLazy()

    override fun popularAnimeRequest(page: Int) = GET("$baseUrl/manga-streaming-1/page/$page/", headers)

    override fun animeDetailsParse(document: Document) = SAnime.create().apply {
        description = document.selectFirst("div.full-text")?.text()
        genre = document.select("li:contains(Genre) a").joinToString { it.text() }
        author = document.selectFirst("li:contains(Studio) a")?.text()
        status = when {
            document.select("li:contains(Statut) a").text().contains("En cours", true) -> SAnime.ONGOING
            document.select("li:contains(Statut) a").text().contains("Terminé", true) -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector() = "select#player-select option"

    override fun episodeFromElement(element: Element) = SEpisode.create().apply {
        val nameText = element.text()
        name = nameText
        episode_number = nameText.filter { it.isDigit() }.toFloatOrNull() ?: 1f
        setUrlWithoutDomain(element.attr("value"))
    }

    override fun episodeListParse(response: Response) = super.episodeListParse(response).reversed()

    // ============================ Video Links =============================
    override fun videoListSelector() = throw UnsupportedOperationException()
    override fun videoFromElement(element: Element) = throw UnsupportedOperationException()
    override fun videoUrlParse(document: Document) = throw UnsupportedOperationException()

    private val doodExtractor by lazy { DoodExtractor(client) }
    private val streamwishExtractor by lazy { StreamWishExtractor(client, headers) }
    private val sibnetExtractor by lazy { SibnetExtractor(client) }
    private val voeExtractor by lazy { VoeExtractor(client, headers) }
    private val sendvidExtractor by lazy { SendvidExtractor(client, headers) }
    private val okruExtractor by lazy { OkruExtractor(client) }

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val iframes = document.select("div.player-iframe-container iframe")

        return iframes.parallelCatchingFlatMapBlocking {
            val url = it.attr("abs:src")
            getVideosFromUrl(url)
        }
    }

    private fun getVideosFromUrl(url: String): List<Video> = when {
        url.contains("dood") || url.contains("d0000d") -> doodExtractor.videosFromUrl(url)
        url.contains("streamwish") -> streamwishExtractor.videosFromUrl(url)
        url.contains("sibnet") -> sibnetExtractor.videosFromUrl(url)
        url.contains("voe") -> voeExtractor.videosFromUrl(url)
        url.contains("sendvid") -> sendvidExtractor.videosFromUrl(url)
        url.contains("ok.ru") -> okruExtractor.videosFromUrl(url)
        else -> emptyList()
    }

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString("preferred_quality", "1080")!!
        return this.sortedWith(
            compareByDescending<Video> { it.quality.contains(quality) }
                .thenByDescending { it.quality },
        )
    }
}

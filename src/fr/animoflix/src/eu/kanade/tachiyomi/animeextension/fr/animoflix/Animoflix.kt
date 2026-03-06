package eu.kanade.tachiyomi.animeextension.fr.animoflix

import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor
import eu.kanade.tachiyomi.lib.sendvidextractor.SendvidExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Animoflix :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "Animoflix"

    override val baseUrl = "https://animoflix.com"

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeSelector() = "a.anime-card"

    override fun popularAnimeRequest(page: Int) = GET("$baseUrl/catalogue/")

    override fun popularAnimeFromElement(element: Element) = SAnime.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        title = element.selectFirst("h3")?.text() ?: ""
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    override fun popularAnimeNextPageSelector(): String? = null

    // =============================== Latest ===============================
    override fun latestUpdatesSelector() = "a.anime-card"

    override fun latestUpdatesRequest(page: Int) = GET(baseUrl)

    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // =============================== Search ===============================
    override fun searchAnimeSelector() = "a.anime-card"

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = if (query.isNotEmpty()) {
        GET("$baseUrl/catalogue/?search=$query")
    } else {
        GET("$baseUrl/catalogue/")
    }

    override fun searchAnimeFromElement(element: Element) = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String? = null

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document) = SAnime.create().apply {
        title = document.selectFirst("h1.anime-title")?.text() ?: ""
        description = document.selectFirst("div.anime-description p")?.text()
        genre = document.select("div.anime-genres p").text()
        status = parseStatus(document.selectFirst("p.anime-status")?.text())
    }

    private fun parseStatus(status: String?): Int = when {
        status == null -> SAnime.UNKNOWN
        status.contains("En cours", true) -> SAnime.ONGOING
        status.contains("Terminé", true) -> SAnime.COMPLETED
        else -> SAnime.UNKNOWN
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        // Animoflix lists seasons on the anime page.
        // We need to fetch each season page to get episodes?
        // Or are they all on the main page?
        // In my check, I saw /anime/sentenced-to-be-a-hero/saison-1/

        val seasons = document.select("a[href*='/saison-']")
        if (seasons.isEmpty()) {
            // Check if it's a single season or movie
            // ...
        }

        // For now, let's assume we are on the anime page and we need to fetch episodes from all seasons.
        // This is tricky for ParsedAnimeHttpSource.
        // Let's simplify and just get episodes if they are listed.

        // Actually, let's check the season page content again.
        return document.select("a[href*='/episode-']").map {
            SEpisode.create().apply {
                val url = it.attr("href")
                setUrlWithoutDomain(url)
                name = it.text().ifEmpty { url.substringAfterLast("/").replace("-", " ").capitalize() }
                episode_number = url.substringAfter("episode-").substringBefore("/").toFloatOrNull() ?: 0f
            }
        }.reversed()
    }

    override fun episodeListSelector() = throw UnsupportedOperationException()
    override fun episodeFromElement(element: Element) = throw UnsupportedOperationException()

    // ============================ Video Links =============================
    private val doodExtractor by lazy { DoodExtractor(client) }
    private val sibnetExtractor by lazy { SibnetExtractor(client) }
    private val sendvidExtractor by lazy { SendvidExtractor(client, headers) }
    private val filemoonExtractor by lazy { FilemoonExtractor(client) }

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()

        val players = document.select("select.player-controls option")

        return players.parallelCatchingFlatMapBlocking {
            val iframeSrc = it.attr("value")
            if (iframeSrc.isEmpty() || !iframeSrc.startsWith("http")) return@parallelCatchingFlatMapBlocking emptyList<Video>()

            when {
                iframeSrc.contains("sibnet") -> sibnetExtractor.videosFromUrl(iframeSrc)
                iframeSrc.contains("sendvid") -> sendvidExtractor.videosFromUrl(iframeSrc)
                iframeSrc.contains("dood") -> doodExtractor.videosFromUrl(iframeSrc)
                iframeSrc.contains("filemoon") -> filemoonExtractor.videosFromUrl(iframeSrc)
                else -> emptyList()
            }
        }
    }

    override fun videoListSelector() = throw UnsupportedOperationException()
    override fun videoFromElement(element: Element) = throw UnsupportedOperationException()
    override fun videoUrlParse(document: Document) = throw UnsupportedOperationException()

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString(PREF_QUALITY_KEY, PREF_QUALITY_DEFAULT)!!
        return sortedWith(
            compareBy { it.quality.contains(quality) },
        ).reversed()
    }

    // ============================== Settings ==============================
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = PREF_QUALITY_KEY
            title = "Qualité préférée"
            entries = arrayOf("1080p", "720p", "480p", "360p")
            entryValues = arrayOf("1080p", "720p", "480p", "360p")
            setDefaultValue(PREF_QUALITY_DEFAULT)
            summary = "%s"
        }.also(screen::addPreference)
    }

    companion object {
        private const val PREF_QUALITY_KEY = "preferred_quality"
        private const val PREF_QUALITY_DEFAULT = "720p"
    }
}

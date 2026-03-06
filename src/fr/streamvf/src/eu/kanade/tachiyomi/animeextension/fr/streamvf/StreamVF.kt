package eu.kanade.tachiyomi.animeextension.fr.streamvf

import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.okruextractor.OkruExtractor
import eu.kanade.tachiyomi.lib.sendvidextractor.SendvidExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.streamwishextractor.StreamWishExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class StreamVF :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "StreamVF"

    override val baseUrl = "https://www.stream-vf.top"

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int) = GET("$baseUrl/latest-release?page=$page", headers)

    override fun popularAnimeSelector() = "div.manga-item"

    override fun popularAnimeFromElement(element: Element) = SAnime.create().apply {
        val a = element.selectFirst("h3.manga-heading a")!!
        setUrlWithoutDomain(a.attr("abs:href"))
        title = a.text()
        // Thumbnail is not directly available in manga-item on latest-release
        // We'll leave it empty or try to find a pattern
    }

    override fun popularAnimeNextPageSelector() = "ul.pagination li.active + li a"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int) = popularAnimeRequest(page)

    override fun latestUpdatesSelector() = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = GET("$baseUrl/search?query=$query", headers)

    override fun searchAnimeSelector() = "div.media"

    override fun searchAnimeFromElement(element: Element) = SAnime.create().apply {
        val a = element.selectFirst("h5.media-heading a")!!
        setUrlWithoutDomain(a.attr("abs:href"))
        title = a.text()
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun searchAnimeNextPageSelector() = null

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document) = SAnime.create().apply {
        val infoElement = document.selectFirst("div.row")
        title = document.selectFirst("h2.widget-title")?.text() ?: ""
        description = document.selectFirst("div.well p")?.text()
        thumbnail_url = document.selectFirst("img.img-responsive")?.attr("abs:src")

        val infostring = document.select("ul.manga-info li").joinToString { it.text() }
        genre = document.select("ul.manga-info li:contains(Genre) a").joinToString { it.text() }
        status = when {
            infostring.contains("En cours", true) -> SAnime.ONGOING
            infostring.contains("Terminé", true) -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector() = "ul.episodes li:has(a)"

    override fun episodeFromElement(element: Element) = SEpisode.create().apply {
        val a = element.selectFirst("a")!!
        setUrlWithoutDomain(a.attr("abs:href"))
        name = a.text()
        episode_number = element.selectFirst("em")?.text()?.toFloatOrNull() ?: 1f
        date_upload = element.selectFirst("div.date-episode-title-rtl")?.text()?.let {
            parseDate(it)
        } ?: 0L
    }

    private fun parseDate(dateStr: String): Long = try {
        val format = SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH)
        format.parse(dateStr.trim())?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    // ============================ Video Links =============================
    private val doodExtractor by lazy { DoodExtractor(client) }
    private val streamwishExtractor by lazy { StreamWishExtractor(client, headers) }
    private val sibnetExtractor by lazy { SibnetExtractor(client) }
    private val voeExtractor by lazy { VoeExtractor(client, headers) }
    private val sendvidExtractor by lazy { SendvidExtractor(client, headers) }
    private val okruExtractor by lazy { OkruExtractor(client) }

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val players = document.select("ul.nav-tabs li a[data-toggle=tab]")

        return players.parallelCatchingFlatMapBlocking {
            val serverName = it.text()
            val id = it.attr("href").removePrefix("#")
            val iframe = document.selectFirst("div#$id iframe")
            val url = iframe?.attr("abs:src") ?: ""

            getVideosFromUrl(url, serverName)
        }
    }

    private fun getVideosFromUrl(url: String, serverName: String): List<Video> = when {
        url.contains("dood") || url.contains("d0000d") -> doodExtractor.videosFromUrl(url)
        url.contains("streamwish") -> streamwishExtractor.videosFromUrl(url)
        url.contains("sibnet") -> sibnetExtractor.videosFromUrl(url)
        url.contains("voe") -> voeExtractor.videosFromUrl(url)
        url.contains("sendvid") -> sendvidExtractor.videosFromUrl(url)
        url.contains("ok.ru") -> okruExtractor.videosFromUrl(url)
        else -> emptyList()
    }

    override fun videoListSelector() = throw UnsupportedOperationException()
    override fun videoFromElement(element: Element) = throw UnsupportedOperationException()
    override fun videoUrlParse(document: Document) = throw UnsupportedOperationException()

    // ============================= Settings ==============================
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val videoQualityPref = ListPreference(screen.context).apply {
            key = "preferred_quality"
            title = "Qualité préférée"
            entries = arrayOf("1080p", "720p", "480p", "360p")
            entryValues = arrayOf("1080", "720", "480", "360")
            setDefaultValue("1080")
            summary = "%s"
        }
        screen.addPreference(videoQualityPref)
    }

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString("preferred_quality", "1080")!!
        return this.sortedWith(
            compareByDescending<Video> { it.quality.contains(quality) }
                .thenByDescending { it.quality },
        )
    }
}

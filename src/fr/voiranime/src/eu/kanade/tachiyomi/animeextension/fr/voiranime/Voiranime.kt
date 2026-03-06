package eu.kanade.tachiyomi.animeextension.fr.voiranime

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
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Voiranime :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "Voiranime"

    override val baseUrl = "https://voiranime.io"

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeSelector() = "div.listupd div.bsx"

    override fun popularAnimeRequest(page: Int) = GET("$baseUrl/series/page/$page/?order=popular")

    override fun popularAnimeFromElement(element: Element) = SAnime.create().apply {
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        title = element.selectFirst("a")!!.attr("title")
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    override fun popularAnimeNextPageSelector() = "div.hpage a.next"

    // =============================== Latest ===============================
    override fun latestUpdatesSelector() = popularAnimeSelector()

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/series/page/$page/?order=update")

    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeSelector() = popularAnimeSelector()

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = if (query.isNotEmpty()) {
        GET("$baseUrl/page/$page/?s=$query")
    } else {
        GET("$baseUrl/series/page/$page/")
    }

    override fun searchAnimeFromElement(element: Element) = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector() = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document) = SAnime.create().apply {
        title = document.selectFirst("h1.entry-title")?.text() ?: ""
        description = document.select("div.entry-content p").text()
        genre = document.select("div.genredot a").joinToString { it.text() }
        status = parseStatus(document.selectFirst("div.info-content span:contains(Status)")?.text())
    }

    private fun parseStatus(status: String?): Int = when {
        status == null -> SAnime.UNKNOWN
        status.contains("Ongoing", true) -> SAnime.ONGOING
        status.contains("Completed", true) -> SAnime.COMPLETED
        else -> SAnime.UNKNOWN
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector() = "div.eplister ul li"

    override fun episodeFromElement(element: Element) = SEpisode.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("href"))
        val epNum = element.selectFirst("div.epl-num")?.text() ?: ""
        val epTitle = element.selectFirst("div.epl-title")?.text() ?: ""
        name = if (epNum.isNotEmpty()) "$epNum - $epTitle" else epTitle
        episode_number = epNum.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
    }

    // ============================ Video Links =============================
    private val doodExtractor by lazy { DoodExtractor(client) }
    private val sibnetExtractor by lazy { SibnetExtractor(client) }
    private val sendvidExtractor by lazy { SendvidExtractor(client, headers) }
    private val vidMolyExtractor by lazy { VidMolyExtractor(client) }
    private val filemoonExtractor by lazy { FilemoonExtractor(client) }
    private val voeExtractor by lazy { VoeExtractor(client, headers) }

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        // Default iframes
        document.select("div#pembed iframe, div.player-embed iframe, div.megavid iframe, .video-content iframe").forEach {
            val src = it.attr("abs:src").ifEmpty { it.attr("src") }
            if (src.isNotEmpty()) {
                videos.addAll(extractVideos(src))
            }
        }

        val players = document.select("select#player-option option, select.player-option option, select.mirror option, .item-mirror select option")

        videos.addAll(
            players.toList().parallelCatchingFlatMapBlocking {
                val value = it.attr("value")
                if (value.isEmpty()) return@parallelCatchingFlatMapBlocking emptyList<Video>()

                val decoded = if (value.startsWith("http")) {
                    value
                } else {
                    try {
                        android.util.Base64.decode(value, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
                    } catch (e: Exception) {
                        ""
                    }
                }

                if (decoded.isEmpty()) return@parallelCatchingFlatMapBlocking emptyList<Video>()

                val iframeSrc = if (decoded.startsWith("http")) {
                    decoded
                } else {
                    Regex("""src=["']([^"']+)["']""").find(decoded)?.groupValues?.get(1) ?: ""
                }

                if (iframeSrc.isEmpty()) return@parallelCatchingFlatMapBlocking emptyList<Video>()

                extractVideos(iframeSrc)
            },
        )

        return videos.distinctBy { it.url }
    }

    private fun extractVideos(iframeSrc: String): List<Video> = when {
        iframeSrc.contains("vidmoly") -> vidMolyExtractor.videosFromUrl(iframeSrc)
        iframeSrc.contains("sibnet") -> sibnetExtractor.videosFromUrl(iframeSrc)
        iframeSrc.contains("sendvid") -> sendvidExtractor.videosFromUrl(iframeSrc)
        iframeSrc.contains("dood") -> doodExtractor.videosFromUrl(iframeSrc)
        iframeSrc.contains("filemoon") -> filemoonExtractor.videosFromUrl(iframeSrc)
        iframeSrc.contains("voe") -> voeExtractor.videosFromUrl(iframeSrc)
        else -> emptyList()
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

package eu.kanade.tachiyomi.animeextension.fr.vostfree

import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.luluextractor.LuluExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.uqloadextractor.UqloadExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Vostfree :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "Vostfree"

    override val baseUrl by lazy {
        preferences.getString(PREF_URL_KEY, PREF_URL_DEFAULT)!!
    }

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/page/$page/", headers)

    override fun popularAnimeSelector(): String = "div.movie-poster"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("div.title a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = link.text()
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = "span.pagi-next a"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/last-news/page/$page/", headers)

    override fun latestUpdatesSelector(): String = "div.movie-item"

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.startsWith(PREFIX_SEARCH)) {
            val id = query.removePrefix(PREFIX_SEARCH)
            return GET("$baseUrl/index.php?newsid=$id", headers)
        }
        val body = FormBody.Builder()
            .add("do", "search")
            .add("subaction", "search")
            .add("story", query)
            .add("search_start", page.toString())
            .build()
        return POST("$baseUrl/index.php?do=search", headers, body)
    }

    override fun searchAnimeSelector(): String = "div.movie-poster, div.movie-item"

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h1")?.text() ?: ""
        description = document.select(".desc").text()
        genre = document.select(".slide-middle p:contains(Genre) b a").joinToString { it.text() }
        thumbnail_url = document.selectFirst(".slide-trailer img")?.attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        val selectors = document.select("select.new_player_selector option")
        if (selectors.isNotEmpty()) {
            selectors.forEach { option ->
                val epNumText = option.text().lowercase().replace("épisode", "").trim()
                val sEp = SEpisode.create().apply {
                    name = "Épisode ${option.text()}"
                    episode_number = epNumText.toFloatOrNull() ?: 1f
                    url = "${response.request.url}|${option.attr("value")}"
                }
                episodes.add(sEp)
            }
        } else {
            val sEp = SEpisode.create().apply {
                name = "Épisode 1"
                episode_number = 1f
                url = "${response.request.url}|1"
            }
            episodes.add(sEp)
        }

        return episodes.reversed()
    }

    override fun episodeListSelector(): String = throw UnsupportedOperationException()
    override fun episodeFromElement(element: Element): SEpisode = throw UnsupportedOperationException()

    // ============================ Video Links =============================
    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val parts = episode.url.split("|")
        val pageUrl = parts[0]

        val response = client.newCall(GET(pageUrl, headers)).execute()
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        val sibnetExtractor = SibnetExtractor(client)
        val doodExtractor = DoodExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val uqloadExtractor = UqloadExtractor(client)
        val luluExtractor = LuluExtractor(client, headers)

        document.select(".player_box").forEach { box ->
            val content = box.text().trim()
            if (content.isNotBlank()) {
                videos.addAll(extractVideos(content, sibnetExtractor, doodExtractor, voeExtractor, vidMolyExtractor, uqloadExtractor, luluExtractor))
            }
        }

        return videos
    }

    private fun extractVideos(
        text: String,
        sibnet: SibnetExtractor,
        dood: DoodExtractor,
        voe: VoeExtractor,
        vidMoly: VidMolyExtractor,
        uqload: UqloadExtractor,
        lulu: LuluExtractor,
    ): List<Video> {
        val url = if (text.startsWith("//")) "https:$text" else text
        return when {
            url.contains("sibnet") || text.all { it.isDigit() } -> {
                val sibnetId = if (text.all { it.isDigit() }) text else url.substringAfter("videoid=").substringBefore("&")
                val sibnetUrl = "https://video.sibnet.ru/shell.php?videoid=$sibnetId"
                sibnet.videosFromUrl(sibnetUrl)
            }

            url.contains("dood") -> dood.videosFromUrl(url)

            url.contains("voe") -> voe.videosFromUrl(url)

            url.contains("vidmoly") -> vidMoly.videosFromUrl(url, "")

            url.contains("uqload") -> uqload.videosFromUrl(url)

            url.contains("luluvid") || url.contains("vidnest") || url.contains("vidzy") -> lulu.videosFromUrl(url, "")

            url.startsWith("http") -> listOf(Video(url, "Video", url))

            else -> emptyList()
        }
    }

    override fun videoListSelector(): String = throw UnsupportedOperationException()
    override fun videoFromElement(element: Element): Video = throw UnsupportedOperationException()
    override fun videoUrlParse(document: Document): String = throw UnsupportedOperationException()

    // ============================= Preferences ============================
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        EditTextPreference(screen.context).apply {
            key = PREF_URL_KEY
            title = "URL de base"
            setDefaultValue(PREF_URL_DEFAULT)
            summary = baseUrl
            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString(PREF_URL_KEY, newValue as String).commit()
            }
        }.also(screen::addPreference)
    }

    companion object {
        const val PREFIX_SEARCH = "id:"
        private const val PREF_URL_KEY = "preferred_baseUrl"
        private const val PREF_URL_DEFAULT = "https://vostfree.ws"
    }
}

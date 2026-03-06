package eu.kanade.tachiyomi.animeextension.fr.streamvf

import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class StreamVF :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "Stream-VF"

    override val baseUrl by lazy {
        preferences.getString(PREF_URL_KEY, PREF_URL_DEFAULT)!!
    }

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/anime-list?page=$page&sortBy=views", headers)

    override fun popularAnimeSelector(): String = "ul.hot-thumbnails li, div.media"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = (element.selectFirst(".manga-name") ?: element.selectFirst(".media-heading"))?.text() ?: ""
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = "ul.pagination li:last-child a"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/latest-release?page=$page", headers)

    override fun latestUpdatesSelector(): String = "div.manga-item"

    override fun latestUpdatesFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("h3 a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = link.text()
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.startsWith(PREFIX_SEARCH)) {
            val id = query.removePrefix(PREFIX_SEARCH)
            return GET("$baseUrl/anime/$id", headers)
        }
        return GET("$baseUrl/search?query=$query", headers)
    }

    override fun searchAnimeSelector(): String = "div.media"

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String? = null

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h2.widget-title")?.text() ?: ""
        description = document.select("div.well p").text()
        genre = document.select("div.manga-info a.label").joinToString { it.text() }
        thumbnail_url = document.selectFirst("img.img-responsive")?.attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector(): String = "ul.episodes li:not(.volume)"

    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        val epNumText = element.selectFirst("em")?.text() ?: "1"
        name = "Épisode $epNumText: ${link.text()}"
        episode_number = epNumText.toFloatOrNull() ?: 0f
        date_upload = 0L // TODO: Parse date if needed
    }

    override fun episodeListParse(response: Response): List<SEpisode> = super.episodeListParse(response).reversed()

    // ============================ Video Links =============================
    override fun videoListSelector(): String = "iframe"

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val response = client.newCall(GET(baseUrl + episode.url, headers)).execute()
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        val sibnetExtractor = SibnetExtractor(client)
        val doodExtractor = DoodExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val filemoonExtractor = FilemoonExtractor(client)

        document.select("iframe").forEach { iframe ->
            val url = iframe.attr("abs:src")
            when {
                url.contains("sibnet") -> videos.addAll(sibnetExtractor.videosFromUrl(url))
                url.contains("dood") -> videos.addAll(doodExtractor.videosFromUrl(url))
                url.contains("voe") -> videos.addAll(voeExtractor.videosFromUrl(url))
                url.contains("vidmoly") -> videos.addAll(vidMolyExtractor.videosFromUrl(url))
                url.contains("filemoon") -> videos.addAll(filemoonExtractor.videosFromUrl(url))
            }
        }

        // Check for scripts if iframes are empty
        if (videos.isEmpty()) {
            val scriptData = document.select("script").joinToString { it.data() }
            val urlRegex = Regex("""https?://[^\s'"]+""")
            urlRegex.findAll(scriptData).forEach { match ->
                val url = match.value
                when {
                    url.contains("sibnet") -> videos.addAll(sibnetExtractor.videosFromUrl(url))
                    url.contains("dood") -> videos.addAll(doodExtractor.videosFromUrl(url))
                    url.contains("voe") -> videos.addAll(voeExtractor.videosFromUrl(url))
                    url.contains("vidmoly") -> videos.addAll(vidMolyExtractor.videosFromUrl(url))
                    url.contains("filemoon") -> videos.addAll(filemoonExtractor.videosFromUrl(url))
                }
            }
        }

        return videos
    }

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
        private const val PREF_URL_DEFAULT = "https://www.stream-vf.top"
    }
}

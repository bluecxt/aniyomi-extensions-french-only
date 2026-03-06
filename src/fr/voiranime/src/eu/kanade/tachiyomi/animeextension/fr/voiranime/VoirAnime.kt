package eu.kanade.tachiyomi.animeextension.fr.voiranime

import androidx.preference.EditTextPreference
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
import eu.kanade.tachiyomi.lib.okruextractor.OkruExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.vkextractor.VkExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class VoirAnime :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "VoirAnime"

    override val baseUrl by lazy {
        preferences.getString(PREF_URL_KEY, PREF_URL_DEFAULT)!!
    }

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/page/$page/?m_orderby=views", headers)

    override fun popularAnimeSelector(): String = ".c-tabs-item__content .page-item-detail, .search-wrap .page-item-detail"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        title = element.select(".post-title a").text()
        setUrlWithoutDomain(element.select(".post-title a").attr("href"))
        thumbnail_url = element.select("img").attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = ".nextpostslink"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/page/$page/?m_orderby=latest", headers)

    override fun latestUpdatesSelector(): String = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.startsWith(PREFIX_SEARCH)) {
            val id = query.removePrefix(PREFIX_SEARCH)
            return GET("$baseUrl/anime/$id", headers)
        }
        val url = "$baseUrl/page/$page/".toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .addQueryParameter("post_type", "wp-manga")
        return GET(url.build().toString(), headers)
    }

    override fun searchAnimeSelector(): String = ".search-wrap .page-item-detail, .c-tabs-item__content .page-item-detail"

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.select(".post-title h1").text()
        description = document.select(".description-summary .summary__content").text()
        genre = document.select(".genres-content a").joinToString { it.text() }
        status = when (document.select(".post-status .summary-content").text().lowercase()) {
            "en cours" -> SAnime.ONGOING
            "terminé" -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
        thumbnail_url = document.select(".summary_image img").attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector(): String = ".listing-chapters_wrap .wp-manga-chapter"

    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create().apply {
        val link = element.select("a")
        name = link.text()
        setUrlWithoutDomain(link.attr("href"))
        // Madara usually lists chapters/episodes by date, we can try to parse it
        // date_upload = ...
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        // Madara sometimes uses AJAX for episode list
        val mangaId = document.select("#manga-chapters-holder").attr("data-id")
        if (mangaId.isNotEmpty()) {
            val ajaxUrl = "$baseUrl/wp-admin/admin-ajax.php"
            val body = FormBody.Builder()
                .add("action", "manga_get_chapters")
                .add("manga", mangaId)
                .build()
            val ajaxResponse = client.newCall(POST(ajaxUrl, headers, body)).execute()
            val ajaxDocument = ajaxResponse.asJsoup()
            ajaxDocument.select(episodeListSelector()).forEach {
                episodes.add(episodeFromElement(it))
            }
        } else {
            document.select(episodeListSelector()).forEach {
                episodes.add(episodeFromElement(it))
            }
        }

        return episodes.reversed()
    }

    // ============================ Video Links =============================
    override fun videoListSelector(): String = ".player-embed iframe, .video-player iframe"

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val response = client.newCall(GET(baseUrl + episode.url, headers)).execute()
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        val doodExtractor = DoodExtractor(client)
        val sibnetExtractor = SibnetExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val okruExtractor = OkruExtractor(client)
        val vkExtractor = VkExtractor(client, headers)

        // Madara video players are often in iframes or script tags
        document.select("iframe").forEach { iframe ->
            val url = iframe.attr("abs:src")
            videos.addAll(extractVideos(url, doodExtractor, sibnetExtractor, voeExtractor, vidMolyExtractor, okruExtractor, vkExtractor))
        }

        // Sometimes URLs are in scripts
        val scriptData = document.select("script").joinToString { it.data() }
        val urlRegex = Regex("""https?://[^\s'"]+""")
        urlRegex.findAll(scriptData).forEach { match ->
            val url = match.value
            if (url.contains("dood") || url.contains("sibnet") || url.contains("voe") || url.contains("vidmoly") || url.contains("ok.ru") || url.contains("vk.com")) {
                videos.addAll(extractVideos(url, doodExtractor, sibnetExtractor, voeExtractor, vidMolyExtractor, okruExtractor, vkExtractor))
            }
        }

        return videos
    }

    private fun extractVideos(
        url: String,
        dood: DoodExtractor,
        sibnet: SibnetExtractor,
        voe: VoeExtractor,
        vidMoly: VidMolyExtractor,
        okru: OkruExtractor,
        vk: VkExtractor,
    ): List<Video> = when {
        url.contains("dood") -> dood.videosFromUrl(url)
        url.contains("sibnet") -> sibnet.videosFromUrl(url)
        url.contains("voe") -> voe.videosFromUrl(url)
        url.contains("vidmoly") -> vidMoly.videosFromUrl(url)
        url.contains("ok.ru") -> okru.videosFromUrl(url)
        url.contains("vk.com") -> vk.videosFromUrl(url)
        else -> emptyList()
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
        private const val PREF_URL_DEFAULT = "https://v6.voiranime.com"
    }
}

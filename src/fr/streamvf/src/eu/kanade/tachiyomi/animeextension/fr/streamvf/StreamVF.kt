package eu.kanade.tachiyomi.animeextension.fr.streamvf

import android.util.Base64
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.cloudflareinterceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor
import eu.kanade.tachiyomi.lib.luluextractor.LuluExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
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

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(CloudflareInterceptor(network.client))
        .build()

    // Client sans intercepteur pour les redirections simples
    private val simpleClient = network.client.newBuilder()
        .followRedirects(true)
        .build()

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

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

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
        date_upload = 0L
    }

    override fun episodeListParse(response: Response): List<SEpisode> = super.episodeListParse(response)

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
        val luluExtractor = LuluExtractor(client, headers)

        val scripts = document.select("script").joinToString { it.data() }
        val fRegex = Regex("""f\('([^']+)'\)""")

        val allUrls = mutableListOf<String>()
        document.select("iframe").forEach { allUrls.add(it.attr("abs:src")) }

        fRegex.findAll(scripts).forEach { match ->
            val encoded = match.groupValues[1]
            try {
                val decoded = Base64.decode(encoded, Base64.DEFAULT).toString(Charsets.UTF_8)
                // On cherche src="URL" dans le texte décodé
                val srcMatch = Regex("""src=["']([^"']+)["']""").find(decoded)
                srcMatch?.groupValues?.get(1)?.let { allUrls.add(it) }
            } catch (e: Exception) {}
        }

        allUrls.filter { it.isNotBlank() }.distinct().forEach { url ->
            val resolvedUrl = resolveUrl(url)
            val extracted = extractVideos(resolvedUrl, sibnetExtractor, doodExtractor, voeExtractor, vidMolyExtractor, filemoonExtractor, luluExtractor, "")
            videos.addAll(extracted)
        }

        return videos.sortedByDescending { it.quality.contains("Sibnet", ignoreCase = true) }.distinctBy { it.videoUrl }
    }

    private fun resolveUrl(url: String): String {
        val absoluteUrl = when {
            url.startsWith("//") -> "https:$url"
            !url.startsWith("http") -> "https://$url"
            else -> url
        }

        if (absoluteUrl.contains("short.icu")) {
            return try {
                // On utilise le simpleClient pour éviter les conflits d'intercepteurs sur les raccourcisseurs
                val response = simpleClient.newCall(GET(absoluteUrl)).execute()
                response.request.url.toString()
            } catch (e: Exception) {
                absoluteUrl
            }
        }
        return absoluteUrl
    }

    private fun extractVideos(
        url: String,
        sibnet: SibnetExtractor,
        dood: DoodExtractor,
        voe: VoeExtractor,
        vidMoly: VidMolyExtractor,
        filemoon: FilemoonExtractor,
        lulu: LuluExtractor,
        suffix: String,
    ): List<Video> = when {
        url.contains("sibnet") -> sibnet.videosFromUrl(url, "Sibnet $suffix - ")

        url.contains("dood") -> dood.videosFromUrl(url, "Dood $suffix - ")

        url.contains("voe") -> voe.videosFromUrl(url, "Voe $suffix - ")

        url.contains("vidmoly") -> vidMoly.videosFromUrl(url, "VidMoly $suffix - ")

        url.contains("filemoon") -> filemoon.videosFromUrl(url, "Filemoon $suffix - ")

        url.contains("lulu") || url.contains("vidnest") || url.contains("vidzy") -> {
            lulu.videosFromUrl(url, "Lulu/Vidnest $suffix - ")
        }

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
        private const val PREF_URL_DEFAULT = "https://www.stream-vf.top"
    }
}

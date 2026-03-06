package eu.kanade.tachiyomi.animeextension.fr.adkami

import android.util.Base64
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ADKami :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "ADKami"

    override val baseUrl by lazy {
        preferences.getString(PREF_URL_KEY, PREF_URL_DEFAULT)!!
    }

    override val lang = "fr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(CloudflareInterceptor(network.client))
        .build()

    private val preferences by getPreferencesLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/hentai-streaming?page=$page", headers)

    override fun popularAnimeSelector(): String = "div.h-card"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = element.selectFirst(".title")?.text()?.trim() ?: ""
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = "span.pagi-next a"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = popularAnimeRequest(page)

    override fun latestUpdatesSelector(): String = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = GET("$baseUrl/hentai-streaming?search=$query&page=$page", headers)

    override fun searchAnimeSelector(): String = popularAnimeSelector()

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h1.title-header-video")?.text()?.substringBefore(" - Episode")
            ?: document.selectFirst(".fiche-info h1")?.text() ?: ""
        description = document.select("#look-video br").first()?.nextSibling()?.toString()?.trim()
            ?: document.select(".fiche-info h4[itemprop=alternateName]").next().text() ?: ""
        genre = document.select("a.label span[itemprop=genre]").joinToString { it.text() }
        thumbnail_url = document.selectFirst("img.video-image")?.attr("abs:src")
            ?: document.selectFirst(".fiche-info img")?.attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        val episodeLinks = document.select("#row-nav-episode .ul-episodes a")
        if (episodeLinks.isNotEmpty()) {
            episodeLinks.forEach { a ->
                val epName = a.text().trim()
                val sEp = SEpisode.create().apply {
                    name = epName
                    episode_number = epName.substringAfter("Episode").trim().substringBefore(" ").toFloatOrNull() ?: 0f
                    setUrlWithoutDomain(a.attr("abs:href"))
                }
                episodes.add(sEp)
            }
        } else {
            val sEp = SEpisode.create().apply {
                name = document.selectFirst("h1.title-header-video")?.text() ?: "Épisode 1"
                episode_number = 1f
                url = response.request.url.toString().removePrefix(baseUrl)
            }
            episodes.add(sEp)
        }

        return episodes.reversed()
    }

    override fun episodeListSelector(): String = throw UnsupportedOperationException()
    override fun episodeFromElement(element: Element): SEpisode = throw UnsupportedOperationException()

    // ============================ Video Links =============================
    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val response = client.newCall(GET(baseUrl + episode.url, headers)).execute()
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        val sibnetExtractor = SibnetExtractor(client)
        val doodExtractor = DoodExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val luluExtractor = LuluExtractor(client, headers)
        val filemoonExtractor = FilemoonExtractor(client)

        document.select("div.video-iframe").forEach { block ->
            val encodedUrl = block.attr("data-url")
            val name = block.attr("data-name")

            if (encodedUrl.isNotBlank()) {
                val decodedUrl = decodeAdkamiUrl(encodedUrl)
                if (decodedUrl != null) {
                    when {
                        decodedUrl.contains("sibnet") -> videos.addAll(sibnetExtractor.videosFromUrl(decodedUrl, "$name - "))

                        decodedUrl.contains("dood") -> videos.addAll(doodExtractor.videosFromUrl(decodedUrl, "$name - "))

                        decodedUrl.contains("voe") -> videos.addAll(voeExtractor.videosFromUrl(decodedUrl, "$name - "))

                        decodedUrl.contains("vidmoly") -> videos.addAll(vidMolyExtractor.videosFromUrl(decodedUrl, "$name - "))

                        decodedUrl.contains("luluvid") || decodedUrl.contains("vidnest") || decodedUrl.contains("vidzy") -> {
                            videos.addAll(luluExtractor.videosFromUrl(decodedUrl, "$name - "))
                        }

                        decodedUrl.contains("filemoon") -> videos.addAll(filemoonExtractor.videosFromUrl(decodedUrl, "$name - "))

                        else -> videos.add(Video(decodedUrl, name, decodedUrl))
                    }
                }
            }
        }

        return videos
    }

    private fun decodeAdkamiUrl(encodedUrl: String): String? {
        val part = encodedUrl.substringAfter("embed/", "")
        if (part.isBlank()) return null

        return try {
            val e = String(Base64.decode(part, Base64.DEFAULT), Charsets.ISO_8859_1)
            var t = ""
            val n = "ETEfazefzeaZa13MnZEe"
            var i = 0
            for (o in e) {
                t += ((175 xor o.code) - n[i].code).toChar()
                i = if (i > n.length - 2) 0 else i + 1
            }
            t
        } catch (e: Exception) {
            null
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
        private const val PREF_URL_DEFAULT = "https://hentai.adkami.com"
    }
}

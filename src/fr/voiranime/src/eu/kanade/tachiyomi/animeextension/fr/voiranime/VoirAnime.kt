package eu.kanade.tachiyomi.animeextension.fr.voiranime

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
import eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor
import eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor
import eu.kanade.tachiyomi.lib.okruextractor.OkruExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.vkextractor.VkExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import keiyoushi.utils.getPreferencesLazy
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
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
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/series/?page=$page&order=popular", headers)

    override fun popularAnimeSelector(): String = "div.listupd article.bs"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = link.selectFirst(".tt")!!.ownText()
        thumbnail_url = link.selectFirst("img")?.attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = "div.pagination a.next"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/series/?page=$page&order=update", headers)

    override fun latestUpdatesSelector(): String = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.startsWith(PREFIX_SEARCH)) {
            val id = query.removePrefix(PREFIX_SEARCH)
            return GET("$baseUrl/series/$id", headers)
        }
        return GET("$baseUrl/page/$page/?s=$query", headers)
    }

    override fun searchAnimeSelector(): String = "div.listupd article.bs, .result-item article"

    override fun searchAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = (element.selectFirst(".tt") ?: element.selectFirst(".title")).let { it!!.ownText() }
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h1.entry-title")!!.text()
        description = document.select(".entry-content[itemprop=description]").text()
        genre = document.select(".genxed a").joinToString { it.text() }
        status = when (document.select("span:contains(Status) i").text().lowercase()) {
            "ongoing", "en cours" -> SAnime.ONGOING
            "completed", "terminé" -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
        thumbnail_url = document.selectFirst(".thumb img")?.attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListSelector(): String = "div.eplister ul li a"

    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create().apply {
        setUrlWithoutDomain(element.attr("abs:href"))
        val epNum = element.selectFirst(".epl-num")?.text() ?: "1"
        name = "Épisode $epNum"
        episode_number = epNum.toFloatOrNull() ?: 0f
        scanlator = element.selectFirst(".epl-sub")?.text()
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        return document.select(episodeListSelector()).map(::episodeFromElement).reversed()
    }

    // ============================ Video Links =============================
    override fun videoListSelector(): String = "select.mirror option[data-index]"

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val response = client.newCall(GET(baseUrl + episode.url, headers)).execute()
        val document = response.asJsoup()

        val doodExtractor = DoodExtractor(client)
        val sibnetExtractor = SibnetExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val okruExtractor = OkruExtractor(client)
        val vkExtractor = VkExtractor(client, headers)
        val filemoonExtractor = FilemoonExtractor(client)

        val items = document.select(videoListSelector())
        return items.parallelCatchingFlatMapBlocking { element ->
            val encodedData = element.attr("value")
            if (encodedData.isBlank()) return@parallelCatchingFlatMapBlocking emptyList()

            val decodedHtml = try {
                Base64.decode(encodedData, Base64.DEFAULT).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                return@parallelCatchingFlatMapBlocking emptyList()
            }

            val iframeUrl = Jsoup.parse(decodedHtml).selectFirst("iframe")?.attr("src")
                ?: return@parallelCatchingFlatMapBlocking emptyList()

            extractVideos(iframeUrl, doodExtractor, sibnetExtractor, voeExtractor, vidMolyExtractor, okruExtractor, vkExtractor, filemoonExtractor)
        }
    }

    private fun extractVideos(
        url: String,
        dood: DoodExtractor,
        sibnet: SibnetExtractor,
        voe: VoeExtractor,
        vidMoly: VidMolyExtractor,
        okru: OkruExtractor,
        vk: VkExtractor,
        filemoon: FilemoonExtractor,
    ): List<Video> {
        val absoluteUrl = if (url.startsWith("//")) "https:$url" else url
        return when {
            absoluteUrl.contains("dood") -> dood.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("sibnet") -> sibnet.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("voe") -> voe.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("vidmoly") -> vidMoly.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("ok.ru") -> okru.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("vk.com") -> vk.videosFromUrl(absoluteUrl)
            absoluteUrl.contains("filemoon") -> filemoon.videosFromUrl(absoluteUrl)
            else -> emptyList()
        }
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
        private const val PREF_URL_DEFAULT = "https://voiranime.io"
    }
}

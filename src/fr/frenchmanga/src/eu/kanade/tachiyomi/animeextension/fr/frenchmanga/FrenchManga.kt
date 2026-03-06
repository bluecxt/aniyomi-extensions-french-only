package eu.kanade.tachiyomi.animeextension.fr.frenchmanga

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
import eu.kanade.tachiyomi.lib.luluextractor.LuluExtractor
import eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor
import eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class FrenchManga :
    ParsedAnimeHttpSource(),
    ConfigurableAnimeSource {

    override val name = "French-Manga"

    override val baseUrl by lazy {
        preferences.getString(PREF_URL_KEY, PREF_URL_DEFAULT)!!
    }

    override val lang = "fr"

    override val supportsLatest = true

    private val preferences by getPreferencesLazy()

    private val json: Json by injectLazy()

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/manga-streaming/page/$page/", headers)

    override fun popularAnimeSelector(): String = "div.short"

    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        val link = element.selectFirst("a.short-poster")!!
        setUrlWithoutDomain(link.attr("abs:href"))
        title = element.selectFirst(".short-title")!!.text()
        thumbnail_url = link.selectFirst("img")?.attr("abs:src")
    }

    override fun popularAnimeNextPageSelector(): String = ".pagi-nav a:contains(Suivant)"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/page/$page/", headers)

    override fun latestUpdatesSelector(): String = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.startsWith(PREFIX_SEARCH)) {
            val id = query.removePrefix(PREFIX_SEARCH)
            return GET("$baseUrl/index.php?newsid=$id", headers)
        }
        return GET("$baseUrl/index.php?do=search&subaction=search&story=$query&search_start=$page", headers)
    }

    override fun searchAnimeSelector(): String = popularAnimeSelector()

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h1")?.text() ?: ""
        description = document.select(".full-story").text()
        genre = document.select(".full-inf li:contains(Genre) a").joinToString { it.text() }
        thumbnail_url = document.selectFirst(".full-poster img")?.attr("abs:src")
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val newsId = response.request.url.queryParameter("newsid")
            ?: document.selectFirst("input[name=post_id]")?.attr("value")
            ?: return emptyList()

        val ajaxUrl = "$baseUrl/engine/ajax/manga_episodes_api.php?id=$newsId"
        val ajaxResponse = client.newCall(GET(ajaxUrl, headers)).execute()
        val jsonResponse = json.parseToJsonElement(ajaxResponse.body.string()).jsonObject

        val episodes = mutableListOf<SEpisode>()

        listOf("vf", "vostfr").forEach { langType ->
            jsonResponse[langType]?.jsonObject?.forEach { (epNum, _) ->
                val sEp = SEpisode.create().apply {
                    val actualEpNum = epNum.toFloatOrNull() ?: 0f
                    episode_number = actualEpNum
                    name = "Épisode $epNum (${langType.uppercase()})"
                    url = "$newsId|$langType|$epNum"
                }
                episodes.add(sEp)
            }
        }

        return episodes.sortedByDescending { it.episode_number }
    }

    override fun episodeListSelector(): String = throw UnsupportedOperationException()
    override fun episodeFromElement(element: Element): SEpisode = throw UnsupportedOperationException()

    // ============================ Video Links =============================
    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val parts = episode.url.split("|")
        if (parts.size < 3) return emptyList()

        val newsId = parts[0]
        val langType = parts[1]
        val epNum = parts[2]

        val ajaxUrl = "$baseUrl/engine/ajax/manga_episodes_api.php?id=$newsId"
        val ajaxResponse = client.newCall(GET(ajaxUrl, headers)).execute()
        val jsonResponse = json.parseToJsonElement(ajaxResponse.body.string()).jsonObject

        val hosters = jsonResponse[langType]?.jsonObject?.get(epNum)?.jsonObject ?: return emptyList()
        val videos = mutableListOf<Video>()

        val luluExtractor = LuluExtractor(client, headers)
        val doodExtractor = DoodExtractor(client)
        val voeExtractor = VoeExtractor(client, headers)
        val sibnetExtractor = SibnetExtractor(client)
        val vidMolyExtractor = VidMolyExtractor(client, headers)
        val filemoonExtractor = FilemoonExtractor(client)

        hosters.forEach { (_, hosterUrlElement) ->
            val hosterUrl = hosterUrlElement.toString().trim('"')
            when {
                hosterUrl.contains("luluvid") || hosterUrl.contains("vidnest") -> {
                    videos.addAll(luluExtractor.videosFromUrl(hosterUrl, ""))
                }

                hosterUrl.contains("dood") -> {
                    videos.addAll(doodExtractor.videosFromUrl(hosterUrl))
                }

                hosterUrl.contains("voe") -> {
                    videos.addAll(voeExtractor.videosFromUrl(hosterUrl))
                }

                hosterUrl.contains("sibnet") -> {
                    videos.addAll(sibnetExtractor.videosFromUrl(hosterUrl))
                }

                hosterUrl.contains("vidmoly") -> {
                    videos.addAll(vidMolyExtractor.videosFromUrl(hosterUrl, ""))
                }

                hosterUrl.contains("filemoon") -> {
                    videos.addAll(filemoonExtractor.videosFromUrl(hosterUrl))
                }
            }
        }

        return videos
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
        private const val PREF_URL_DEFAULT = "https://w16.french-manga.net"
    }
}

package eu.kanade.tachiyomi.animeextension.fr.adkami

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class Adkami : ParsedAnimeHttpSource() {

    override val name = "Adkami"

    override val baseUrl = "https://www.adkami.com"

    override val lang = "fr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client

    // ============================== Popular ===============================

    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/anime")

    override fun popularAnimeSelector(): String = "div.video-item-list"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        element.select("span.top a").let {
            anime.setUrlWithoutDomain(it.attr("abs:href"))
            anime.title = it.select("span.title").text()
        }
        anime.thumbnail_url = element.select("img").attr("abs:data-original").ifEmpty { element.select("img").attr("abs:src") }
        return anime
    }

    override fun popularAnimeNextPageSelector(): String? = null

    // =============================== Latest ===============================

    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl")

    override fun latestUpdatesSelector(): String = "div.video-item-list"

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // =============================== Search ===============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = GET("$baseUrl/video?search=$query")

    override fun searchAnimeSelector(): String = popularAnimeSelector()

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String? = null

    // =========================== Anime Details ============================

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.selectFirst("h1.title-header-video")?.text() ?: ""
        anime.description = document.select("p.description").text()
        anime.genre = document.select("span[itemprop=genre]").joinToString { it.text() }
        anime.author = document.select("b[itemprop=author]").text()
        return anime
    }

    // ============================== Episodes ==============================

    override fun episodeListSelector(): String = "div.ul-episodes li:not(.saison)"

    override fun episodeFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        val link = element.select("a").first()!!
        episode.setUrlWithoutDomain(link.attr("abs:href"))
        val title = link.text()

        episode.name = title

        val epNum = title.lowercase().substringAfter("episode").trim().takeWhile { it.isDigit() || it == '.' }
        episode.episode_number = epNum.toFloatOrNull() ?: 1f

        return episode
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        val items = document.select(episodeListSelector())
        if (items.isNotEmpty()) {
            val grouped = items.map { episodeFromElement(it) }.groupBy { it.episode_number }
            grouped.forEach { (num, eps) ->
                val mainEp = eps.first()
                if (eps.size > 1) {
                    mainEp.name = "Episode ${num.toInt()} (VOSTFR / VF)"
                }
                episodes.add(mainEp)
            }
        } else {
            val episode = SEpisode.create()
            episode.setUrlWithoutDomain(response.request.url.toString())
            episode.name = document.select("h1.title-header-video").text().ifEmpty { "Episode 1" }
            episode.episode_number = 1f
            episodes.add(episode)
        }

        return episodes.reversed()
    }

    // ============================ Video Links =============================

    override fun videoListSelector(): String = "li.dooplay_player_option"

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        // DooPlay options
        document.select(videoListSelector()).forEach { option ->
            val post = option.attr("data-post")
            val num = option.attr("data-nume")
            val type = option.attr("data-type")

            val body = FormBody.Builder()
                .add("action", "doo_player_ajax")
                .add("post", post)
                .add("nume", num)
                .add("type", type)
                .build()

            val ajaxHeaders = headers.newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .build()

            val ajaxResponse = client.newCall(POST("$baseUrl/wp-admin/admin-ajax.php", ajaxHeaders, body)).execute()
            if (ajaxResponse.isSuccessful) {
                val ajaxHtml = ajaxResponse.body.string()
                val iframeUrl = Jsoup.parse(ajaxHtml).select("iframe").attr("abs:src").ifEmpty { Jsoup.parse(ajaxHtml).select("iframe").attr("src") }
                if (iframeUrl.isNotEmpty()) {
                    videos.addAll(extractVideos(iframeUrl))
                }
            }
        }

        // Fallback to iframes in page
        document.select("iframe").forEach { iframe ->
            val url = iframe.attr("abs:src").ifEmpty { iframe.attr("src") }
            if (url.isEmpty() || url.contains("youtube") || url.contains("google") || url.contains("adkami")) return@forEach
            videos.addAll(extractVideos(url))
        }

        return videos
    }

    private fun extractVideos(url: String): List<Video> {
        val videos = mutableListOf<Video>()
        when {
            url.contains("dood") -> videos.addAll(eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor(client).videosFromUrl(url))
            url.contains("sibnet") -> videos.addAll(eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor(client).videosFromUrl(url))
            url.contains("voe") -> videos.addAll(eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor(client, headers).videosFromUrl(url))
            url.contains("filemoon") -> videos.addAll(eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor(client).videosFromUrl(url))
            url.contains("sendvid") -> videos.addAll(eu.kanade.tachiyomi.lib.sendvidextractor.SendvidExtractor(client, headers).videosFromUrl(url))
        }
        return videos
    }

    override fun videoFromElement(element: Element): Video = throw Exception("Not used")

    override fun videoUrlParse(document: Document): String = throw Exception("Not used")
}

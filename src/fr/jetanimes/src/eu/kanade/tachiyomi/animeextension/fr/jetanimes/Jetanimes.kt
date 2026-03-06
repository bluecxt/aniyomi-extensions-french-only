package eu.kanade.tachiyomi.animeextension.fr.jetanimes

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class Jetanimes : ParsedAnimeHttpSource() {

    override val name = "Jetanimes"

    override val baseUrl = "https://on.jetanimes.com"

    override val lang = "fr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client

    // ============================== Popular ===============================

    override fun popularAnimeRequest(page: Int): Request = if (page == 1) {
        GET("$baseUrl/series/")
    } else {
        GET("$baseUrl/series/page/$page/")
    }

    override fun popularAnimeSelector(): String = "article.item.tvshows"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        element.select("div.data h3 a").let {
            anime.setUrlWithoutDomain(it.attr("abs:href"))
            anime.title = it.text()
        }
        anime.thumbnail_url = element.select("div.poster img").attr("abs:src")
        return anime
    }

    override fun popularAnimeNextPageSelector(): String = "a.next"

    // =============================== Latest ===============================

    override fun latestUpdatesRequest(page: Int): Request = if (page == 1) {
        GET("$baseUrl/episodes/")
    } else {
        GET("$baseUrl/episodes/page/$page/")
    }

    override fun latestUpdatesSelector(): String = "article.item.episodes"

    override fun latestUpdatesFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        element.select("div.data h3 a").let {
            anime.setUrlWithoutDomain(it.attr("abs:href"))
            anime.title = it.text()
        }
        anime.thumbnail_url = element.select("div.poster img").attr("abs:src")
        return anime
    }

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = if (page == 1) {
        GET("$baseUrl/?s=$query")
    } else {
        GET("$baseUrl/page/$page/?s=$query")
    }

    override fun searchAnimeSelector(): String = "div.result-item article"

    override fun searchAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        element.select("div.details div.title a").let {
            anime.setUrlWithoutDomain(it.attr("abs:href"))
            anime.title = it.text()
        }
        anime.thumbnail_url = element.select("div.image div.poster img").attr("abs:src")
        return anime
    }

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.selectFirst("div.data h1")?.text() ?: ""
        anime.description = document.select("div.wp-content p").text()
        anime.genre = document.select("div.sgeneros a").joinToString { it.text() }
        anime.status = parseStatus(document.select("div.custom_fields b:contains(Status) + span").text())
        return anime
    }

    private fun parseStatus(status: String): Int = when {
        status.contains("En cours", true) -> SAnime.ONGOING
        status.contains("Terminé", true) -> SAnime.COMPLETED
        else -> SAnime.UNKNOWN
    }

    // ============================== Episodes ==============================

    override fun episodeListSelector(): String = "ul.episodios li"

    override fun episodeFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        val link = element.select("div.episodiotitle a").first()!!
        episode.setUrlWithoutDomain(link.attr("abs:href"))

        val title = link.text()
        val num = element.select("div.numerando").text() // e.g. "1 - 1"

        episode.name = if (num.isNotEmpty()) "S$num - $title" else title

        // Try to parse episode number
        val epNum = num.substringAfter("-").trim()
        episode.episode_number = epNum.toFloatOrNull() ?: 1f

        return episode
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        val items = document.select(episodeListSelector())
        if (items.isNotEmpty()) {
            items.forEach { episodes.add(episodeFromElement(it)) }
        } else {
            // Check for direct video page (DooPlay episodes)
            val episode = SEpisode.create()
            episode.setUrlWithoutDomain(response.request.url.toString())
            episode.name = document.select("h1.epih1").text().ifEmpty { document.select("h1").text() }
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

        // DooPlay uses AJAX to load player, but often has options in the HTML
        document.select(videoListSelector()).forEach { option ->
            val post = option.attr("data-post")
            val num = option.attr("data-nume")
            val type = option.attr("data-type")

            // If we can't do AJAX here, we try to find iframes directly
        }

        // Fallback to iframes
        document.select("iframe, .embed-container iframe").forEach { iframe ->
            val url = iframe.attr("abs:src").ifEmpty { iframe.attr("src") }
            if (url.isEmpty() || url.contains("youtube")) return@forEach

            when {
                url.contains("gupy.fr") -> videos.add(Video(url, "Gupy", url))
                url.contains("dood") -> videos.addAll(eu.kanade.tachiyomi.lib.doodextractor.DoodExtractor(client).videosFromUrl(url))
                url.contains("sibnet") -> videos.addAll(eu.kanade.tachiyomi.lib.sibnetextractor.SibnetExtractor(client).videosFromUrl(url))
                url.contains("voe") -> videos.addAll(eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor(client, headers).videosFromUrl(url))
                url.contains("filemoon") -> videos.addAll(eu.kanade.tachiyomi.lib.filemoonextractor.FilemoonExtractor(client).videosFromUrl(url))
                url.contains("sendvid") -> videos.addAll(eu.kanade.tachiyomi.lib.sendvidextractor.SendvidExtractor(client, headers).videosFromUrl(url))
                url.contains("vidmoly") -> videos.addAll(eu.kanade.tachiyomi.lib.vidmolyextractor.VidMolyExtractor(client).videosFromUrl(url))
                url.contains("upstream") -> videos.addAll(eu.kanade.tachiyomi.lib.upstreamextractor.UpstreamExtractor(client).videosFromUrl(url))
                url.contains("vido") -> videos.addAll(eu.kanade.tachiyomi.lib.vidoextractor.VidoExtractor(client).videosFromUrl(url))
                url.contains("uqload") -> videos.addAll(eu.kanade.tachiyomi.lib.uqloadextractor.UqloadExtractor(client).videosFromUrl(url))
                url.contains("vudeo") -> videos.addAll(eu.kanade.tachiyomi.lib.vudeoextractor.VudeoExtractor(client).videosFromUrl(url))
            }
        }

        return videos
    }

    override fun videoFromElement(element: Element): Video = throw Exception("Not used")

    override fun videoUrlParse(document: Document): String = throw Exception("Not used")
}

package eu.kanade.tachiyomi.animeextension.fr.waveanime

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WaveAnime : AnimeHttpSource() {

    override val name = "WaveAnime"

    override val baseUrl = "https://waveanime.fr"

    override val lang = "fr"

    override val supportsLatest = true

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/catalog")

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val anime = document.select("div.component.serie-card").map { element ->
            SAnime.create().apply {
                val link = element.selectFirst("a")!!
                setUrlWithoutDomain(link.attr("href"))
                title = element.attr("title")
                thumbnail_url = baseUrl + element.selectFirst("img")!!.attr("src")
            }
        }
        return AnimesPage(anime, false)
    }

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl)

    override fun latestUpdatesParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        // Scrape from "Dernières sorties" on home page
        val anime = document.select("div.component.episode-card").map { element ->
            // These are episodes, we want the series link from context menu or attributes if available
            // In WaveAnime, episode cards often link directly to /watch?v=...
            // But we need the serie ID to get to the serie page.
            // On home page, the title often contains the serie name.
            SAnime.create().apply {
                val onclick = element.attr("oncontextmenu")
                // ComponentUtils['episode-card'].onContextMenu(event, '', false, 'videoId', 'serieId')
                val serieId = onclick.split("'").getOrNull(9)
                if (serieId != null) {
                    setUrlWithoutDomain("/catalog/serie?id=$serieId")
                } else {
                    // Fallback or skip
                    setUrlWithoutDomain("/") 
                }
                title = element.selectFirst("h4")?.text() ?: ""
                thumbnail_url = baseUrl + element.selectFirst("img")!!.attr("src")
            }
        }.distinctBy { it.url }.filter { it.url != "/" }
        
        return AnimesPage(anime, false)
    }

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = "$baseUrl/catalog".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =========================== Anime Details ============================
    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()
        return SAnime.create().apply {
            val info = document.selectFirst("div.serie-info")
            title = info?.selectFirst("h1")?.text() ?: ""
            description = document.selectFirst("div.synopsis p")?.text()
            genre = document.select("div.metadata .item:contains(Genres) .value").text()
            status = parseStatus(document.select("div.metadata .item:contains(Statut) .value").text())
            author = document.select("div.metadata .item:contains(Studio) .value").text()
            thumbnail_url = baseUrl + document.selectFirst("div.poster img")?.attr("src")
        }
    }

    private fun parseStatus(status: String): Int = when {
        status.contains("En cours", true) -> SAnime.ONGOING
        status.contains("Terminé", true) -> SAnime.COMPLETED
        else -> SAnime.UNKNOWN
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()
        
        document.select("div.component.episode-card-grid").forEach { seasonGrid ->
            val seasonNum = seasonGrid.attr("data-season")
            seasonGrid.select("div.component.episode-card").forEach { element ->
                episodes.add(SEpisode.create().apply {
                    val link = element.selectFirst("a")!!.attr("href")
                    setUrlWithoutDomain(link)
                    val epName = element.selectFirst("h4")?.text() ?: ""
                    val epNum = element.selectFirst("h5")?.text() ?: "" // e.g. "S1 E1"
                    name = if (epNum.isNotEmpty()) "$epNum - $epName" else epName
                    
                    // Extract episode number from "S1 E1"
                    episode_number = epNum.substringAfter("E").toFloatOrNull() ?: 0f
                })
            }
        }
        
        return episodes.reversed()
    }

    // ============================ Video Links =============================
    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val html = document.toString()
        
        // Find /playback/ID/master.mpd in script
        // dash.initialize(video, "/playback/7iSBC3SvyqD/master.mpd", true);
        val playbackPath = Regex("""/playback/([^/]+)/master\.mpd""").find(html)?.value
            ?: return emptyList()
            
        val videoUrl = baseUrl + playbackPath
        return listOf(
            Video(videoUrl, "WavePlayer (DASH)", videoUrl)
        )
    }

    override fun List<Video>.sort(): List<Video> {
        return this
    }
}

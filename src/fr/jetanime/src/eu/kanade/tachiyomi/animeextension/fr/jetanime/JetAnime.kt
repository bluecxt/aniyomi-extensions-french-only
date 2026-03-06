package eu.kanade.tachiyomi.animeextension.fr.jetanime

import eu.kanade.tachiyomi.animeextension.fr.jetanime.extractors.HdsplayExtractor
import eu.kanade.tachiyomi.animeextension.fr.jetanime.extractors.SentinelExtractor
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.lib.vidhideextractor.VidHideExtractor
import eu.kanade.tachiyomi.multisrc.dooplay.DooPlay
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class JetAnime :
    DooPlay(
        "fr",
        "JetAnime",
        "https://on.jetanimes.com",
    ) {

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", baseUrl)

    // ============================== Popular ===============================

    override fun popularAnimeRequest(page: Int) = GET(baseUrl, headers)

    override fun popularAnimeSelector(): String = "aside#dtw_content_views-2 div.dtw_content > article"

    override fun popularAnimeNextPageSelector() = null

    // =============================== Latest ===============================

    override fun latestUpdatesFromElement(element: Element): SAnime = SAnime.create().apply {
        val img = element.selectFirst("img")!!
        val url = element.selectFirst("a")?.attr("href") ?: element.attr("href")
        val slug = url.substringAfter("/episodes/")
        setUrlWithoutDomain("/serie/${slug.substringBeforeLast("-saison").substringBeforeLast("-episode")}")
        title = img.attr("alt")
        thumbnail_url = img.getImageUrl()
    }

    override fun latestUpdatesNextPageSelector(): String = "div.pagination > span.current + a"

    // =============================== Search ===============================

    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val url = response.request.url.toString()

        val animeList = when {
            "/?s=" in url -> { // Search by name.
                document.select(searchSelector())
                    .map(::searchAnimeFromElement)
            }

            "/annee/" in url -> { // Search by year
                document.select(searchYearSelector())
                    .map(::popularAnimeFromElement)
            }

            else -> { // Search by some kind of filter, like genres or popularity.
                document.select(searchAnimeSelector())
                    .map(::popularAnimeFromElement)
            }
        }

        val hasNextPage = document.selectFirst(searchAnimeNextPageSelector()) != null
        return AnimesPage(animeList, hasNextPage)
    }

    private fun searchSelector() = "div.search-page > div.result-item div.image a"

    private fun searchYearSelector() = "div.content > div.items > article div.poster"

    override fun searchAnimeSelector() = "div#archive-content > article > div.poster"

    // ============================== Filters ===============================

    override val fetchGenres = false

    override fun getFilterList(): AnimeFilterList = AnimeFilterList(
        AnimeFilter.Header("Text search ignores filters"),
        AnimeFilter.Header("Only one filter at a time works"),
        SubPageFilter(),
        YearFilter(),
    )

    private class SubPageFilter :
        UriPartFilter(
            "Sub-page",
            arrayOf(
                Pair("<select>", ""),
                Pair("FILMS Animes", "/films"),
                Pair("SERIES Animes", "/serie"),

            ),
        )

    private class YearFilter :
        UriPartFilter(
            "Year",
            arrayOf(
                Pair("<select>", ""),
                Pair("2024", "/annee/2024"),
                Pair("2023", "/annee/2023"),
                Pair("2022", "/annee/2022"),
                Pair("2021", "/annee/2021"),
                Pair("2020", "/annee/2020"),
                Pair("2019", "/annee/2019"),
                Pair("2018", "/annee/2018"),
                Pair("2017", "/annee/2017"),
                Pair("2016", "/annee/2016"),
                Pair("2015", "/annee/2015"),
                Pair("2014", "/annee/2014"),
                Pair("2013", "/annee/2013"),
                Pair("2012", "/annee/2012"),
                Pair("2011", "/annee/2011"),
                Pair("2010", "/annee/2010"),
                Pair("2009", "/annee/2009"),
            ),
        )

    // ============================== Episodes ==============================

    override fun episodeListSelector() = "ul.episodios > li"

    override fun episodeFromElement(element: Element, seasonName: String): SEpisode = SEpisode.create().apply {
        val a = element.selectFirst("a")!!
        setUrlWithoutDomain(a.attr("abs:href"))
        val epText = element.selectFirst("div.numerando")?.text() ?: ""
        name = if (seasonName.isNotEmpty()) "$seasonName - ${a.text()}" else a.text()
        episode_number = epText.substringAfter("-").trim().toFloatOrNull() ?: 0f
        date_upload = element.selectFirst("span.date")?.text()?.let { parseDate(it) } ?: 0L
    }

    private fun parseDate(dateStr: String): Long = try {
        SimpleDateFormat("MMM. dd, yyyy", Locale.ENGLISH).parse(dateStr)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    // ============================ Video Links =============================

    private val noRedirects = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val players = document.select("ul#playeroptionsul li")
        val nonce = document.selectFirst("script:containsData(nonce)")?.data()
            ?.substringAfter("\"nonce\":\"")?.substringBefore("\"") ?: ""

        return players.mapNotNull { player ->
            runCatching {
                val url = getPlayerUrl(player, nonce).ifEmpty { return@mapNotNull null }
                val redirected = noRedirects.newCall(
                    GET(url),
                ).execute().headers["location"] ?: url

                val name = player.text().trim()
                getPlayerVideos(redirected, name)
            }.getOrNull()
        }.flatten()
    }

    private fun getPlayerUrl(player: Element, nonce: String): String {
        val type = player.attr("data-type")
        val id = player.attr("data-post")
        val num = player.attr("data-nume")
        if (num == "trailer") return ""

        val url = "$baseUrl/wp-admin/admin-ajax.php"
        val body = okhttp3.FormBody.Builder()
            .add("action", "doo_player_ajax")
            .add("post", id)
            .add("nume", num)
            .add("type", type)
            .build()

        // DooPlay often uses admin-ajax.php with action=doo_player_ajax
        // Let's try this first as the wp-json endpoint returned empty.

        val response = client.newCall(okhttp3.Request.Builder().url(url).post(body).build()).execute().body.string()
        return response.substringAfter("\"embed_url\":\"").substringBefore("\"").replace("\\", "")
    }

    private fun getPlayerVideos(url: String, name: String): List<Video> = when {
        url.contains("https://sentinel") -> SentinelExtractor(client).videoFromUrl(url, name)
        url.contains("https://hdsplay") -> HdsplayExtractor(client).videoFromUrl(url, name)
        url.contains("secured.lol") || url.contains("vidhide") -> VidHideExtractor(client, headers).videosFromUrl(url)
        else -> emptyList()
    }

    // ============================== Settings ==============================

    override val prefQualityValues = arrayOf("1080p", "720p", "480p", "360p", "240p")
    override val prefQualityEntries = prefQualityValues
}

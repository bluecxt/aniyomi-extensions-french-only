package eu.kanade.tachiyomi.animeextension.fr.southtv

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

class SouthTV : AnimeHttpSource() {

    override val name = "SouthTV"
    override val baseUrl = "https://southtv.fr"
    private val videoUrlHost = "https://southtv.info"
    override val lang = "fr"
    override val supportsLatest = false

    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

    private val episodesPerSeason = listOf(13, 18, 17, 17, 14, 17, 15, 14, 14, 14, 14, 14, 14, 14, 14, 14, 10, 10, 10, 10, 10, 10, 10, 4, 6, 6, 5, 5)
    private val movies = listOf(
        Triple("Creed", "creed.mp4", "https://tse4.mm.bing.net/th/id/OIP.cUq62US5QYmJx7ahQUT6vQHaEK?rs=1&pid=ImgDetMain&o=7&rm=3"),
        Triple("Panda Verse", "pandav.mp4", "https://tse3.mm.bing.net/th/id/OIP.6KFYYuFK79wOcDl71177owHaD4?rs=1&pid=ImgDetMain&o=7&rm=3"),
        Triple("The end of Obesity", "obes.mp4", "https://thumbnails.cbsig.net/CBS_Production_Entertainment_VMS/2024/05/01/2333651011699/SPTEO_US_2024_SA_16x9_1920x1080_NB_2695584_1920x1080.jpg"),
        Triple("Streaming War P1", "streamwar1.mp4", "https://m.media-amazon.com/images/S/pv-target-images/b5a1bd01e3984eec29a70506b160d7060895b7845f40c0e13b44f5a903815496._UR1920,1080_.jpg"),
        Triple("Streaming War P2", "streamwar2.mp4", "https://thumbnails.cbsig.net/CBS_Production_Entertainment_VMS/2022/06/22/2045697603902/SPTSW2_SAlone_16_9_1920x1080_NB_1476556_1920x1080.jpg"),
        Triple("South Park le Film", "Filmsouthpark.mp4", "https://thumbnails.cbsig.net/CBS_Production_Entertainment_VMS/2021/11/10/1972863555958/SPBLU_SAlone_16_9_1920x1080_1040472_1920x1080.jpg"),
    )

    private fun getAnimeList(): List<SAnime> {
        val animeList = mutableListOf<SAnime>()
        // South Park
        animeList.add(
            SAnime.create().apply {
                title = "South Park (VF)"
                url = "south_park_vf"
                thumbnail_url = "https://image.tmdb.org/t/p/original/2PzoiWFm3WymmOPOkWe5DKv7xD8.jpg"
                description = "South Park est une série d\'animation américaine pour adultes créée par Trey Parker et Matt Stone. La série est connue pour son humour noir, sa satire et son langage grossier."
                artist = "Trey Parker, Matt Stone"
                status = SAnime.ONGOING
            },
        )
        animeList.add(
            SAnime.create().apply {
                title = "South Park (VO)"
                url = "south_park_vo"
                thumbnail_url = "https://image.tmdb.org/t/p/original/2PzoiWFm3WymmOPOkWe5DKv7xD8.jpg"
                description = "South Park is an American adult animated sitcom created by Trey Parker and Matt Stone. The series is known for its black humor, satire, and crude language."
                artist = "Trey Parker, Matt Stone"
                status = SAnime.ONGOING
            },
        )
        // Movies
        movies.forEach { (title, file, thumb) ->
            animeList.add(
                SAnime.create().apply {
                    this.title = title
                    this.url = "movie_$file"
                    this.thumbnail_url = thumb
                    this.status = SAnime.COMPLETED
                },
            )
        }
        return animeList
    }

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        return AnimesPage(getAnimeList(), false)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        val filtered = getAnimeList().filter { it.title.contains(query, ignoreCase = true) }
        return AnimesPage(filtered, false)
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return anime
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        val episodes = mutableListOf<SEpisode>()
        if (anime.url.startsWith("southpark")) {
            val lang = anime.url.split('_')[1]
            var episodeCountSoFar = 0
            episodesPerSeason.forEachIndexed { seasonIndex, count ->
                for (i in 1..count) {
                    episodes.add(
                        SEpisode.create().apply {
                            name = "Saison ${seasonIndex + 1} Episode $i"
                            url = "southpark#lang=$lang&s=${seasonIndex + 1}&e=$i"
                            episode_number = (episodeCountSoFar + i).toFloat()
                            scanlator = "S${seasonIndex + 1}"
                        },
                    )
                }
                episodeCountSoFar += count
            }
        } else { // movie
            episodes.add(
                SEpisode.create().apply {
                    name = anime.title
                    url = anime.url
                    episode_number = 1f
                },
            )
        }
        return episodes.reversed()
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val videoList = mutableListOf<Video>()
        if (episode.url.startsWith("southpark")) {
            val parts = episode.url.split("#")[1].split("&")
            val lang = parts.find { it.startsWith("lang=") }!!.substring(5)
            val season = parts.find { it.startsWith("s=") }!!.substring(2)
            val episodeNum = parts.find { it.startsWith("e=") }!!.substring(2)
            val videoUrlBase = if (lang == "vf") "$videoUrlHost/southpark/" else "$videoUrlHost/southparkvo/"
            val videoUrl = "${videoUrlBase}s${season}e$episodeNum.mp4"

            val videoHeaders = Headers.Builder().apply {
                add("User-Agent", USER_AGENT)
                if (lang == "vf") {
                    add("Referer", "$baseUrl/")
                }
            }.build()
            videoList.add(Video(videoUrl, "default", videoUrl, headers = videoHeaders))
        } else { // movie
            val movieName = episode.url.substringAfter("movie_")
            val videoUrl = "$videoUrlHost/films/$movieName"
            val videoHeaders = Headers.Builder().add("User-Agent", USER_AGENT).build()
            videoList.add(Video(videoUrl, "default", videoUrl, headers = videoHeaders))
        }
        return videoList
    }

    override fun popularAnimeRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun searchAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun animeDetailsParse(response: Response): SAnime = throw UnsupportedOperationException()
    override fun episodeListParse(response: Response): List<SEpisode> = throw UnsupportedOperationException()
    override fun videoListParse(response: Response): List<Video> = throw UnsupportedOperationException()
}

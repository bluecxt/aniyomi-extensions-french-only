package eu.kanade.tachiyomi.animeextension.fr.vostfree

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class VostfreeUrlActivity : Activity() {

    private val tag = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            val path = data.path
            if (path != null && path.contains("-") && path.endsWith(".html")) {
                // Extract ID from path like /864-my-hero-academia.html
                val id = path.substringAfter("/").substringBefore("-")
                val mainIntent = Intent().apply {
                    action = "eu.kanade.tachiyomi.ANIMESEARCH"
                    putExtra("query", "${Vostfree.PREFIX_SEARCH}$id")
                    putExtra("filter", packageName)
                }

                try {
                    startActivity(mainIntent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(tag, e.toString())
                }
            }
        }

        finish()
        exitProcess(0)
    }
}

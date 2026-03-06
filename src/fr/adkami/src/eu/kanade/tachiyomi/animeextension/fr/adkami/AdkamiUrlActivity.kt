package eu.kanade.tachiyomi.animeextension.fr.adkami

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class AdkamiUrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val intent = Intent().apply {
                action = "eu.kanade.tachiyomi.ANIMESEARCH"
                putExtra("query", pathSegments[1])
                putExtra("filter", packageName)
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AdkamiUrlActivity", e.toString())
            }
        } else {
            Log.e("AdkamiUrlActivity", "could not parse global search intent")
        }

        finish()
        exitProcess(0)
    }
}

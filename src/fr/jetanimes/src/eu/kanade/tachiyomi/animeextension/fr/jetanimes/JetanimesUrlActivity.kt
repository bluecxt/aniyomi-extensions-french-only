package eu.kanade.tachiyomi.animeextension.fr.jetanimes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class JetanimesUrlActivity : Activity() {
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
                Log.e("JetanimesUrlActivity", e.toString())
            }
        } else {
            Log.e("JetanimesUrlActivity", "could not parse global search intent")
        }

        finish()
        exitProcess(0)
    }
}

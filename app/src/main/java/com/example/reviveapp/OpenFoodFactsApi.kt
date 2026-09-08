package com.example.reviveapp

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * Talks to the Open Food Facts search endpoint over HTTP and hands back
 * ready-to-use FoodItems. Callers don't need to know about OkHttp, JSON,
 * or threading — just "give me a query, get foods back."
 */
class OpenFoodFactsApi {

    // One client per instance of this class, reused for every search.
    // OkHttpClient owns a connection pool and a thread pool internally —
    // expensive to create, cheap to reuse — so we build it exactly once
    // here rather than inside search().
    private val client = OkHttpClient()

    // OkHttp's callbacks fire on a background thread it manages itself,
    // never on the main/UI thread. Anything that touches views has to
    // happen back on the main thread, so we keep a Handler tied to the
    // main Looper and post results through it. (Kotlin coroutines would
    // let you avoid this manual hop with suspend functions — a natural
    // refactor later, but your codebase is callback-based throughout,
    // so we're matching that here rather than mixing two styles.)
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    companion object {
        // Open Food Facts asks every app to identify itself with a
        // descriptive User-Agent so they can contact you if your traffic
        // looks like abuse. Put a real contact here before you ship this
        // anywhere — "example.com" isn't a real point of contact.
        private const val USER_AGENT = "ReviveApp/1.0 (youremail@example.com)"
        private const val RESULTS_PER_SEARCH = "20"
    }

    /**
     * Searches Open Food Facts for [query] and calls exactly one of
     * [onResult] or [onError], on the main thread, when it's done.
     */
    fun search(query: String, onResult: (List<FoodItem>) -> Unit, onError: (Exception) -> Unit) {
        // Building the URL piece by piece, rather than concatenating a
        // string, has a real advantage: addQueryParameter percent-encodes
        // the value for you. Search "greek yogurt" and the space becomes
        // %20 automatically — get that wrong by hand and the request
        // either fails outright or silently searches for the wrong thing.
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("world.openfoodfacts.org")
            .addPathSegment("cgi")
            .addPathSegment("search.pl")
            .addQueryParameter("search_terms", query)
            .addQueryParameter("json", "1")
            .addQueryParameter("page_size", RESULTS_PER_SEARCH)
            .addQueryParameter("fields", "code,product_name,nutriments")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).enqueue(object : Callback {
            // Called for network-level failures: no connection, timeout,
            // DNS failure, etc. Note this is NOT called for HTTP error
            // codes like 404 or 503 — those come through onResponse below,
            // because as far as OkHttp's concerned, "the server answered
            // with a 503" is a successful round trip, just an unhelpful one.
            override fun onFailure(call: Call, e: IOException) {
                mainThreadHandler.post { onError(e) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { res ->
                        if (!res.isSuccessful) {
                            val retryAfter = res.header("Retry-After")
                            val errorBody = res.body?.string()?.take(300) // cap it — don't log a huge HTML error page
                            val details = buildString {
                                append("Open Food Facts returned HTTP ${res.code}")
                                if (retryAfter != null) append(" (retry after ${retryAfter}s)")
                                if (!errorBody.isNullOrBlank()) append(": $errorBody")
                            }
                            mainThreadHandler.post { onError(IOException(details)) }
                            return
                        }

                        val bodyText = res.body?.string()
                        if (bodyText.isNullOrBlank()) {
                            mainThreadHandler.post { onError(IOException("Empty response body")) }
                            return
                        }

                        val json = JSONObject(bodyText)
                        val foodItems = parseOffSearchResponse(json).mapNotNull { it.toFoodItemOrNull() }
                        mainThreadHandler.post { onResult(foodItems) }
                    }
                } catch (e: Exception) {
                    // Now catches everything that can go wrong while reading or
                    // parsing the response, not just the JSON step — a bad read,
                    // a truncated body, whatever. One net instead of two.
                    mainThreadHandler.post { onError(e) }
                }
            }
        })
    }
}
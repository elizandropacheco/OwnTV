package com.conreo.couchytv.ui

import android.content.Context
import android.content.Intent
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

/** Public video author lookup only; no Google account, cookies, history or recommendation API. */
internal object OwnYouTubeMetadata {
    private val slots = Semaphore(3)

    private fun videoId(item: TvSuggestion): String? = runCatching {
        val uri =
            Intent.parseUri(item.intent, Intent.URI_INTENT_SCHEME).data ?: return@runCatching null
        val host = uri.host.orEmpty().lowercase()
        val id =
            when {
                host == "youtu.be" -> uri.pathSegments.firstOrNull()
                host == "youtube.com" || host.endsWith(".youtube.com") ->
                    uri.getQueryParameter("v")
                        ?: uri.pathSegments
                            .takeIf { it.firstOrNull() in listOf("shorts", "embed") }
                            ?.getOrNull(1)
                uri.scheme == "vnd.youtube" ->
                    runCatching { uri.getQueryParameter("v") }.getOrNull()
                        ?: uri.schemeSpecificPart
                            .removePrefix("//")
                            .substringBefore('?')
                            .substringBefore('/')
                else -> null
            }
        id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }
        .getOrNull()

    suspend fun enrich(context: Context, items: List<TvSuggestion>): List<TvSuggestion> =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "youtube-authors.json")
            val cache = runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
            val now = System.currentTimeMillis()
            val candidates =
                items
                    .filter {
                        it.publisher == "com.google.android.youtube.tv" && it.author.isBlank()
                    }
                    .mapNotNull { item -> videoId(item)?.let { it to item } }
                    .distinctBy { it.first }
                    .take(24)
            val authors = coroutineScope {
                candidates
                    .map { (id, _) ->
                        async {
                            val saved = cache.optJSONObject(id)
                            if (
                                saved != null &&
                                    now - saved.optLong("at") in
                                        0 until
                                            (if (saved.optString("author").isBlank())
                                                60 * 60 * 1000L
                                            else 7 * 24 * 60 * 60 * 1000L)
                            )
                                return@async id to saved.optString("author")
                            val author = slots.withPermit {
                                ensureActive()
                                runCatching {
                                        val video = "https://www.youtube.com/watch?v=$id"
                                        val connection =
                                            URL(
                                                    "https://www.youtube.com/oembed?format=json&url=" +
                                                        URLEncoder.encode(video, "UTF-8"),
                                                )
                                                .openConnection() as HttpURLConnection
                                        connection.connectTimeout = 3000
                                        connection.readTimeout = 3000
                                        connection.setRequestProperty(
                                            "User-Agent",
                                            "OwnTV/0.2.2 public video metadata",
                                        )
                                        try {
                                            if (connection.responseCode != 200)
                                                return@runCatching ""
                                            val bytes =
                                                connection.inputStream.use { input ->
                                                    val output = java.io.ByteArrayOutputStream()
                                                    val buffer = ByteArray(4096)
                                                    while (true) {
                                                        val count = input.read(buffer)
                                                        if (count < 0) break
                                                        output.write(buffer, 0, count)
                                                        if (output.size() > 131072)
                                                            throw java.io.IOException(
                                                                "metadata size",
                                                            )
                                                    }
                                                    output.toByteArray()
                                                }
                                            JSONObject(String(bytes, Charsets.UTF_8))
                                                .optString("author_name")
                                                .replace('\n', ' ')
                                                .trim()
                                                .take(120)
                                        } finally {
                                            connection.disconnect()
                                        }
                                    }
                                    .getOrDefault("")
                            }
                            id to author
                        }
                    }
                    .awaitAll()
                    .toMap()
            }
            ensureActive()
            authors.forEach { (id, author) ->
                cache.put(id, JSONObject().put("author", author).put("at", now))
            }
            val keys = cache.keys().asSequence().toList()
            keys
                .filter {
                    now - (cache.optJSONObject(it)?.optLong("at") ?: 0) > 7 * 24 * 60 * 60 * 1000L
                }
                .forEach { cache.remove(it) }
            cache.keys().asSequence().toList().drop(256).forEach { cache.remove(it) }
            runCatching {
                val temp = File(file.parentFile, file.name + ".part")
                temp.writeText(cache.toString())
                temp.renameTo(file)
            }
            items.map { item ->
                val author = videoId(item)?.let { authors[it] }
                if (item.author.isBlank() && !author.isNullOrBlank()) item.copy(author = author)
                else item
            }
        }
}

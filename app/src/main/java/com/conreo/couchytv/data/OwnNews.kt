package com.conreo.couchytv.data

import android.content.Context
import android.util.Xml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

/** RSS headlines only: no account data, article scraping or background UI automation. */
data class OwnHeadline(
    val title: String,
    val topic: String,
    val source: String,
    val url: String,
    val publishedAt: Long,
)

object OwnNews {
    @JvmStatic
    fun topics(context: Context): List<String> =
        context
            .getSharedPreferences("own_news", 0)
            .getString("topics", "telecom, tecnologia, inteligência artificial")
            .orEmpty()
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(8)

    suspend fun load(context: Context): List<OwnHeadline> =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("own_news", 0)
            if (!prefs.getBoolean("enabled", true)) return@withContext emptyList()
            val selected = topics(context)
            if (selected.isEmpty()) return@withContext emptyList()
            val signature =
                MessageDigest.getInstance("SHA-256")
                    .digest(selected.joinToString("|").toByteArray())
                    .joinToString("") { "%02x".format(it) }
            val file = File(context.filesDir, "news-$signature.json")
            val cached = runCatching { JSONObject(file.readText()) }.getOrNull()
            val age = System.currentTimeMillis() - (cached?.optLong("fetchedAt", 0) ?: 0)
            if (cached != null && age in 0 until 30 * 60 * 1000L)
                return@withContext decode(cached.optJSONArray("items"))
            val groups = coroutineScope {
                selected
                    .map { topic ->
                        async { runCatching { fetch(topic) }.getOrDefault(emptyList()) }
                    }
                    .awaitAll()
            }
            val items = ArrayList<OwnHeadline>()
            for (index in 0 until 12) for (group in groups) group.getOrNull(index)?.let {
                items.add(it)
            }
            val fresh = items.distinctBy { it.title }.take(96)
            if (fresh.isEmpty()) return@withContext decode(cached?.optJSONArray("items"))
            val array = JSONArray()
            fresh.forEach {
                array.put(
                    JSONObject()
                        .put("title", it.title)
                        .put("topic", it.topic)
                        .put("source", it.source)
                        .put("url", it.url)
                        .put("publishedAt", it.publishedAt),
                )
            }
            runCatching {
                val temp = File(file.path + ".new")
                temp.writeText(
                    JSONObject()
                        .put("fetchedAt", System.currentTimeMillis())
                        .put("items", array)
                        .toString(),
                )
                temp.renameTo(file)
            }
            fresh
        }

    private fun decode(array: JSONArray?): List<OwnHeadline> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { n ->
            array.optJSONObject(n)?.let { p ->
                OwnHeadline(
                    p.optString("title"),
                    p.optString("topic"),
                    p.optString("source"),
                    p.optString("url"),
                    p.optLong("publishedAt"),
                )
            }
        }
    }

    private fun fetch(topic: String): List<OwnHeadline> {
        val query = URLEncoder.encode("$topic when:7d", "UTF-8")
        val connection =
            URL("https://news.google.com/rss/search?q=$query&hl=pt-BR&gl=BR&ceid=BR:pt-419")
                .openConnection() as HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.setRequestProperty("User-Agent", "OwnTV/0.2 (+https://elizandropacheco.io)")
        try {
            if (connection.responseCode != 200) return emptyList()
            val data =
                connection.inputStream.use { input ->
                    val sink = ByteArrayOutputStream()
                    val buffer = ByteArray(32768)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sink.write(buffer, 0, count)
                        if (sink.size() > 1024 * 1024) return emptyList()
                    }
                    sink.toByteArray()
                }
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(data), "UTF-8")
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            val result = ArrayList<OwnHeadline>()
            var inItem = false
            var title = ""
            var link = ""
            var source = ""
            var published = 0L
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            inItem = true
                            title = ""
                            link = ""
                            source = ""
                            published = 0
                        }
                        "title" -> if (inItem) title = parser.nextText().trim()
                        "link" -> if (inItem) link = parser.nextText().trim()
                        "source" -> if (inItem) source = parser.nextText().trim()
                        "pubDate" ->
                            if (inItem)
                                published =
                                    runCatching { dateFormat.parse(parser.nextText())?.time ?: 0L }
                                        .getOrDefault(0)
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "item") {
                    if (title.isNotBlank() && link.startsWith("https://")) {
                        val suffix = if (source.isBlank()) "" else " - $source"
                        result.add(
                            OwnHeadline(
                                if (suffix.isNotEmpty()) title.removeSuffix(suffix) else title,
                                topic,
                                source.ifBlank { "Google Notícias" },
                                link,
                                published,
                            ),
                        )
                    }
                    inItem = false
                }
                parser.next()
            }
            return result.sortedByDescending { it.publishedAt }.take(12)
        } finally {
            connection.disconnect()
        }
    }
}

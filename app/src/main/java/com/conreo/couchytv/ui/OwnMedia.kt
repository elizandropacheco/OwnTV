package com.conreo.couchytv.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.conreo.couchytv.OwnIcons
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal fun artwork(
    context: Context,
    source: String,
    maxWidth: Int = 960,
    coverHeight: Int = 0,
): ImageBitmap? = runCatching {
    if (source.startsWith("logo:")) {
        val brand = source.removePrefix("logo:")
        val bitmap =
            android.graphics.Bitmap.createBitmap(
                if (brand == "globoplay") 512 else 256,
                if (brand == "globoplay") 192 else 256,
                android.graphics.Bitmap.Config.ARGB_8888,
            )
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color =
            when (brand) {
                "youtube" -> 0xFFFF3048.toInt()
                "spotify" -> 0xFF1ED760.toInt()
                else -> android.graphics.Color.WHITE
            }
        val svg = runCatching {
            context.assets.open("ep/logos/$brand.svg").bufferedReader().use { it.readText() }
        }
            .getOrNull()
        if (brand == "camera") {
            canvas.save()
            canvas.translate(13f, 13f)
            OwnIcons.draw(canvas, "camera", 230, 230)
            canvas.restore()
        } else if (svg != null) {
            val box =
                Regex("viewBox=\"([^\"]+)\"")
                    .find(svg)
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?.split(Regex("[ ,]+"))
                    ?.mapNotNull { it.toFloatOrNull() } ?: listOf(0f, 0f, 24f, 24f)
            val sw = box.getOrElse(2) { 24f }
            val sh = box.getOrElse(3) { 24f }
            val ratio = minOf((bitmap.width - 28f) / sw, (bitmap.height - 28f) / sh)
            canvas.save()
            canvas.translate(
                (bitmap.width - sw * ratio) / 2 - box[0] * ratio,
                (bitmap.height - sh * ratio) / 2 - box[1] * ratio,
            )
            canvas.scale(ratio, ratio)
            Regex("<path[^>]*d=\"([^\"]+)\"").findAll(svg).forEach { m ->
                androidx.core.graphics.PathParser.createPathFromPathData(m.groupValues[1])?.let {
                    canvas.drawPath(it, paint)
                }
            }
            canvas.restore()
        } else {
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.textSize = if (brand == "globoplay") 46f else 58f
            paint.typeface =
                android.graphics.Typeface.create(
                    "sans-serif-medium",
                    android.graphics.Typeface.NORMAL,
                )
            canvas.drawText(if (brand == "globoplay") "globoplay" else brand, 128f, 145f, paint)
        }
        return@runCatching bitmap.asImageBitmap()
    }
    if (
        source == "camera" &&
            runCatching { context.assets.open("ep/cards/cameras.png").close() }.isSuccess
    )
        return@runCatching artwork(context, "camera_photo", maxWidth)
    if (source in listOf("camera", "youtube", "game")) {
        val bitmap =
            android.graphics.Bitmap.createBitmap(640, 360, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.shader =
            android.graphics.LinearGradient(
                0f,
                0f,
                640f,
                360f,
                intArrayOf(0xFF123F56.toInt(), 0xFF06151E.toInt()),
                null,
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, 640f, 360f, paint)
        canvas.save()
        canvas.translate(360f, 70f)
        OwnIcons.draw(canvas, source, 190, 190)
        canvas.restore()
        return@runCatching bitmap.asImageBitmap()
    }
    if (
        source == "fortnite" &&
            runCatching { context.assets.open("ep/cards/fortnite-landscape.jpg").close() }.isFailure
    )
        return@runCatching artwork(context, "game", maxWidth)
    var resolved = source
    if (source.startsWith("https://")) {
        val hash =
            MessageDigest.getInstance("SHA-256").digest(source.toByteArray()).joinToString("") {
                "%02x".format(it)
            }
        val file = File(context.cacheDir, "media-$hash")
        if (!file.exists()) {
            val connection = URL(source).openConnection() as HttpURLConnection
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            try {
                connection.inputStream.use { input ->
                    val sink = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(32768)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sink.write(buffer, 0, count)
                        if (sink.size() > 4 * 1024 * 1024) return@runCatching null
                    }
                    val data = sink.toByteArray()
                    if (data.size > 4 * 1024 * 1024) return@runCatching null
                    file.writeBytes(data)
                }
            } finally {
                connection.disconnect()
            }
        }
        resolved = file.path
    }
    if (android.os.Build.VERSION.SDK_INT < 28) {
        fun open(): java.io.InputStream =
            when {
                resolved == "fortnite" -> context.assets.open("ep/cards/fortnite-landscape.jpg")
                resolved == "camera_photo" -> context.assets.open("ep/cards/cameras.png")
                resolved.startsWith("asset:") ->
                    context.assets.open("ep/wallpapers/" + resolved.removePrefix("asset:"))
                resolved.startsWith("content:") ->
                    context.contentResolver.openInputStream(Uri.parse(resolved))
                        ?: throw java.io.IOException()
                else -> java.io.FileInputStream(resolved)
            }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (
            bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxWidth * 2
        ) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return@runCatching open().use {
            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
        }
    }
    val imageSource =
        when {
            resolved == "fortnite" ->
                ImageDecoder.createSource(context.assets, "ep/cards/fortnite-landscape.jpg")
            resolved == "camera_photo" ->
                ImageDecoder.createSource(context.assets, "ep/cards/cameras.png")
            resolved.startsWith("asset:") ->
                ImageDecoder.createSource(
                    context.assets,
                    "ep/wallpapers/" + resolved.removePrefix("asset:"),
                )
            resolved.startsWith("content:") ->
                ImageDecoder.createSource(context.contentResolver, Uri.parse(resolved))
            else -> ImageDecoder.createSource(File(resolved))
        }
    ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            val scale =
                if (coverHeight > 0)
                    minOf(
                        1.0,
                        maxOf(
                            maxWidth.toDouble() / info.size.width,
                            coverHeight.toDouble() / info.size.height,
                        ),
                    )
                else
                    minOf(
                        1.0,
                        maxWidth.toDouble() / info.size.width,
                        maxWidth.toDouble() / info.size.height,
                    )
            decoder.setTargetSize(
                maxOf(1, (info.size.width * scale).toInt()),
                maxOf(1, (info.size.height * scale).toInt()),
            )
        }
        .asImageBitmap()
}
    .getOrNull()

internal data class TvSuggestion(
    val id: String,
    val title: String,
    val image: String,
    val intent: String,
    val publisher: String,
    val group: String,
    val source: String,
    val author: String = "",
    val durationMs: Long = 0,
    val releaseDate: String = "",
) {
    val duration: String
        get() {
            val seconds = durationMs / 1000
            return if (seconds <= 0) ""
            else if (seconds >= 3600)
                String.format(
                    java.util.Locale.ROOT,
                    "%d:%02d:%02d",
                    seconds / 3600,
                    seconds / 60 % 60,
                    seconds % 60,
                )
            else String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
        }

    val metadata: String
        get() {
            val date = runCatching {
                java.time.LocalDate.parse(releaseDate.take(10))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }
                .getOrElse { releaseDate.takeIf { it.matches(Regex("[12][0-9]{3}")) }.orEmpty() }
            return listOf(source, author.takeIf { !it.equals(source, true) }.orEmpty(), date)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
        }
}

internal fun suggestions(context: Context): List<TvSuggestion> {
    val result = mutableListOf<TvSuggestion>()
    val labels = mutableMapOf<String, String?>()
    for (table in listOf("watch_next_program", "preview_program")) {
        val publisherCounts = mutableMapOf<String, Int>()
        runCatching {
            val uri = Uri.parse("content://android.media.tv/$table")
            val basic = arrayOf("_id", "title", "poster_art_uri", "intent_uri", "package_name")
            val projection = basic + arrayOf("author", "duration_millis", "release_date")
            val query = runCatching {
                context.contentResolver.query(uri, projection, null, null, null)
            }
                .getOrElse { context.contentResolver.query(uri, basic, null, null, null) }
            query?.use { cursor ->
                fun text(column: String): String {
                    val index = cursor.getColumnIndex(column)
                    return if (index >= 0) cursor.getString(index).orEmpty() else ""
                }
                while (cursor.moveToNext()) {
                    val title = cursor.getString(1).orEmpty()
                    val intent = cursor.getString(3).orEmpty()
                    val pkg = cursor.getString(4).orEmpty()
                    if (
                        title.isBlank() ||
                            intent.isBlank() ||
                            pkg == context.packageName ||
                            pkg.contains("launcher") ||
                            pkg.contains("tcl")
                    )
                        continue
                    // A prolific publisher must not exhaust the budget before other apps are read.
                    if (publisherCounts.getOrDefault(pkg, 0) >= 24) continue
                    val parsed =
                        runCatching { Intent.parseUri(intent, Intent.URI_INTENT_SCHEME) }
                            .getOrNull() ?: continue
                    if (parsed.component != null && parsed.component!!.packageName != pkg) continue
                    val label =
                        labels.getOrPut(pkg) {
                            if (
                                context.packageManager.getLeanbackLaunchIntentForPackage(pkg) ==
                                    null
                            )
                                null
                            else
                                runCatching {
                                        context.packageManager
                                            .getApplicationLabel(
                                                context.packageManager.getApplicationInfo(pkg, 0),
                                            )
                                            .toString()
                                    }
                                    .getOrDefault(pkg)
                        } ?: continue
                    val author =
                        text("author").ifBlank {
                            if (pkg != "tv.twitch.android.app") ""
                            else
                                runCatching {
                                        val data = parsed.data
                                        val path = data?.pathSegments.orEmpty()
                                        val name =
                                            if (data?.scheme == "twitch" && data.host == "stream")
                                                path.firstOrNull()
                                            else if (
                                                data?.host in
                                                    listOf(
                                                        "twitch.tv",
                                                        "www.twitch.tv",
                                                        "m.twitch.tv",
                                                    )
                                            )
                                                path.firstOrNull()
                                            else null
                                        name
                                            ?.takeIf {
                                                it.matches(Regex("[A-Za-z0-9_]{1,25}")) &&
                                                    it !in
                                                        listOf(
                                                            "directory",
                                                            "videos",
                                                            "downloads",
                                                            "search",
                                                            "settings",
                                                        )
                                            }
                                            .orEmpty()
                                    }
                                    .getOrDefault("")
                        }
                    result.add(
                        TvSuggestion(
                            "$table:${cursor.getLong(0)}",
                            title,
                            cursor.getString(2).orEmpty(),
                            intent,
                            pkg,
                            if (table == "watch_next_program") "Continuar assistindo" else label,
                            label,
                            author,
                            text("duration_millis").toLongOrNull()?.coerceAtLeast(0) ?: 0,
                            text("release_date"),
                        ),
                    )
                    publisherCounts[pkg] = publisherCounts.getOrDefault(pkg, 0) + 1
                }
            }
        }
    }
    return result
        .distinctBy { it.publisher + it.title }
        .sortedBy { if (it.publisher == "com.google.android.youtube.tv") 0 else 1 }
        .take(240)
}

internal fun openSuggestion(context: Context, item: TvSuggestion) {
    runCatching {
        val intent = Intent.parseUri(item.intent, Intent.URI_INTENT_SCHEME)
        intent.selector = null
        intent.clipData = null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setPackage(item.publisher)
        context.startActivity(intent)
    }
        .onFailure { com.conreo.couchytv.Actions.launchApp(context, item.publisher) }
}

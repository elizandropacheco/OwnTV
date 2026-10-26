package com.conreo.couchytv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.LauncherConfig
import java.io.File

/** Public defaults contain no device addresses or credentials. */
object EpDefaults {
    val favorites =
        listOf(
            "com.google.android.youtube.tv",
            "com.spotify.tv.android",
            "br.com.skymais",
            "tv.twitch.android.app",
            "com.globo.globotv",
            "ep:ps5",
            "ep:cameras",
        )

    fun config() =
        LauncherConfig(
            categories =
                listOf(
                    CategoryCfg("favorites", "Favoritos"),
                    CategoryCfg("streaming", "Filmes"),
                    CategoryCfg("music", "Música"),
                    CategoryCfg("games", "Jogos"),
                    CategoryCfg("home", "Casa"),
                    CategoryCfg("__all__", "Todos os apps"),
                ),
            sections =
                favorites.associateWith { pkg ->
                    setOf("favorites") +
                        when (pkg) {
                            "com.spotify.tv.android" -> setOf("music")
                            "ep:ps5" -> setOf("games")
                            "ep:cameras" -> setOf("home")
                            else -> setOf("streaming")
                        }
                } +
                    mapOf(
                        "ep:ps5" to setOf("favorites", "games"),
                        "ep:cameras" to setOf("favorites", "home"),
                        "ep:youtube" to setOf("home"),
                        "ep:settings" to setOf("home"),
                        "ep:wallpapers" to setOf("home"),
                        "ep:original" to setOf("home"),
                    ),
            order =
                mapOf(
                    "favorites" to favorites,
                    "home" to
                        listOf(
                            "ep:ps5",
                            "ep:cameras",
                            "ep:youtube",
                            "ep:settings",
                            "ep:wallpapers",
                            "ep:original",
                        ),
                ),
            cornerRadius = 2,
            layout = 2,
            accent = 0,
            wallpaper = 0,
            useCustomWallpaper = true,
            setupDone = true,
            showVpnButton = false,
            dateFormat = 2,
            scrimMode = 2,
            iconScale = 2,
            spacing = 1,
            uiScale = 3,
            showAppLabels = true,
            statusBarGlass = true,
        )

    fun prepare(ctx: Context) {
        val wallpaper = File(ctx.filesDir, "wallpaper.jpg")
        if (!wallpaper.exists())
            ctx.assets.open("ep/linhas.jpg").use { i ->
                wallpaper.outputStream().use { i.copyTo(it) }
            }
    }

    private var cached: List<AppEntry>? = null

    fun entries(ctx: Context): List<AppEntry> =
        cached
            ?: listOf(
                    Triple("ps5", "PS5 · Fortnite", "PS5"),
                    Triple("cameras", "Câmeras", "CAM"),
                    Triple("youtube", "YouTube · Inscrições", "PLAY"),
                    Triple("settings", "Configurações TCL", "TV"),
                    Triple("wallpapers", "Fundos EP", "EP"),
                    Triple("original", "Google TV original", "HOME"),
                )
                .map { (id, label, symbol) ->
                    val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(AndroidColor.rgb(3, 11, 18))
                    val paint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.rgb(0, 132, 254)
                            strokeWidth = 4f
                        }
                    canvas.drawLine(24f, 24f, 90f, 24f, paint)
                    canvas.drawLine(90f, 24f, 104f, 38f, paint)
                    paint.color = AndroidColor.rgb(44, 235, 67)
                    canvas.drawLine(220f, 156f, 296f, 156f, paint)
                    paint.color = AndroidColor.rgb(244, 246, 248)
                    paint.textSize = 39f
                    paint.textAlign = Paint.Align.CENTER
                    paint.typeface =
                        android.graphics.Typeface.create(
                            "sans-serif-medium",
                            android.graphics.Typeface.NORMAL,
                        )
                    canvas.drawText(symbol, 160f, 108f, paint)
                    AppEntry(
                        "ep:$id",
                        label,
                        bitmap.asImageBitmap(),
                        null,
                        "home",
                        Color(0xFF030B12),
                        100L,
                        0L,
                    )
                }
                .also { cached = it }
}

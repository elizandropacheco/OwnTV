@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.conreo.couchytv.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.conreo.couchytv.*
import com.conreo.couchytv.R
import com.conreo.couchytv.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

private val Ink = Color(0xFF030B12)
private val Blue = Color(0xFF0084FE)
private val Green = Color(0xFF2CEB43)
private val Silver = Color(0xFFC1D0DA)
private val LocalOwnConfig = staticCompositionLocalOf { EpDefaults.config() }

/**
 * Keep fully visible controls still, including on TVs whose platform default uses a focus pivot.
 */
private object MinimalFocusScrolling : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = offset + size
        return when {
            offset >= -0.5f && end <= containerSize + 0.5f -> 0f
            offset <= 0f && end >= containerSize -> 0f
            offset < 0f -> offset
            end > containerSize -> end - containerSize
            else -> 0f
        }
    }
}

@Composable
private fun Label(
    s: String,
    size: Int = 14,
    color: Color = Color.White,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    BasicText(
        s,
        modifier,
        style =
            TextStyle(
                color = color,
                fontSize = size.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Stable geometry: focus changes only the tint and the thin gradient outline. */
@Composable
private fun Glass(
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
    opacity: Float = .8f,
    imageAlignment: Alignment = Alignment.Center,
    imageScale: ContentScale = ContentScale.Crop,
    blendImageEdge: Boolean = false,
    horizontalScrim: Boolean = false,
    action: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance = LocalOwnConfig.current
    val radius = listOf(4, 6, 8, 10, 12)[appearance.cornerRadius.coerceIn(0, 4)].dp
    var focus by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val tint by
        animateFloatAsState(
            if (focus) 1f else 0f,
            tween(if (appearance.ownEffects) 150 else 0),
            label = "cardFocusTint",
        )
    val shape = RoundedCornerShape(radius)
    val fill =
        if (image == null)
            Brush.linearGradient(listOf(Color(0xFF1C3544).copy(.52f + .05f * tint), Ink.copy(.72f)))
        else Brush.horizontalGradient(listOf(Ink, Color(0xFF092133), Ink))
    Box(
        modifier
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Blue.copy(.22f + .5f * tint),
                        Color(0xFF00BFC6).copy(.12f + .25f * tint),
                        Green.copy(.15f + .45f * tint),
                    ),
                ),
                shape,
            )
            .onFocusChanged { focus = it.isFocused }
            .clickable(interactionSource = interactions, indication = null, onClick = action),
    ) {
        if (image != null) {
            val imageModifier =
                Modifier.matchParentSize()
                    .graphicsLayer {
                        alpha = opacity
                        compositingStrategy =
                            if (blendImageEdge) CompositingStrategy.Offscreen
                            else CompositingStrategy.Auto
                    }
                    .drawWithContent {
                        drawContent()
                        if (blendImageEdge) {
                            val fit =
                                minOf(
                                    size.width / image.width.toFloat(),
                                    size.height / image.height.toFloat(),
                                )
                            val width = image.width * fit
                            val bias =
                                if (imageAlignment == Alignment.CenterStart) 0f
                                else if (imageAlignment == Alignment.Center) .5f else 1f
                            val left = (size.width - width) * bias
                            val feather = minOf(width * .30f, 72.dp.toPx())
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White),
                                    startX = left,
                                    endX = left + feather,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    }
            Image(image, null, imageModifier, contentScale = imageScale, alignment = imageAlignment)
            val scrim =
                if (horizontalScrim)
                    Brush.horizontalGradient(listOf(Ink.copy(.80f), Ink.copy(.45f), Ink.copy(.04f)))
                else Brush.verticalGradient(listOf(Ink.copy(.02f), Ink.copy(.1f), Ink.copy(.88f)))
            Box(Modifier.matchParentSize().background(scrim))
        }
        Column(Modifier.fillMaxSize().padding(14.dp), content = content)
    }
}

@Composable
private fun Art(source: String, max: Int = 960): ImageBitmap? {
    val ctx = LocalContext.current
    return produceState<ImageBitmap?>(null, source, max) {
            value = withContext(Dispatchers.IO) { artwork(ctx, source, max) }
        }
        .value
}

/** Preserve the full 16:9 thumbnail and reserve readable space for the publisher's metadata. */
@Composable
private fun SuggestionCard(item: TvSuggestion, modifier: Modifier) {
    val ctx = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(Ink.copy(.92f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Blue.copy(if (focused) .8f else .30f),
                        Green.copy(if (focused) .6f else .20f),
                    ),
                ),
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                openSuggestion(ctx, item)
            },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF102433))) {
            Art(item.image, 960)?.let {
                Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (item.duration.isNotEmpty())
                Box(
                    Modifier.align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Ink.copy(.90f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Label(item.duration, 10, bold = true, maxLines = 1)
                }
        }
        Column(
            Modifier.fillMaxWidth().height(60.dp).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Label(item.title, 13, bold = true)
            Label(item.metadata, 10, Silver, maxLines = 1)
        }
    }
}

/** Cover the whole surface while retaining the selected image's aspect ratio and focal position. */
@Composable
private fun FeatureCard(card: org.json.JSONObject, modifier: Modifier) {
    val ctx = LocalContext.current
    val alignment =
        when (card.optString("alignment", "right")) {
            "left" -> Alignment.CenterStart
            "center" -> Alignment.Center
            else -> Alignment.CenterEnd
        }
    val art = card.optString("art")
    // The default camera artwork must show both the camera and DVR; its navy edges blend into Ink.
    val scale = if (art == "camera") ContentScale.Fit else ContentScale.Crop
    Glass(
        modifier,
        image = Art(art, 1200),
        opacity = card.optDouble("opacity", .75).toFloat().coerceIn(0f, 1f),
        imageAlignment = alignment,
        imageScale = scale,
        blendImageEdge = art == "camera",
        horizontalScrim = true,
        action = { Actions.launchApp(ctx, card.optString("action")) },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 76.dp
            val showSubtitle = maxHeight >= 38.dp
            Column(
                Modifier.fillMaxWidth(.72f).align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Label(
                    card.optString("title"),
                    if (compact) 16 else 18,
                    bold = true,
                    maxLines = if (compact) 1 else 2,
                )
                val subtitle = card.optString("subtitle")
                if (subtitle.isNotBlank() && showSubtitle)
                    Label(
                        subtitle,
                        if (compact) 10 else 11,
                        Silver,
                        maxLines = if (compact) 1 else 2,
                    )
            }
        }
    }
}

/**
 * Four complete cards, with controls in their own gutters instead of a partially clipped next card.
 */
@Composable
private fun <T> PagedCards(
    title: String,
    entries: List<T>,
    gap: Dp,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
    card: @Composable (T, Modifier) -> Unit,
) {
    if (entries.isEmpty()) return
    var page by rememberSaveable(title) { mutableIntStateOf(0) }
    val lastPage = (entries.size - 1) / 4
    val currentPage = page.coerceIn(0, lastPage)
    LaunchedEffect(lastPage) { if (page > lastPage) page = lastPage }
    val localFocus = remember { FocusRequester() }
    val pageFocus = firstFocus ?: localFocus
    val scope = rememberCoroutineScope()
    val start = currentPage * 4
    val visible = entries.drop(start).take(4)
    fun changePage(next: Int) {
        page = next.coerceIn(0, lastPage)
        scope.launch {
            withFrameNanos {}
            runCatching { pageFocus.requestFocus() }
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label(title, 18, bold = true)
            Spacer(Modifier.weight(1f))
            if (entries.size > 4)
                Label("${start+1}–${start+visible.size} de ${entries.size}", 10, Silver)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val arrowWidth = 30.dp
            val arrowGap = 8.dp
            val cardGap = gap.coerceAtLeast(24.dp)
            val cardWidth =
                ((maxWidth - arrowWidth * 2 - arrowGap * 2 - cardGap * 3) / 4).coerceAtLeast(1.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(arrowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentPage > 0)
                    PageArrow(false, Modifier.width(arrowWidth)) { changePage(currentPage - 1) }
                else Spacer(Modifier.width(arrowWidth))
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement =
                        Arrangement.spacedBy(cardGap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    visible.forEachIndexed { index, entry ->
                        key(start + index) {
                            val navigation = Modifier.onPreviewKeyEvent {
                                if (it.type != KeyEventType.KeyDown) false
                                else if (
                                    it.key == Key.DirectionRight &&
                                        index == visible.lastIndex &&
                                        currentPage < lastPage
                                ) {
                                    changePage(currentPage + 1)
                                    true
                                } else if (
                                    it.key == Key.DirectionLeft && index == 0 && currentPage > 0
                                ) {
                                    changePage(currentPage - 1)
                                    true
                                } else false
                            }
                            card(
                                entry,
                                Modifier.width(cardWidth)
                                    .then(
                                        if (index == 0) Modifier.focusRequester(pageFocus)
                                        else Modifier,
                                    )
                                    .then(navigation),
                            )
                        }
                    }
                }
                if (currentPage < lastPage)
                    PageArrow(true, Modifier.width(arrowWidth)) { changePage(currentPage + 1) }
                else Spacer(Modifier.width(arrowWidth))
            }
        }
    }
}

@Composable
private fun PageArrow(next: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    var focus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(50.dp)
            .semantics { contentDescription = if (next) "Próximos itens" else "Itens anteriores" }
            .clip(shape)
            .background(if (focus) Blue.copy(.3f) else Ink.copy(.42f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Blue.copy(if (focus) .8f else .25f),
                        Green.copy(if (focus) .65f else .15f),
                    ),
                ),
                shape,
            )
            .onFocusChanged { focus = it.isFocused }
            .clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val halfWidth = 3.5.dp.toPx()
            val halfHeight = 6.dp.toPx()
            val x = center.x
            val y = center.y
            val chevron =
                Path().apply {
                    if (next) {
                        moveTo(x - halfWidth, y - halfHeight)
                        lineTo(x + halfWidth, y)
                        lineTo(x - halfWidth, y + halfHeight)
                    } else {
                        moveTo(x + halfWidth, y - halfHeight)
                        lineTo(x - halfWidth, y)
                        lineTo(x + halfWidth, y + halfHeight)
                    }
                }
            drawPath(
                chevron,
                Color.White,
                style =
                    Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun AppImage(app: AppEntry): ImageBitmap? {
    val ctx = LocalContext.current
    val override = ctx.getSharedPreferences("own_cards", 0).getString("art:" + app.pkg, null)
    val brand =
        when (app.pkg) {
            "com.google.android.youtube.tv" -> "youtube"
            "com.spotify.tv.android" -> "spotify"
            "br.com.skymais" -> "sky"
            "tv.twitch.android.app",
            "tv.twitch.android.viewer" -> "twitch"
            "com.globo.globotv" -> "globoplay"
            "ep:ps5" -> "playstation"
            else -> null
        }
    // A final replacement gets no transient system icon while its artwork is loading.
    return when {
        override != null -> Art(override, 640)
        brand != null -> Art("logo:$brand", 640)
        app.pkg == "ep:cameras" -> Art("logo:camera", 640)
        else -> app.icon ?: app.banner
    }
}

/**
 * One shared translucent dock gives the logos room instead of boxing each app into another panel.
 */
@Composable
private fun FavoriteDock(
    apps: List<AppEntry>,
    gap: Dp,
    returnFocus: FocusRequester,
    onEdit: () -> Unit,
    onDown: () -> Boolean,
    iconStyle: String,
    onUp: () -> Unit,
) {
    val ctx = LocalContext.current
    val config = LocalOwnConfig.current
    val height = listOf(86, 98, 110, 122, 134)[config.iconScale.coerceIn(0, 4)].dp
    val shape = RoundedCornerShape(listOf(6, 8, 10, 12, 14)[config.cornerRadius.coerceIn(0, 4)].dp)
    var page by rememberSaveable { mutableIntStateOf(0) }
    val lastPage = (apps.size - 1).coerceAtLeast(0) / 8
    val currentPage = page.coerceIn(0, lastPage)
    LaunchedEffect(lastPage) { if (page > lastPage) page = lastPage }
    val scope = rememberCoroutineScope()
    var dockHasFocus by remember { mutableStateOf(false) }
    var transferringFocus by remember { mutableStateOf(false) }
    fun focusFirst(nextPage: Int) {
        transferringFocus = true
        page = nextPage.coerceIn(0, lastPage)
        scope.launch {
            try {
                withFrameNanos {}
                runCatching { returnFocus.requestFocus() }
            } finally {
                transferringFocus = false
            }
        }
    }
    fun changePage(next: Int) {
        focusFirst(next)
    }
    val dockModifier =
        Modifier.fillMaxWidth()
            .height(height)
            .onFocusChanged { state ->
                val entering = state.hasFocus && !dockHasFocus
                dockHasFocus = state.hasFocus
                // Internal page changes can temporarily remove the focused child; that is not a new
                // entry.
                if (entering && !transferringFocus) focusFirst(0)
            }
            .focusGroup()
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) false
                else
                    when (it.key) {
                        Key.DirectionDown -> onDown()
                        Key.DirectionUp -> {
                            onUp()
                            true
                        }
                        else -> false
                    }
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF173244).copy(.46f), Ink.copy(.58f))))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Blue.copy(.28f), Color(0xFF00BDC8).copy(.1f), Green.copy(.2f)),
                ),
                shape,
            )
    Box(dockModifier) {
        if (apps.isEmpty())
            Box(
                Modifier.fillMaxSize().padding(12.dp).focusRequester(returnFocus),
                contentAlignment = Alignment.CenterStart,
            ) {
                FooterAction("Escolher seus favoritos", onEdit)
            }
        else if (apps.size <= 8)
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gap.coerceAtMost(16.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                apps.forEachIndexed { index, app ->
                    DockItem(
                        app,
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (index == 0) Modifier.focusRequester(returnFocus) else Modifier,
                            ),
                        iconStyle,
                    ) {
                        Actions.launchApp(ctx, app.pkg)
                    }
                }
            }
        else
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                val shown = apps.drop(currentPage * 8).take(8)
                val arrowWidth = 30.dp
                val arrowGap = 8.dp
                val itemGap = gap.coerceAtMost(12.dp)
                val itemWidth =
                    ((maxWidth - arrowWidth * 2 - arrowGap * 2 - itemGap * 7) / 8).coerceAtLeast(
                        1.dp,
                    )
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(arrowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentPage > 0)
                        PageArrow(false, Modifier.width(arrowWidth)) { changePage(currentPage - 1) }
                    else Spacer(Modifier.width(arrowWidth))
                    Row(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalArrangement =
                            Arrangement.spacedBy(itemGap, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        shown.forEachIndexed { index, app ->
                            key(app.pkg) {
                                DockItem(
                                    app,
                                    Modifier.width(itemWidth)
                                        .fillMaxHeight()
                                        .then(
                                            if (index == 0) Modifier.focusRequester(returnFocus)
                                            else Modifier,
                                        )
                                        .onPreviewKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) false
                                            else if (
                                                it.key == Key.DirectionRight &&
                                                    index == shown.lastIndex &&
                                                    currentPage < lastPage
                                            ) {
                                                changePage(currentPage + 1)
                                                true
                                            } else if (
                                                it.key == Key.DirectionLeft &&
                                                    index == 0 &&
                                                    currentPage > 0
                                            ) {
                                                changePage(currentPage - 1)
                                                true
                                            } else false
                                        },
                                    iconStyle,
                                ) {
                                    Actions.launchApp(ctx, app.pkg)
                                }
                            }
                        }
                    }
                    if (currentPage < lastPage)
                        PageArrow(true, Modifier.width(arrowWidth)) { changePage(currentPage + 1) }
                    else Spacer(Modifier.width(arrowWidth))
                }
            }
    }
}

@Composable
private fun DockItem(app: AppEntry, modifier: Modifier, iconStyle: String, action: () -> Unit) {
    val config = LocalOwnConfig.current
    val image = AppImage(app)
    val ctx = LocalContext.current
    val vectorLogo =
        app.pkg in
            setOf(
                "com.google.android.youtube.tv",
                "com.spotify.tv.android",
                "br.com.skymais",
                "tv.twitch.android.app",
                "tv.twitch.android.viewer",
                "com.globo.globotv",
                "ep:ps5",
                "ep:cameras",
            ) && ctx.getSharedPreferences("own_cards", 0).getString("art:" + app.pkg, null) == null
    val iconFilter =
        if (iconStyle == "mono") {
            if (vectorLogo) ColorFilter.tint(Color.White)
            else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else if (vectorLogo)
            ColorFilter.tint(
                when (app.pkg) {
                    "com.google.android.youtube.tv" -> Color(0xFFFF3048)
                    "com.spotify.tv.android" -> Color(0xFF1ED760)
                    "tv.twitch.android.app",
                    "tv.twitch.android.viewer" -> Color(0xFFA970FF)
                    "com.globo.globotv" -> Color(0xFFFF3160)
                    else -> Color(0xFF38A6FF)
                },
            )
        else null
    var focus by remember { mutableStateOf(false) }
    val tint by
        animateFloatAsState(
            if (focus) 1f else 0f,
            tween(if (config.ownEffects) 150 else 0),
            label = "dockFocusTint",
        )
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Blue.copy(.10f * tint), Green.copy(.025f * tint))),
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Blue.copy(.65f * tint), Green.copy(.48f * tint))),
                shape,
            )
            .onFocusChanged { focus = it.isFocused }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = action,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            image?.let {
                Image(
                    it,
                    app.label,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = iconFilter,
                )
            }
        }
        if (config.showAppLabels) Label(app.label, 10, if (focus) Color.White else Silver)
    }
}

@Composable
fun OwnTVApp(rescanTick: Int, homeRequestTick: Int = 0) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val store = remember { ConfigStore(ctx.applicationContext) }
    val config by store.flow.collectAsState(initial = EpDefaults.config())
    val apps by
        produceState<List<AppEntry>>(AppRepository.memoryCache.orEmpty(), rescanTick) {
            value = withContext(Dispatchers.Default) { AppRepository.scan(ctx.applicationContext) }
        }
    var tab by rememberSaveable { mutableStateOf("start") }
    var settings by remember { mutableStateOf(false) }
    var wallpaperTick by remember { mutableIntStateOf(0) }
    var clock by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val homeList = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dockFocus = remember { FocusRequester() }
    val heroFocus = remember { FocusRequester() }
    val heroTopFocus = remember { FocusRequester() }
    val topFocus = remember { FocusRequester() }
    val belowFocus = remember { FocusRequester() }
    LaunchedEffect(homeRequestTick) {
        if (homeRequestTick > 0) {
            settings = false
            tab = "start"
            homeList.scrollToItem(0)
        }
        withFrameNanos {}
        runCatching { topFocus.requestFocus() }
    }
    LaunchedEffect(Unit) {
        OwnWallpapers.schedule(ctx)
        val appearance = ctx.getSharedPreferences("own_appearance", 0)
        if (!appearance.getBoolean("tvScaleAdjusted", false)) {
            store.update { old ->
                old.copy(
                    uiScale = if (old.uiScale == 0) 3 else old.uiScale,
                    iconScale = maxOf(old.iconScale, 2),
                )
            }
            appearance.edit().putBoolean("tvScaleAdjusted", true).apply()
        }
    }
    LaunchedEffect(config.categories, apps) {
        if (tab != "start" && config.categories.none { it.id == tab }) tab = "start"
        if (
            config.categories.map { it.id } == listOf("favorites", "home", "__all__") &&
                apps.isNotEmpty()
        )
            store.update { old ->
                old.copy(
                    categories = EpDefaults.config().categories,
                    sections =
                        old.sections.mapValues { (pkg, ids) ->
                            ids +
                                listOfNotNull(
                                    if (pkg == "ep:ps5") "games"
                                    else
                                        apps
                                            .find { it.pkg == pkg }
                                            ?.autoCategory
                                            ?.takeIf {
                                                it in listOf("streaming", "music", "games")
                                            },
                                )
                        },
                )
            }
    }
    LaunchedEffect(rescanTick, lifecycle, config.h24) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val selected =
                    ctx.getSharedPreferences("own_clock", 0).getString("timezone", "system")
                val zone =
                    if (selected == "system") TimeZone.getDefault()
                    else TimeZone.getTimeZone(selected)
                clock =
                    SimpleDateFormat(if (config.h24) "HH:mm" else "hh:mm a", Locale.getDefault())
                        .apply { timeZone = zone }
                        .format(Date())
                date =
                    SimpleDateFormat("EEE, dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                        .apply { timeZone = zone }
                        .format(Date())
                delay(1000)
            }
        }
    }
    BackHandler {
        if (settings) settings = false
        else if (tab != "start") tab = "start"
        else if (homeList.firstVisibleItemIndex > 0 || homeList.firstVisibleItemScrollOffset > 0)
            scope.launch { homeList.animateScrollToItem(0) }
    }
    val visible =
        remember(apps, config.hidden) {
            apps.filter { it.pkg !in config.hidden && it.pkg != ctx.packageName }
        }
    val nativeFavoriteOrder =
        remember(rescanTick) {
            runCatching {
                    val array =
                        org.json.JSONArray(
                            ctx.getSharedPreferences("own_cards", 0)
                                .getString("favoriteOrder", "[]"),
                        )
                    (0 until array.length())
                        .mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                        .distinct()
                }
                .getOrDefault(emptyList())
        }
    val favorites =
        remember(visible, config, nativeFavoriteOrder) {
            val order = (nativeFavoriteOrder + config.order["favorites"].orEmpty()).distinct()
            visible
                .filter {
                    config.sections[it.pkg]?.contains("favorites")
                        ?: (it.pkg in EpDefaults.favorites)
                }
                .sortedWith(
                    compareBy<AppEntry> {
                            val i = order.indexOf(it.pkg)
                            if (i < 0) Int.MAX_VALUE else i
                        }
                        .thenBy { it.label },
                )
        }
    val favoritePackages = remember(favorites) { favorites.map { it.pkg } }
    LaunchedEffect(favoritePackages) {
        ctx.getSharedPreferences("own_cards", 0)
            .edit()
            .putString("visibleFavorites", org.json.JSONArray(favoritePackages).toString())
            .apply()
    }
    val groups = remember(visible, config) { AppRepository.categorize(visible, config) }
    val tabs =
        listOf(CategoryCfg("start", "Início")) + config.categories.filter { it.id != "favorites" }
    val shown =
        if (tab == "start") favorites
        else groups.firstOrNull { it.first.id == tab }?.second.orEmpty()
    val hero =
        remember(rescanTick) {
            val a = OwnCards.read(ctx, "hero")
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }
        }
    val gameCards =
        remember(rescanTick) {
            val a = OwnCards.read(ctx, "games")
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }
        }
    val showSuggestions =
        remember(rescanTick) {
            ctx.getSharedPreferences("own_cards", 0).getBoolean("recommendations", true)
        }
    val recs by
        produceState<List<TvSuggestion>>(emptyList(), rescanTick, showSuggestions) {
            if (showSuggestions)
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    val entries = withContext(Dispatchers.IO) { suggestions(ctx) }
                    value = entries
                    value = OwnYouTubeMetadata.enrich(ctx, entries)
                }
            else value = emptyList()
        }
    val brandMode = remember(rescanTick) { OwnBrand.mode(ctx) }
    val brandCustom =
        remember(rescanTick) {
            ctx.getSharedPreferences("own_brand", 0).getString("custom", "").orEmpty()
        }
    val favoriteIconStyle =
        remember(rescanTick) {
            ctx.getSharedPreferences("own_cards", 0)
                .getString("favoriteIconStyle", "brand")
                .orEmpty()
        }
    val visibleRecommendations = recs.filter { it.publisher !in config.hidden }
    val youtubeRecommendations =
        visibleRecommendations
            .filter { it.publisher == "com.google.android.youtube.tv" }
            .sortedBy { if (it.id.startsWith("watch_next_program:")) 0 else 1 }
    val otherRecommendations =
        visibleRecommendations
            .filter { it.publisher != "com.google.android.youtube.tv" }
            .groupBy { it.group }
    val showYouTubeRow = showSuggestions && "com.google.android.youtube.tv" !in config.hidden
    val extraHero = hero.drop(2)
    val hasBelow =
        extraHero.isNotEmpty() ||
            showYouTubeRow ||
            (showSuggestions && otherRecommendations.isNotEmpty())
    val newsEnabled =
        remember(rescanTick) { ctx.getSharedPreferences("own_news", 0).getBoolean("enabled", true) }
    val newsTopics = remember(rescanTick) { OwnNews.topics(ctx) }
    val newsInterval =
        remember(rescanTick) {
            ctx.getSharedPreferences("own_news", 0).getInt("intervalSeconds", 12).coerceIn(5, 600) *
                1000L
        }
    val headlines by
        produceState<List<OwnHeadline>>(emptyList(), rescanTick, newsEnabled, newsTopics) {
            value =
                if (newsEnabled)
                    withContext(Dispatchers.IO) {
                        try {
                            OwnNews.load(ctx)
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (error: Exception) {
                            emptyList()
                        }
                    }
                else emptyList()
        }
    var headlineIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(headlines, lifecycle, newsEnabled, newsInterval) {
        headlineIndex = 0
        if (newsEnabled && headlines.isNotEmpty())
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(newsInterval)
                    headlineIndex = (headlineIndex + 1) % headlines.size
                }
            }
    }
    val headline = if (headlines.isEmpty()) null else headlines[headlineIndex % headlines.size]
    val photo by
        produceState<ImageBitmap?>(null, rescanTick, wallpaperTick, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val prefs = ctx.getSharedPreferences("own_appearance", 0)
                val mode = prefs.getString("mode", "slideshow")
                val interval = prefs.getInt("intervalSeconds", 60).coerceIn(15, 600) * 1000L
                val started =
                    prefs
                        .getLong("rotationStartedAt", System.currentTimeMillis())
                        .coerceAtMost(System.currentTimeMillis())
                prefs.edit().putLong("rotationStartedAt", started).apply()
                do {
                    val collection = OwnWallpapers.available(ctx)
                    val index =
                        if (collection.isEmpty()) 0
                        else
                            (((System.currentTimeMillis() - started).coerceAtLeast(0) / interval) %
                                    collection.size)
                                .toInt()
                    val selected = prefs.getString("selected", "asset:california.jpg").orEmpty()
                    val source =
                        when (mode) {
                            "slideshow" -> collection.getOrNull(index)
                            "collection" ->
                                if (selected.startsWith("/") || selected.startsWith("asset:"))
                                    selected
                                else "asset:$selected.jpg"
                            "manual" -> java.io.File(ctx.filesDir, "wallpaper.jpg").path
                            else -> null
                        }
                    // Match the actual UI surface; originals in assets/private storage retain their
                    // full resolution.
                    value =
                        withContext(Dispatchers.IO) {
                            source?.let {
                                artwork(
                                    ctx,
                                    it,
                                    ctx.resources.displayMetrics.widthPixels.coerceAtMost(3840),
                                    ctx.resources.displayMetrics.heightPixels.coerceAtMost(2160),
                                )
                            }
                        }
                    if (mode != "slideshow") break
                    prefs.edit().putInt("slideIndex", index).apply()
                    delay(
                        (interval -
                                ((System.currentTimeMillis() - started).coerceAtLeast(0) %
                                    interval))
                            .coerceAtLeast(200),
                    )
                } while (isActive)
            }
        }
    val gap = listOf(8, 12, 16, 20, 24)[config.spacing.coerceIn(0, 4)].dp
    val appWidth = listOf(120, 140, 160, 180, 200)[config.iconScale.coerceIn(0, 4)].dp
    val heroHeight = listOf(190, 218, 244, 270)[config.ownHeroHeight.coerceIn(0, 3)].dp
    fun openBelow(): Boolean {
        if (!hasBelow) return false
        scope.launch {
            homeList.animateScrollToItem(1)
            withFrameNanos {}
            runCatching { belowFocus.requestFocus() }
        }
        return true
    }
    val returnToDock = Modifier.onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
            scope.launch {
                homeList.animateScrollToItem(0)
                withFrameNanos {}
                runCatching { dockFocus.requestFocus() }
            }
            true
        } else false
    }
    CompositionLocalProvider(
        LocalOwnConfig provides config,
        LocalBringIntoViewSpec provides MinimalFocusScrolling,
    ) {
        ScaledUi(
            uiScaleFactor(
                config.uiScale,
                androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp,
            ),
        ) {
            Box(Modifier.fillMaxSize().background(Ink)) {
                if (config.ownEffects)
                    Crossfade(
                        photo,
                        Modifier.fillMaxSize(),
                        animationSpec = tween(1000),
                        label = "wallpaper",
                    ) { frame ->
                        frame?.let {
                            Image(
                                it,
                                null,
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                else
                    photo?.let {
                        Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                Canvas(Modifier.fillMaxSize()) {
                    val dim =
                        listOf(.15f, .3f, .45f, .6f, .75f)[config.ownWallpaperDim.coerceIn(0, 4)]
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Ink.copy((dim + .15f).coerceAtMost(1f)),
                                Ink.copy((dim - .18f).coerceAtLeast(0f)),
                                Ink.copy((dim + .26f).coerceAtMost(1f)),
                            ),
                        ),
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Ink.copy(.22f), Color.Transparent, Color.Transparent),
                        ),
                    )
                    for (i in 0..4) {
                        val p =
                            Path().apply {
                                moveTo(size.width * (.62f + i * .055f), -20f)
                                lineTo(size.width * (.88f + i * .055f), size.height * .46f)
                                lineTo(size.width * (.66f + i * .055f), size.height * .89f)
                                lineTo(size.width * (.89f + i * .055f), size.height * 1.2f)
                            }
                        drawPath(
                            p,
                            Brush.linearGradient(
                                listOf(Blue.copy(.025f), Blue.copy(.21f), Green.copy(.11f)),
                            ),
                            style = Stroke(1.2f + i * .35f),
                        )
                    }
                }
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF173244).copy(.46f), Ink.copy(.48f)),
                                ),
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(Blue.copy(.16f), Color.Transparent, Green.copy(.13f)),
                                ),
                                RoundedCornerShape(12.dp),
                            )
                            .onPreviewKeyEvent {
                                if (
                                    tab == "start" &&
                                        it.type == KeyEventType.KeyDown &&
                                        it.key == Key.DirectionDown
                                ) {
                                    scope.launch {
                                        homeList.scrollToItem(0)
                                        withFrameNanos {}
                                        runCatching { dockFocus.requestFocus() }
                                    }
                                    true
                                } else false
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (brandMode == "custom" && brandCustom.isNotEmpty())
                            Art(brandCustom, 512)?.let {
                                Image(
                                    it,
                                    "Logo personalizado",
                                    Modifier.width(48.dp).height(30.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        else
                            Image(
                                painterResource(
                                    if (brandMode == "owntv") R.drawable.own_monogram
                                    else R.drawable.own_ep_monogram,
                                ),
                                if (brandMode == "owntv") "OwnTV" else "Elizandro Pacheco",
                                Modifier.width(if (brandMode == "owntv") 30.dp else 48.dp)
                                    .height(30.dp),
                                contentScale = ContentScale.Fit,
                            )
                        Spacer(Modifier.width(6.dp))
                        Label("OwnTV", 13, bold = true)
                        Spacer(Modifier.width(12.dp))
                        Row(
                            Modifier.weight(1f)
                                .focusRequester(topFocus)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tabs.forEach { category ->
                                NavAction(category.name, tab == category.id) {
                                    tab = category.id
                                    if (category.id == "start")
                                        scope.launch { homeList.animateScrollToItem(0) }
                                }
                            }
                        }
                        if (config.showStatusBar)
                            Column(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .clickable { Actions.launchApp(ctx, "ep:datetime") }
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                Label(clock, 15, bold = true)
                                Label(date, 9, Silver)
                            }
                    }
                    if (tab == "start") {
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                            val homeViewportHeight = maxHeight
                            LazyColumn(
                                state = homeList,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                item(key = "openHome") {
                                    Column(
                                        Modifier.fillMaxWidth()
                                            .height(homeViewportHeight)
                                            .padding(top = 12.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            Modifier.weight(1f).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(
                                                Modifier.weight(1.25f)
                                                    .padding(start = 5.dp, end = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Label(
                                                    "ENTRETENIMENTO · JOGOS · CASA",
                                                    10,
                                                    Green,
                                                    true,
                                                )
                                                Label(
                                                    "Seu universo.\nDo seu jeito.",
                                                    34,
                                                    bold = true,
                                                )
                                                if (newsEnabled) HeadlineStrip(headline)
                                            }
                                            if (hero.isNotEmpty())
                                                BoxWithConstraints(
                                                    Modifier.weight(.86f).fillMaxHeight(),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    val cardsHeight = minOf(heroHeight, maxHeight)
                                                    Column(
                                                        Modifier.fillMaxWidth().height(cardsHeight),
                                                        verticalArrangement =
                                                            Arrangement.spacedBy(24.dp),
                                                    ) {
                                                        hero.take(2).forEachIndexed { index, card ->
                                                            FeatureCard(
                                                                card,
                                                                Modifier.fillMaxWidth()
                                                                    .weight(1f)
                                                                    .focusRequester(
                                                                        if (index == 0) heroTopFocus
                                                                        else heroFocus,
                                                                    )
                                                                    .onPreviewKeyEvent {
                                                                        if (
                                                                            it.type !=
                                                                                KeyEventType.KeyDown
                                                                        )
                                                                            false
                                                                        else if (
                                                                            it.key ==
                                                                                Key.DirectionDown
                                                                        ) {
                                                                            runCatching {
                                                                                (if (
                                                                                        index ==
                                                                                            0 &&
                                                                                            hero
                                                                                                .size >
                                                                                                1
                                                                                    )
                                                                                        heroFocus
                                                                                    else dockFocus)
                                                                                    .requestFocus()
                                                                            }
                                                                            true
                                                                        } else if (
                                                                            it.key ==
                                                                                Key.DirectionUp
                                                                        ) {
                                                                            runCatching {
                                                                                (if (index == 0)
                                                                                        topFocus
                                                                                    else
                                                                                        heroTopFocus)
                                                                                    .requestFocus()
                                                                            }
                                                                            true
                                                                        } else false
                                                                    },
                                                            )
                                                        }
                                                    }
                                                }
                                        }
                                        Row(
                                            Modifier.fillMaxWidth().height(28.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Label(
                                                config.categories
                                                    .find { it.id == "favorites" }
                                                    ?.name ?: "Seus favoritos",
                                                15,
                                                bold = true,
                                            )
                                            Spacer(Modifier.weight(1f))
                                        }
                                        FavoriteDock(
                                            favorites,
                                            gap,
                                            dockFocus,
                                            { settings = true },
                                            ::openBelow,
                                            favoriteIconStyle,
                                        ) {
                                            runCatching {
                                                (if (hero.size > 1) heroFocus
                                                    else if (hero.isNotEmpty()) heroTopFocus
                                                    else topFocus)
                                                    .requestFocus()
                                            }
                                        }
                                        if (hasBelow)
                                            Row(
                                                Modifier.fillMaxWidth().height(28.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                FooterAction("↓  Explorar seus conteúdos") {
                                                    openBelow()
                                                }
                                            }
                                    }
                                }
                                if (showYouTubeRow) {
                                    item(key = "youtubeFirst") {
                                        if (youtubeRecommendations.isNotEmpty())
                                            PagedCards(
                                                if (
                                                    youtubeRecommendations.any {
                                                        it.id.startsWith("watch_next_program:")
                                                    }
                                                )
                                                    "Continuar no YouTube"
                                                else "Recomendações do YouTube",
                                                youtubeRecommendations,
                                                gap,
                                                modifier = returnToDock,
                                                firstFocus = belowFocus,
                                            ) { suggestion, modifier ->
                                                SuggestionCard(suggestion, modifier)
                                            }
                                        else
                                            Column(
                                                returnToDock,
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Label("YouTube", 18, bold = true)
                                                Box(
                                                    Modifier.fillMaxWidth().padding(12.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Glass(
                                                        Modifier.widthIn(max = 620.dp)
                                                            .fillMaxWidth()
                                                            .height(100.dp)
                                                            .focusRequester(belowFocus),
                                                        action = {
                                                            Actions.launchApp(
                                                                ctx,
                                                                "com.google.android.youtube.tv",
                                                            )
                                                        },
                                                    ) {
                                                        Row(
                                                            Modifier.fillMaxSize(),
                                                            verticalAlignment =
                                                                Alignment.CenterVertically,
                                                            horizontalArrangement =
                                                                Arrangement.spacedBy(20.dp),
                                                        ) {
                                                            Art("logo:youtube", 640)?.let {
                                                                Image(
                                                                    it,
                                                                    "YouTube",
                                                                    Modifier.width(80.dp)
                                                                        .fillMaxHeight(),
                                                                    contentScale = ContentScale.Fit,
                                                                )
                                                            }
                                                            Column(
                                                                Modifier.weight(1f),
                                                                verticalArrangement =
                                                                    Arrangement.spacedBy(6.dp),
                                                            ) {
                                                                Label(
                                                                    "Abrir o YouTube",
                                                                    17,
                                                                    bold = true,
                                                                )
                                                                Label(
                                                                    "Veja as recomendações no aplicativo.",
                                                                    12,
                                                                    Silver,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    }
                                }
                                if (showSuggestions)
                                    otherRecommendations.entries.forEachIndexed {
                                        groupIndex,
                                        (group, entries) ->
                                        item(key = "recommendations:$group") {
                                            val first = groupIndex == 0 && !showYouTubeRow
                                            PagedCards(
                                                group,
                                                entries,
                                                gap,
                                                modifier = if (first) returnToDock else Modifier,
                                                firstFocus = if (first) belowFocus else null,
                                            ) { suggestion, modifier ->
                                                SuggestionCard(suggestion, modifier)
                                            }
                                        }
                                    }
                                if (extraHero.isNotEmpty())
                                    item(key = "moreShortcuts") {
                                        val first =
                                            !showYouTubeRow &&
                                                (!showSuggestions || otherRecommendations.isEmpty())
                                        PagedCards(
                                            "Seus atalhos",
                                            extraHero,
                                            gap,
                                            modifier = if (first) returnToDock else Modifier,
                                            firstFocus = if (first) belowFocus else null,
                                        ) { card, modifier ->
                                            FeatureCard(card, modifier.aspectRatio(16f / 9f))
                                        }
                                    }
                                item(key = "contentEnd") { Spacer(Modifier.height(16.dp)) }
                            }
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Label(tabs.find { it.id == tab }?.name.orEmpty(), 26, bold = true)
                            Spacer(Modifier.weight(1f))
                            FooterAction("Personalizar") { settings = true }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(appWidth),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(gap.coerceAtLeast(24.dp)),
                            verticalArrangement = Arrangement.spacedBy(gap.coerceAtLeast(24.dp)),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            if (tab == "games")
                                items(gameCards.size) { n ->
                                    FeatureCard(
                                        gameCards[n],
                                        Modifier.fillMaxWidth().height(138.dp),
                                    )
                                }
                            items(shown, key = { it.pkg }) { app ->
                                AppGlass(app, Modifier.fillMaxWidth()) {
                                    Actions.launchApp(ctx, app.pkg)
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(gap.coerceAtMost(16.dp)),
                    ) {
                        Label("OwnTV", 10, Silver.copy(.7f), modifier = Modifier.weight(1f))
                        FooterAction("Personalizar apps") { settings = true }
                        FooterAction("Cards e jogos") { Actions.launchApp(ctx, "ep:customize") }
                        FooterAction("Configurações") { Actions.launchApp(ctx, "ep:settings") }
                        FooterAction("Sobre") { Actions.launchApp(ctx, "ep:about") }
                    }
                }
                if (settings)
                    LiteTvTheme(accent = Blue) {
                        SettingsSheet(
                            config,
                            apps,
                            store,
                            { settings = false },
                            {
                                ctx.getSharedPreferences("own_appearance", 0)
                                    .edit()
                                    .putBoolean("photo", true)
                                    .putString("mode", "manual")
                                    .apply()
                                wallpaperTick++
                            },
                            { settings = false },
                        )
                    }
            }
        }
    }
}

/** Fixed two-line area: a headline change never changes the Home's measurements or focus. */
@Composable
private fun HeadlineStrip(headline: OwnHeadline?) {
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        if (headline != null) {
            val text = buildAnnotatedString {
                withStyle(SpanStyle(color = Green.copy(.9f), fontWeight = FontWeight.Medium)) {
                    append(headline.topic)
                    append(" · ")
                    append(headline.source)
                    append("  ")
                }
                append(headline.title)
            }
            BasicText(
                text,
                Modifier.fillMaxSize(),
                style = TextStyle(color = Silver, fontSize = 14.sp, lineHeight = 20.sp),
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavAction(label: String, active: Boolean, action: () -> Unit) {
    var focus by remember { mutableStateOf(false) }
    val tint by animateFloatAsState(if (focus) 1f else 0f, tween(150), label = "navFocusTint")
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Blue.copy(if (active) .23f else .24f * tint), Green.copy(.025f * tint)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Blue.copy(if (active) .4f else .75f * tint),
                        Green.copy(if (active) .2f else .6f * tint),
                    ),
                ),
                shape,
            )
            .onFocusChanged { focus = it.isFocused }
            .clickable(onClick = action)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Label(label, 13, if (active || focus) Color.White else Silver, active)
    }
}

@Composable
private fun FooterAction(label: String, action: () -> Unit) {
    var focus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.clip(shape)
            .background(if (focus) Blue.copy(.24f) else Color.Transparent)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Blue.copy(if (focus) .65f else 0f), Green.copy(if (focus) .45f else 0f)),
                ),
                shape,
            )
            .onFocusChanged { focus = it.isFocused }
            .clickable(onClick = action)
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        Label(label, 10, if (focus) Color.White else Silver)
    }
}

@Composable
private fun AppGlass(app: AppEntry, modifier: Modifier, action: () -> Unit) {
    val config = LocalOwnConfig.current
    val image = AppImage(app)
    Glass(
        modifier.height(listOf(100, 112, 124, 136, 148)[config.iconScale.coerceIn(0, 4)].dp),
        action = action,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            image?.let {
                Image(it, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        if (config.showAppLabels) {
            Spacer(Modifier.height(7.dp))
            Label(app.label, 11)
        }
    }
}

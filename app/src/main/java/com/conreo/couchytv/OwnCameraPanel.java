package com.conreo.couchytv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;
import org.json.JSONObject;
import java.util.*;

/** Remote-first camera gallery. Credentials stay in memory; never log stream URLs. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class OwnCameraPanel extends LinearLayout {
    private final JSONObject profile;
    private final Runnable menu;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Tile> tiles = new ArrayList<>();
    private final int count;
    private ScrollView scroll;
    private LinearLayout toolbar;
    private int pageGeneration = 0;
    private TextView heading;
    private int expanded = 0, selected = 1, savedScroll = 0;
    private boolean stopped = false, light = false;
    private final Runnable updateVisible = this::updateVisible;

    public OwnCameraPanel(Context context, JSONObject profile, Runnable menu, Bundle saved) {
        super(context);
        this.profile = profile;
        this.menu = menu;
        count = Math.max(1, Math.min(64, profile.optInt("channels", 4)));
        android.content.SharedPreferences viewPrefs =
                context.getSharedPreferences("own_camera_view", 0);
        if (!viewPrefs.getBoolean("fillDefaultApplied", false))
            viewPrefs.edit().putInt("resize", 4).putBoolean("fillDefaultApplied", true).apply();
        setOrientation(VERTICAL);
        setPadding(dp(34), dp(16), dp(34), dp(16));
        setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR, new int[] {0xFF030B12, 0xFF0B202A}));
        if (saved != null) {
            selected = Math.max(1, Math.min(count, saved.getInt("selected", 1)));
            savedScroll = Math.max(0, saved.getInt("scroll", 0));
        }
        if (saved != null && saved.getInt("expanded", 0) > 0)
            showExpanded(
                    Math.min(count, saved.getInt("expanded")), saved.getBoolean("light", false));
        else showGallery();
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView text(String s, int size) {
        TextView v = new TextView(getContext());
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(0xFFE3EDF4);
        return v;
    }

    private int resizeMode() {
        int mode = getContext().getSharedPreferences("own_camera_view", 0).getInt("resize", 4);
        return mode == 0 || mode == 3 || mode == 4 ? mode : 4;
    }

    private Drawable surface(boolean focus) {
        GradientDrawable inside =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR, new int[] {0xFF102330, 0xFF07131D});
        inside.setCornerRadius(dp(7));
        GradientDrawable edge =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        focus
                                ? new int[] {0xFF0084FE, 0xFF15BBA3, 0xFF2CEB43}
                                : new int[] {0x704982AA, 0x50366672, 0x6050A56F});
        edge.setCornerRadius(dp(8));
        LayerDrawable d = new LayerDrawable(new Drawable[] {edge, inside});
        d.setLayerInset(1, dp(1), dp(1), dp(1), dp(1));
        return d;
    }

    private Button control(LinearLayout row, String title, int icon, Runnable action) {
        Button b = new Button(getContext());
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        b.setBackground(surface(false));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setLetterSpacing(0f);
        b.setSingleLine(true);
        CameraIcon drawable = new CameraIcon(icon);
        drawable.setBounds(0, 0, dp(16), dp(16));
        b.setCompoundDrawables(drawable, null, null, null);
        b.setCompoundDrawablePadding(dp(7));
        b.setOnFocusChangeListener((v, f) -> v.setBackground(surface(f)));
        b.setOnClickListener(v -> action.run());
        LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(36));
        if (row.getChildCount() > 0) p.leftMargin = dp(8);
        row.addView(b, p);
        return b;
    }

    private void navigationControl(
            LinearLayout row, String title, int icon, boolean enabled, Runnable action) {
        Button b = control(row, title, icon, action);
        b.setContentDescription(title + " câmera");
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : .38f);
    }

    /** Small vector-style icons share a 24-unit grid and stay centered beside the label. */
    private final class CameraIcon extends Drawable {
        static final int OPTIONS = 0,
                FRAME = 1,
                CAMERA = 2,
                GRID = 3,
                PREVIOUS = 4,
                NEXT = 5,
                QUALITY = 6,
                REFRESH = 7;
        private final int kind;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        CameraIcon(int kind) {
            this.kind = kind;
            paint.setColor(0xFFE3EDF4);
            paint.setStrokeWidth(1.7f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(Canvas canvas) {
            int save = canvas.save();
            Rect b = getBounds();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 24f, b.height() / 24f);
            path.reset();
            switch (kind) {
                case OPTIONS:
                    canvas.drawLine(4, 6, 20, 6, paint);
                    canvas.drawLine(4, 12, 20, 12, paint);
                    canvas.drawLine(4, 18, 20, 18, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(9, 6, 2.4f, paint);
                    canvas.drawCircle(15, 12, 2.4f, paint);
                    canvas.drawCircle(8, 18, 2.4f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;
                case FRAME:
                    path.moveTo(9, 4);
                    path.lineTo(4, 4);
                    path.lineTo(4, 9);
                    path.moveTo(15, 4);
                    path.lineTo(20, 4);
                    path.lineTo(20, 9);
                    path.moveTo(20, 15);
                    path.lineTo(20, 20);
                    path.lineTo(15, 20);
                    path.moveTo(9, 20);
                    path.lineTo(4, 20);
                    path.lineTo(4, 15);
                    canvas.drawPath(path, paint);
                    break;
                case CAMERA:
                    canvas.drawRoundRect(3, 6, 16, 18, 2, 2, paint);
                    path.moveTo(16, 10);
                    path.lineTo(21, 7);
                    path.lineTo(21, 17);
                    path.lineTo(16, 14);
                    canvas.drawPath(path, paint);
                    break;
                case GRID:
                    canvas.drawRoundRect(3, 3, 10, 10, 1, 1, paint);
                    canvas.drawRoundRect(14, 3, 21, 10, 1, 1, paint);
                    canvas.drawRoundRect(3, 14, 10, 21, 1, 1, paint);
                    canvas.drawRoundRect(14, 14, 21, 21, 1, 1, paint);
                    break;
                case PREVIOUS:
                case NEXT:
                    float direction = kind == PREVIOUS ? -1 : 1;
                    canvas.drawLine(12 - direction * 7, 12, 12 + direction * 7, 12, paint);
                    path.moveTo(12 + direction, 6);
                    path.lineTo(12 + direction * 7, 12);
                    path.lineTo(12 + direction, 18);
                    canvas.drawPath(path, paint);
                    break;
                case QUALITY:
                    canvas.drawRoundRect(3, 4, 21, 18, 2, 2, paint);
                    canvas.drawLine(8, 21, 16, 21, paint);
                    canvas.drawLine(9, 9, 9, 13, paint);
                    canvas.drawLine(15, 9, 15, 13, paint);
                    canvas.drawLine(9, 11, 15, 11, paint);
                    break;
                default:
                    canvas.drawArc(4, 4, 20, 20, 40, 285, false, paint);
                    path.moveTo(15, 3);
                    path.lineTo(20, 5);
                    path.lineTo(18, 10);
                    canvas.drawPath(path, paint);
                    break;
            }
            canvas.restoreToCount(save);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private void clearPage() {
        pageGeneration++;
        releaseAll();
        tiles.clear();
        removeAllViews();
        scroll = null;
        toolbar = null;
    }

    private void header(String title, String hint) {
        heading = text(title, 23);
        heading.setTypeface(null, Typeface.BOLD);
        addView(heading);
        TextView help = text(hint, 12);
        help.setTextColor(0xFFAABCC8);
        LayoutParams h = new LayoutParams(-1, -2);
        h.topMargin = dp(4);
        h.bottomMargin = dp(10);
        addView(help, h);
    }

    private void showGallery() {
        clearPage();
        expanded = 0;
        light = false;
        header(
                "Câmeras · " + count + " canais",
                "Setas navegar e rolar · OK abrir em alta resolução · Voltar opções");
        toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(HORIZONTAL);
        addView(toolbar);
        control(toolbar, "Opções do DVR", CameraIcon.OPTIONS, menu);
        control(toolbar, "Enquadrar", CameraIcon.FRAME, this::chooseResize);
        control(
                toolbar,
                "Ir para câmera",
                CameraIcon.CAMERA,
                () -> {
                    String[] names = new String[count];
                    for (int i = 0; i < count; i++) names[i] = "Câmera " + (i + 1);
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Ir para câmera")
                            .setItems(names, (d, n) -> tiles.get(n).requestFocus())
                            .show();
                });
        scroll = new ScrollView(getContext());
        scroll.setFillViewport(false);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(8), dp(4), 0);
        LayoutParams sp = new LayoutParams(-1, 0, 1);
        sp.topMargin = dp(8);
        addView(scroll, sp);
        LinearLayout rows = new LinearLayout(getContext());
        rows.setOrientation(VERTICAL);
        scroll.addView(rows);
        int available = getResources().getDisplayMetrics().widthPixels - dp(72);
        int height = (int) ((available - dp(16)) / 2f * 9 / 16) + dp(30);
        LinearLayout row = null;
        for (int channel = 1; channel <= count; channel++) {
            if (channel % 2 == 1) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                LayoutParams rp = new LayoutParams(-1, height);
                rp.bottomMargin = dp(12);
                rows.addView(row, rp);
            }
            Tile tile = new Tile(channel, false);
            tiles.add(tile);
            LayoutParams lp = new LayoutParams(0, -1, 1);
            if (channel % 2 == 0) lp.leftMargin = dp(16);
            row.addView(tile, lp);
            if (channel == count && count % 2 == 1) {
                Space gap = new Space(getContext());
                LayoutParams gp = new LayoutParams(0, -1, 1);
                gp.leftMargin = dp(16);
                row.addView(gap, gp);
            }
        }
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            if (i >= 2) t.setNextFocusUpId(tiles.get(i - 2).getId());
            if (i + 2 < tiles.size()) t.setNextFocusDownId(tiles.get(i + 2).getId());
            t.setNextFocusLeftId(i % 2 == 1 ? tiles.get(i - 1).getId() : t.getId());
            t.setNextFocusRightId(
                    i % 2 == 0 && i + 1 < tiles.size() ? tiles.get(i + 1).getId() : t.getId());
        }
        scroll.setOnScrollChangeListener(
                (View.OnScrollChangeListener) (v, x, y, oldX, oldY) -> scheduleVisible());
        final int currentPage = pageGeneration;
        final ScrollView currentScroll = scroll;
        scroll.post(
                () -> {
                    if (currentPage != pageGeneration
                            || stopped
                            || scroll != currentScroll
                            || expanded != 0
                            || tiles.isEmpty()) return;
                    scroll.scrollTo(0, savedScroll);
                    tiles.get(Math.min(selected, count) - 1).requestFocus();
                    scheduleVisible();
                });
    }

    private void showExpanded(int channel, boolean useLight) {
        if (scroll != null) savedScroll = scroll.getScrollY();
        clearPage();
        expanded = channel;
        selected = channel;
        light = useLight;
        header(
                "Câmera " + channel + " de " + count,
                useLight
                        ? "Fluxo leve · prévia do DVR"
                        : "Fluxo principal · alta resolução do DVR");
        toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(HORIZONTAL);
        addView(toolbar);
        control(toolbar, "Mosaico", CameraIcon.GRID, this::showGallery);
        navigationControl(
                toolbar,
                "Anterior",
                CameraIcon.PREVIOUS,
                channel > 1,
                () -> showExpanded(channel - 1, light));
        navigationControl(
                toolbar,
                "Próxima",
                CameraIcon.NEXT,
                channel < count,
                () -> showExpanded(channel + 1, light));
        control(
                toolbar,
                useLight ? "Alta resolução" : "Modo leve",
                CameraIcon.QUALITY,
                () -> showExpanded(channel, !light));
        control(toolbar, "Enquadrar", CameraIcon.FRAME, this::chooseResize);
        control(toolbar, "Reconectar", CameraIcon.REFRESH, () -> showExpanded(channel, light));
        Tile tile = new Tile(channel, !useLight);
        tiles.add(tile);
        LayoutParams p = new LayoutParams(-1, 0, 1);
        p.topMargin = dp(12);
        addView(tile, p);
        final int currentPage = pageGeneration;
        tile.post(
                () -> {
                    if (currentPage == pageGeneration
                            && expanded == channel
                            && !stopped
                            && tiles.contains(tile)) {
                        tile.requestFocus();
                        tile.start();
                    }
                });
    }

    private void chooseResize() {
        int current = resizeMode();
        int[] values = {4, 0, 3};
        String[] labels = {
            "Preencher · recortar bordas",
            "Original · manter proporção",
            "Esticar · ocupar todo o quadro"
        };
        int selected = current == 0 ? 1 : current == 3 ? 2 : 0;
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Enquadramento das câmeras")
                .setSingleChoiceItems(
                        labels,
                        selected,
                        (d, n) -> {
                            getContext()
                                    .getSharedPreferences("own_camera_view", 0)
                                    .edit()
                                    .putInt("resize", values[n])
                                    .apply();
                            for (Tile tile : tiles) tile.video.setResizeMode(values[n]);
                            d.dismiss();
                        })
                .show();
    }

    private void scheduleVisible() {
        handler.removeCallbacks(updateVisible);
        handler.postDelayed(updateVisible, 180);
    }

    private void updateVisible() {
        if (stopped || expanded != 0 || scroll == null) return;
        Rect viewport = new Rect();
        if (!scroll.getGlobalVisibleRect(viewport)) return;
        ArrayList<Tile> visible = new ArrayList<>();
        for (Tile t : tiles) {
            Rect r = new Rect();
            if (t.getGlobalVisibleRect(r) && r.intersect(viewport) && r.height() > dp(50))
                visible.add(t);
        }
        visible.sort((a, b) -> Boolean.compare(b.hasFocus(), a.hasFocus()));
        Set<Tile> active = new HashSet<>(visible.subList(0, Math.min(4, visible.size())));
        for (Tile t : tiles) if (!active.contains(t)) t.stop();
        for (Tile t : active) t.start();
    }

    private void releaseAll() {
        handler.removeCallbacksAndMessages(null);
        for (Tile t : tiles) t.stop();
    }

    public void pause() {
        stopped = true;
        releaseAll();
    }

    public void resume() {
        stopped = false;
        if (expanded > 0) {
            if (!tiles.isEmpty()) tiles.get(0).start();
        } else scheduleVisible();
    }

    public void shutdown() {
        pageGeneration++;
        pause();
        tiles.clear();
        scroll = null;
    }

    public Bundle saveState() {
        Bundle state = new Bundle();
        state.putInt("selected", selected);
        state.putInt("expanded", expanded);
        state.putInt("scroll", scroll != null ? scroll.getScrollY() : savedScroll);
        state.putBoolean("light", light);
        return state;
    }

    public boolean back() {
        if (expanded > 0) {
            showGallery();
            return true;
        }
        return false;
    }

    private ViewOutlineProvider roundedOutline(final int radius) {
        return new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        };
    }

    private final class Tile extends LinearLayout {
        final int channel;
        final boolean main;
        final PlayerView video;
        final TextView status;
        ExoPlayer player;
        boolean rendered = false;
        String dimensions = "";
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF ringBounds = new RectF();
        private LinearGradient focusedRing, idleRing;

        Tile(int channel, boolean main) {
            super(OwnCameraPanel.this.getContext());
            this.channel = channel;
            this.main = main;
            setId(View.generateViewId());
            setOrientation(VERTICAL);
            setPadding(dp(3), dp(3), dp(3), dp(3));
            setBackgroundColor(0xFF07131D);
            setClipChildren(true);
            setClipToPadding(true);
            setOutlineProvider(roundedOutline(8));
            setClipToOutline(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
            setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            // A zoomed TextureView may exceed its aspect-ratio layout. Clip it inside a
            // separate inset viewport so neither video nor its black shutter reaches the ring.
            FrameLayout viewport =
                    new FrameLayout(getContext()) {
                        private final Path clip = new Path();

                        @Override
                        protected void dispatchDraw(Canvas canvas) {
                            int save = canvas.save();
                            clip.reset();
                            clip.addRoundRect(
                                    0, 0, getWidth(), getHeight(), dp(5), dp(5), Path.Direction.CW);
                            canvas.clipPath(clip);
                            super.dispatchDraw(canvas);
                            canvas.restoreToCount(save);
                        }
                    };
            viewport.setBackgroundColor(Color.BLACK);
            viewport.setClipChildren(true);
            viewport.setClipToPadding(true);
            viewport.setOutlineProvider(roundedOutline(5));
            viewport.setClipToOutline(true);
            video =
                    (PlayerView)
                            LayoutInflater.from(getContext())
                                    .inflate(R.layout.own_camera_video, viewport, false);
            video.setResizeMode(resizeMode());
            video.setFocusable(false);
            video.setKeepContentOnPlayerReset(false);
            video.setClipChildren(true);
            video.setClipToPadding(true);
            video.setOutlineProvider(roundedOutline(5));
            video.setClipToOutline(true);
            viewport.addView(video, new FrameLayout.LayoutParams(-1, -1));
            addView(viewport, new LayoutParams(-1, 0, 1));
            status = text("Câmera " + channel + " · role para visualizar", 12);
            status.setGravity(Gravity.CENTER_VERTICAL);
            status.setSingleLine(true);
            status.setEllipsize(android.text.TextUtils.TruncateAt.END);
            status.setIncludeFontPadding(false);
            status.setPadding(dp(9), 0, dp(9), 0);
            addView(status, new LayoutParams(-1, dp(27)));
            setOnFocusChangeListener(
                    (v, f) -> {
                        invalidate();
                        if (f && expanded == 0) {
                            selected = channel;
                            heading.setText("Câmeras · " + channel + " de " + count);
                            if (toolbar != null)
                                for (int i = 0; i < toolbar.getChildCount(); i++)
                                    toolbar.getChildAt(i).setNextFocusDownId(getId());
                            requestRectangleOnScreen(
                                    new Rect(0, 0, getWidth(), getHeight()), false);
                            scheduleVisible();
                        }
                    });
            setOnClickListener(
                    v -> {
                        if (expanded == 0) showExpanded(channel, false);
                        else if (player == null || !rendered || player.getPlayerError() != null) {
                            stop();
                            start();
                        }
                    });
            setOnKeyListener(
                    (v, key, e) -> {
                        if (expanded == 0 || e.getAction() != KeyEvent.ACTION_DOWN) return false;
                        if (key == KeyEvent.KEYCODE_DPAD_LEFT && channel > 1) {
                            showExpanded(channel - 1, light);
                            return true;
                        }
                        if (key == KeyEvent.KEYCODE_DPAD_RIGHT && channel < count) {
                            showExpanded(channel + 1, light);
                            return true;
                        }
                        return false;
                    });
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            focusedRing =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height,
                            new int[] {0xFF0084FE, 0xFF13B8B0, 0xFF2CEB43},
                            null,
                            Shader.TileMode.CLAMP);
            idleRing =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height,
                            new int[] {0x99457A9A, 0x70406672, 0x99578B62},
                            null,
                            Shader.TileMode.CLAMP);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            // Draw last: the visible frame always sits above video, shutter and status.
            float stroke = hasFocus() ? dp(1.5f) : dp(1);
            float inset = stroke / 2f;
            ringBounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(stroke);
            ringPaint.setShader(hasFocus() ? focusedRing : idleRing);
            canvas.drawRoundRect(ringBounds, dp(8) - inset, dp(8) - inset, ringPaint);
        }

        void start() {
            if (player != null || stopped) return;
            rendered = false;
            dimensions = "";
            status.setText(
                    "Câmera " + channel + " · conectando" + (main ? " em alta resolução…" : "…"));
            ExoPlayer instance = new ExoPlayer.Builder(getContext()).build();
            player = instance;
            instance.setVolume(0);
            video.setPlayer(instance);
            instance.addListener(
                    new Player.Listener() {
                        @Override
                        public void onVideoSizeChanged(VideoSize size) {
                            dimensions = size.width + " × " + size.height;
                            if (rendered) live();
                            if ((getContext().getApplicationInfo().flags
                                            & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
                                    != 0)
                                android.util.Log.i(
                                        "OwnTVVideo",
                                        "channel="
                                                + channel
                                                + " main="
                                                + main
                                                + " size="
                                                + dimensions
                                                + " pixelRatio="
                                                + size.pixelWidthHeightRatio);
                        }

                        @Override
                        public void onPlaybackStateChanged(int state) {
                            if (player == instance && state == Player.STATE_BUFFERING)
                                status.setText(
                                        "Câmera "
                                                + channel
                                                + " · "
                                                + (rendered ? "reconectando…" : "carregando…"));
                            else if (player == instance && state == Player.STATE_READY && rendered)
                                live();
                        }

                        @Override
                        public void onRenderedFirstFrame() {
                            if (player == instance) {
                                rendered = true;
                                live();
                            }
                        }

                        @Override
                        public void onPlayerError(PlaybackException error) {
                            if (player == instance)
                                status.setText(
                                        "Câmera "
                                                + channel
                                                + " · indisponível · "
                                                + (expanded > 0
                                                        ? "OK reconectar ou Modo leve"
                                                        : "OK tentar fluxo principal"));
                        }
                    });
            String uri =
                    "rtsp://"
                            + Uri.encode(profile.optString("user"))
                            + ":"
                            + Uri.encode(profile.optString("password"))
                            + "@"
                            + profile.optString("host")
                            + ":"
                            + profile.optInt("port", 554)
                            + "/cam/realmonitor?channel="
                            + channel
                            + "&subtype="
                            + (main ? 0 : 1);
            instance.setMediaSource(
                    new RtspMediaSource.Factory()
                            .setForceUseRtpTcp(true)
                            .setTimeoutMs(12000)
                            .createMediaSource(MediaItem.fromUri(uri)));
            instance.prepare();
            instance.play();
            handler.postDelayed(
                    () -> {
                        if (player == instance && !rendered)
                            status.setText(
                                    "Câmera "
                                            + channel
                                            + " · sem vídeo recebido · "
                                            + (expanded > 0 ? "OK reconectar" : "OK abrir"));
                    },
                    15000);
        }

        void live() {
            status.setText(
                    "Câmera "
                            + channel
                            + " · ao vivo · "
                            + dimensions
                            + (main ? " · principal" : " · prévia"));
        }

        void stop() {
            if (player != null) {
                video.setPlayer(null);
                player.release();
                player = null;
                rendered = false;
                status.setText("Câmera " + channel + " · role para visualizar");
            }
        }
    }
}

package com.conreo.couchytv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.tv.*;
import android.net.Uri;
import android.provider.Settings;
import android.security.keystore.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import java.io.*;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

/** Native remote-friendly companion pages. Private camera state never enters launcher exports. */
public class EpActivity extends Activity {
    private LinearLayout body;
    private LinearLayout actionRow;
    private int actionCount;
    private int menuColumns = 2;
    private String newsParent = "settings";
    private String page;
    private OwnCameraPanel cameraPanel;
    private Bundle savedCameraState;
    private int gridOffset = 0;
    private boolean grid = false;
    private final int night = Color.rgb(3, 11, 18),
            blue = Color.rgb(0, 132, 254),
            green = Color.rgb(44, 235, 67);
    private JSONObject camera;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        page = getIntent().getStringExtra("page");
        if (state != null) {
            page = state.getString("ep_page", page);
            newsParent = state.getString("news_parent", "settings");
            savedCameraState = state.getBundle("camera_panel");
            imageGroup = state.getString("image_group");
            imageIndex = state.getInt("image_index", 0);
        }
        if (page == null) page = "settings";
        camera = readCamera();
        if (savedCameraState != null && configured()) showGrid(0, 0);
        else render();
    }

    private int dp(float x) {
        return (int) (x * getResources().getDisplayMetrics().density + .5f);
    }

    private String text(int id) {
        return getString(id);
    }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(244, 246, 248));
        t.setPadding(0, dp(5), 0, dp(8));
        return t;
    }

    private void begin(String title, String subtitle) {
        begin(title, subtitle, 2);
    }

    private void begin(String title, String subtitle, int columns) {
        releasePlayers();
        grid = false;
        actionRow = null;
        actionCount = 0;
        menuColumns = columns;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout background = new FrameLayout(this);
        background.setBackground(new OwnBackdrop());
        scroll.setBackgroundColor(Color.TRANSPARENT);
        background.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false);
        body.setClipToPadding(false);
        body.setPadding(dp(44), dp(25), dp(44), dp(25));
        scroll.addView(body);
        setContentView(background);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView brand = new ImageView(this);
        brand.setImageDrawable(OwnBrand.logo(this));
        brand.setContentDescription("Identidade visual");
        heading.addView(brand, new LinearLayout.LayoutParams(dp(56), dp(42)));
        TextView h = label(title, 28);
        h.setPadding(dp(12), 0, 0, 0);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.addView(h);
        body.addView(heading);
        if (!subtitle.isEmpty()) {
            TextView s = label(subtitle, 14);
            s.setTextColor(Color.rgb(170, 182, 195));
            body.addView(s);
        }
    }

    private Button button(String name, Runnable action) {
        Button b = new Button(this);
        b.setText(name);
        b.setContentDescription(name);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setMaxLines(2);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setStateListAnimator(null);
        b.setFocusable(true);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setMinHeight(0);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setIncludeFontPadding(false);
        b.setCompoundDrawables(OwnIcons.drawable(this, name), null, null, null);
        b.setCompoundDrawablePadding(dp(10));
        b.setBackground(shape(false));
        b.setOnFocusChangeListener(
                (v, f) -> {
                    v.setBackground(shape(f));
                    if (f)
                        v.post(
                                () ->
                                        v.requestRectangleOnScreen(
                                                new android.graphics.Rect(
                                                        -dp(8),
                                                        -dp(8),
                                                        v.getWidth() + dp(8),
                                                        v.getHeight() + dp(8)),
                                                false));
                });
        b.setOnClickListener(v -> action.run());
        int available = getResources().getDisplayMetrics().widthPixels - dp(88);
        int width = Math.min(available, dp(menuColumns == 3 ? 872 : 760));
        // Text fields and explanatory labels break a row. Reusing it would place later actions
        // above that content.
        if (actionRow != null && body.getChildAt(body.getChildCount() - 1) != actionRow) {
            actionRow = null;
            actionCount = 0;
        }
        if (name.equals(text(R.string.ep_back))) {
            actionRow = null;
            actionCount = 0;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(160), dp(44));
            lp.setMargins(0, dp(16), 0, 0);
            body.addView(b, lp);
        } else {
            if (actionRow == null || actionCount % menuColumns == 0) {
                actionRow = new LinearLayout(this);
                actionRow.setOrientation(LinearLayout.HORIZONTAL);
                actionRow.setClipChildren(false);
                actionRow.setClipToPadding(false);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(width, -2);
                rowParams.topMargin = dp(10);
                body.addView(actionRow, rowParams);
            }
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            (width - dp(12) * (menuColumns - 1)) / menuColumns, dp(64));
            if (actionCount % menuColumns != 0) lp.leftMargin = dp(12);
            actionRow.addView(b, lp);
            actionCount++;
        }
        return b;
    }

    private Drawable shape(boolean focus) {
        GradientDrawable fill =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[] {
                            focus ? 0xFF102A39 : 0xFF0C1C27, focus ? 0xFF0B2526 : 0xFF081720
                        });
        fill.setCornerRadius(dp(9));
        if (!focus) return fill;
        GradientDrawable edge =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[] {0xCC0084FE, 0x9900C8C8, 0xBB2CEB43});
        edge.setCornerRadius(dp(10));
        LayerDrawable result = new LayerDrawable(new Drawable[] {edge, fill});
        result.setLayerInset(1, dp(1), dp(1), dp(1), dp(1));
        return result;
    }

    private void render() {
        switch (page) {
            case "ssh":
                ssh();
                break;
            case "customize":
                customize();
                break;
            case "news":
                news();
                break;
            case "favorites":
                favoriteOrder();
                break;
            case "brand":
                brand();
                break;
            case "hdmi":
                begin("Entradas HDMI", "Escolha a entrada do console.");
                openHdmi(getSharedPreferences("own_cards", 0).getInt("hdmi", 1));
                break;
            case "game_mode":
                launch(new Intent("com.android.tv.settings.gamemode"));
                finish();
                break;
            case "inputs":
                begin("Entradas HDMI", "Escolha uma entrada da TV.");
                button(text(R.string.ep_inputs), this::inputChooser);
                button(text(R.string.ep_back), this::finish);
                inputChooser();
                break;
            case "about":
                about();
                break;
            case "datetime":
                dateTime();
                break;
            case "ps5":
                games();
                break;
            case "cameras":
                if (configured()) showGrid(0, 0);
                else cameras();
                break;
            case "youtube":
                youtube();
                break;
            case "wallpapers":
                wallpapers();
                break;
            case "original":
                openOriginal();
                finish();
                break;
            default:
                settings();
        }
    }

    private void launch(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.ep_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void openOriginal() {
        getSharedPreferences("home", 0)
                .edit()
                .putLong("allowOriginalUntil", System.currentTimeMillis() + 60000)
                .apply();
        launch(
                new Intent(Intent.ACTION_MAIN)
                        .setComponent(
                                new ComponentName(
                                        "com.google.android.apps.tv.launcherx",
                                        "com.google.android.apps.tv.launcherx.home.HomeActivity")));
    }

    private void settings() {
        page = "settings";
        begin(text(R.string.ep_settings), text(R.string.ep_settings_sub), 3);
        button(
                text(R.string.ep_android_settings),
                () -> launch(new Intent(Settings.ACTION_SETTINGS)));
        button(
                text(R.string.ep_picture),
                () -> launch(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        button(
                text(R.string.ep_game_master),
                () -> launch(new Intent("com.android.tv.settings.gamemode")));
        button(text(R.string.ep_inputs), this::inputChooser);
        button(text(R.string.ep_network), () -> launch(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button("Data, hora e fuso horário", this::dateTime);
        button("Personalizar sua central", this::customize);
        button("Notícias e temas", () -> openNews("settings"));
        button("Acesso SSH e SFTP", this::ssh);
        button(
                text(R.string.ep_wallpapers),
                () -> {
                    page = "wallpapers";
                    wallpapers();
                });
        button(text(R.string.ep_original), this::openOriginal);
        button(text(R.string.ep_about), this::about);
        button(text(R.string.ep_back), this::finish);
    }

    private void about() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "—";
        }
        page = "about";
        begin(text(R.string.ep_about), getString(R.string.ep_version, version));
        body.addView(label(text(R.string.ep_developer), 20));
        body.addView(label(text(R.string.ep_license), 14));
        button(text(R.string.ep_website), () -> openLink("https://elizandropacheco.io/"));
        button(
                "Dê uma estrela no GitHub",
                () -> openLink("https://github.com/elizandropacheco/OwnTV"));
        button(
                "Inscreva-se no YouTube",
                () -> openLink("https://www.youtube.com/@ElizandroPacheco"));
        button("Siga no Instagram", () -> openLink("https://www.instagram.com/elizandropacheco/"));
        button(text(R.string.ep_back), this::finish);
    }

    private void dateTime() {
        page = "datetime";
        String selected = getSharedPreferences("own_clock", 0).getString("timezone", "system");
        TimeZone zone =
                selected.equals("system") ? TimeZone.getDefault() : TimeZone.getTimeZone(selected);
        begin("Data e hora", "Escolha o fuso exibido no OwnTV ou ajuste a data e hora da TV.");
        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy · HH:mm", new Locale("pt", "BR"));
        format.setTimeZone(zone);
        body.addView(label(format.format(new Date()), 20));
        body.addView(
                label(
                        "Fuso do OwnTV: "
                                + zone.getID()
                                + (selected.equals("system") ? " · acompanhar a TV" : ""),
                        14));
        String server;
        boolean automatic;
        try {
            server = Settings.Global.getString(getContentResolver(), "ntp_server");
            automatic =
                    Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME, 0) == 1;
        } catch (Exception e) {
            server = null;
            automatic = false;
        }
        body.addView(
                label(
                        "Hora automática da TV: "
                                + (automatic ? "ativada" : "desativada")
                                + " · NTP: "
                                + (server == null ? "padrão do sistema" : server),
                        13));
        button("Fuso horário do OwnTV", this::chooseTimezone);
        button(
                "Data e fuso horário da TV",
                () -> launch(new Intent(Settings.ACTION_DATE_SETTINGS)));
        button(text(R.string.ep_back), this::finish);
    }

    private void chooseTimezone() {
        String[] zones = {
            "system",
            "America/Sao_Paulo",
            "America/Manaus",
            "America/Rio_Branco",
            "America/Noronha",
            "America/Los_Angeles",
            "America/New_York",
            "Europe/Lisbon",
            "UTC"
        };
        String[] names = {
            "Acompanhar o fuso da TV",
            "Brasília / São Paulo",
            "Manaus",
            "Rio Branco",
            "Fernando de Noronha",
            "Los Angeles / Las Vegas",
            "Nova York",
            "Lisboa",
            "UTC",
            "Outro fuso (IANA)"
        };
        String selected = getSharedPreferences("own_clock", 0).getString("timezone", "system");
        int checked = java.util.Arrays.asList(zones).indexOf(selected);
        new OwnDialogBuilder(this)
                .setTitle("Fuso horário do OwnTV")
                .setSingleChoiceItems(
                        names,
                        checked,
                        (d, n) -> {
                            d.dismiss();
                            if (n < zones.length) {
                                saveTimezone(zones[n]);
                                return;
                            }
                            EditText input = new EditText(this);
                            input.setSingleLine(true);
                            input.setHint("Ex.: America/Chicago");
                            input.setText(
                                    selected.equals("system")
                                            ? TimeZone.getDefault().getID()
                                            : selected);
                            AlertDialog dialog =
                                    new OwnDialogBuilder(this)
                                            .setTitle("Identificador do fuso horário")
                                            .setView(input)
                                            .setPositiveButton("Salvar", null)
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .create();
                            dialog.setOnShowListener(
                                    v ->
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                    .setOnClickListener(
                                                            b -> {
                                                                String id =
                                                                        input.getText()
                                                                                .toString()
                                                                                .trim();
                                                                if (!java.util.Arrays.asList(
                                                                                TimeZone
                                                                                        .getAvailableIDs())
                                                                        .contains(id)) {
                                                                    input.setError(
                                                                            "Escolha um identificador IANA válido, como America/Chicago");
                                                                    return;
                                                                }
                                                                dialog.dismiss();
                                                                saveTimezone(id);
                                                            }));
                            dialog.show();
                        })
                .show();
    }

    private void saveTimezone(String id) {
        getSharedPreferences("own_clock", 0).edit().putString("timezone", id).apply();
        dateTime();
    }

    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) launch(intent);
        else
            new OwnDialogBuilder(this)
                    .setMessage(url)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
    }

    private void games() {
        page = "ps5";
        begin(text(R.string.ep_games), "Console, jogos e atalhos escolhidos por você.");
        JSONArray cards = OwnCards.read(this, "games");
        for (int n = 0; n < cards.length(); n++) {
            JSONObject item = cards.optJSONObject(n);
            if (item == null) continue;
            imageButton(item);
        }
        button(
                "HDMI do PS5 · entrada " + getSharedPreferences("own_cards", 0).getInt("hdmi", 1),
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Entrada do PS5")
                                .setItems(
                                        new String[] {"HDMI 1", "HDMI 2", "HDMI 3", "HDMI 4"},
                                        (d, n) -> {
                                            getSharedPreferences("own_cards", 0)
                                                    .edit()
                                                    .putInt("hdmi", n + 1)
                                                    .apply();
                                            games();
                                        })
                                .show());
        button("Personalizar menu de jogos", () -> editCards("games"));
        button(
                text(R.string.ep_120_title),
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle(R.string.ep_120_title)
                                .setMessage(R.string.ep_120_guide)
                                .setPositiveButton(android.R.string.ok, null)
                                .show());
        button(text(R.string.ep_back), this::finish);
    }

    private void ssh() {
        page = "ssh";
        android.content.SharedPreferences prefs = getSharedPreferences("own_ssh", 0);
        begin("Acesso pela rede · SSH e SFTP", OwnSshService.status(this));
        body.addView(
                label(
                        "Terminal e transferência de arquivos com as permissões do OwnTV. Usuário e senha ficam nesta TV.",
                        14));
        String address = "IP da TV";
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.util.Enumeration<java.net.InetAddress> addresses =
                        interfaces.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress a = addresses.nextElement();
                    if (a instanceof java.net.Inet4Address && !a.isLoopbackAddress())
                        address = a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        body.addView(
                label(
                        "ssh -p "
                                + prefs.getInt("port", 2222)
                                + " "
                                + prefs.getString("user", "seu_usuario")
                                + "@"
                                + address,
                        17));
        EditText user =
                field(body, "Usuário", prefs.getString("user", ""), InputType.TYPE_CLASS_TEXT);
        EditText password =
                field(
                        body,
                        "Nova senha · deixe vazio para manter",
                        "",
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText port =
                field(
                        body,
                        "Porta",
                        String.valueOf(prefs.getInt("port", 2222)),
                        InputType.TYPE_CLASS_NUMBER);
        button(
                "Salvar e iniciar SSH",
                () -> {
                    try {
                        String username = user.getText().toString().trim(),
                                pass = password.getText().toString();
                        int number = Integer.parseInt(port.getText().toString());
                        if (!username.matches("[A-Za-z0-9._-]{1,32}")
                                || number < 1024
                                || number > 65535) throw new IllegalArgumentException();
                        android.content.SharedPreferences.Editor editor =
                                prefs.edit().putString("user", username).putInt("port", number);
                        if (!pass.isEmpty()) {
                            if (pass.length() < 10 || pass.length() > 1024) {
                                password.setError("Use entre 10 e 1024 caracteres");
                                return;
                            }
                            byte[] salt = new byte[24];
                            new java.security.SecureRandom().nextBytes(salt);
                            editor.putString("salt", android.util.Base64.encodeToString(salt, 2))
                                    .putString("hash", OwnSshService.hash(pass, salt));
                        } else if (!prefs.contains("hash")) {
                            password.setError("Defina a senha inicial");
                            return;
                        }
                        editor.putBoolean("enabled", true).commit();
                        OwnSshService.start(this);
                        password.setText("");
                        new Handler(Looper.getMainLooper()).postDelayed(this::ssh, 700);
                    } catch (Exception e) {
                        Toast.makeText(
                                        this,
                                        "Revise usuário e porta (1024–65535)",
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });
        button(
                "Parar servidor SSH",
                () -> {
                    prefs.edit().putBoolean("enabled", false).apply();
                    stopService(new Intent(this, OwnSshService.class));
                    ssh();
                });
        button("Atualizar status", this::ssh);
        button(text(R.string.ep_back), this::finish);
    }

    private void imageButton(JSONObject item) {
        Button b =
                button(
                        item.optString("title"),
                        () -> Actions.INSTANCE.launchApp(this, item.optString("action")));
        b.getLayoutParams().height = dp(112);
        String art = item.optString("art");
        try {
            android.graphics.Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= 28) {
                android.graphics.ImageDecoder.Source source =
                        art.equals("fortnite")
                                ? android.graphics.ImageDecoder.createSource(
                                        getAssets(), "ep/cards/fortnite-landscape.jpg")
                                : art.equals("camera")
                                        ? android.graphics.ImageDecoder.createSource(
                                                getAssets(), "ep/cards/cameras.png")
                                        : art.startsWith("content:")
                                                ? android.graphics.ImageDecoder.createSource(
                                                        getContentResolver(), Uri.parse(art))
                                                : null;
                if (source == null) {
                    bitmap =
                            android.graphics.Bitmap.createBitmap(
                                    720, 400, android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    canvas.drawColor(night);
                    canvas.save();
                    canvas.translate(410, 60);
                    OwnIcons.draw(canvas, art, 270, 270);
                    canvas.restore();
                } else
                    bitmap =
                            android.graphics.ImageDecoder.decodeBitmap(
                                    source,
                                    (decoder, info, imageSource) -> {
                                        double ratio =
                                                Math.min(1.0, 900.0 / info.getSize().getWidth());
                                        decoder.setTargetSize(
                                                (int) (info.getSize().getWidth() * ratio),
                                                (int) (info.getSize().getHeight() * ratio));
                                    });
            } else {
                try (InputStream in =
                        art.startsWith("content:")
                                ? getContentResolver().openInputStream(Uri.parse(art))
                                : getAssets()
                                        .open(
                                                art.equals("camera")
                                                        ? "ep/cards/cameras.png"
                                                        : "ep/cards/fortnite-landscape.jpg")) {
                    android.graphics.BitmapFactory.Options options =
                            new android.graphics.BitmapFactory.Options();
                    options.inSampleSize = 2;
                    bitmap = android.graphics.BitmapFactory.decodeStream(in, null, options);
                }
                if (bitmap == null) return;
            }
            Drawable photo =
                    new OwnPhotoDrawable(
                            bitmap,
                            (float) item.optDouble("opacity", .7),
                            item.optString("alignment", "right"));
            GradientDrawable shade =
                    new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {0xEE030B12, 0x88030B12});
            shade.setCornerRadius(dp(9));
            Drawable surface = new LayerDrawable(new Drawable[] {shape(false), photo, shade});
            b.setBackground(surface);
            b.setOutlineProvider(
                    new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View v, android.graphics.Outline o) {
                            o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(9));
                        }
                    });
            b.setClipToOutline(true);
            b.setOnFocusChangeListener(
                    (v, f) -> {
                        GradientDrawable edge =
                                new GradientDrawable(
                                        GradientDrawable.Orientation.TL_BR,
                                        new int[] {0x990084FE, 0x882CEB43});
                        edge.setCornerRadius(dp(9));
                        Drawable background = surface;
                        if (f) {
                            LayerDrawable layers =
                                    new LayerDrawable(new Drawable[] {edge, surface});
                            layers.setLayerInset(1, dp(1), dp(1), dp(1), dp(1));
                            background = layers;
                        }
                        v.setBackground(background);
                        if (f)
                            v.post(
                                    () ->
                                            v.requestRectangleOnScreen(
                                                    new android.graphics.Rect(
                                                            -dp(8),
                                                            -dp(8),
                                                            v.getWidth() + dp(8),
                                                            v.getHeight() + dp(8)),
                                                    false));
                    });
        } catch (Exception ignored) {
        }
    }

    private void customize() {
        page = "customize";
        begin(
                "Personalizar sua central",
                "Imagens, favoritos, notícias e atalhos salvos nesta TV.",
                3);
        button("Cards principais da Home", () -> editCards("hero"));
        button("Menu de jogos", () -> editCards("games"));
        button("Imagens dos aplicativos", this::appArtwork);
        button("Wallpapers e downloads", this::wallpapers);
        button(
                "Recomendações · "
                        + (getSharedPreferences("own_cards", 0).getBoolean("recommendations", true)
                                ? "visíveis"
                                : "ocultas"),
                () -> {
                    android.content.SharedPreferences p = getSharedPreferences("own_cards", 0);
                    p.edit()
                            .putBoolean("recommendations", !p.getBoolean("recommendations", true))
                            .apply();
                    customize();
                });
        button("Ordem dos favoritos", this::favoriteOrder);
        button(
                "Ícones dos favoritos · "
                        + (getSharedPreferences("own_cards", 0)
                                        .getString("favoriteIconStyle", "brand")
                                        .equals("mono")
                                ? "monocromáticos"
                                : "coloridos"),
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Ícones dos favoritos")
                                .setItems(
                                        new String[] {
                                            "Monocromáticos · branco e cinza",
                                            "Coloridos · uma cor por marca"
                                        },
                                        (d, n) -> {
                                            getSharedPreferences("own_cards", 0)
                                                    .edit()
                                                    .putString(
                                                            "favoriteIconStyle",
                                                            n == 0 ? "mono" : "brand")
                                                    .apply();
                                            customize();
                                        })
                                .show());
        button("Identidade visual e logo", this::brand);
        button("Notícias e temas", () -> openNews("customize"));
        button(text(R.string.ep_back), this::finish);
    }

    private void openNews(String parent) {
        newsParent = parent;
        news();
    }

    private void brand() {
        page = "brand";
        begin(
                "Identidade visual e logo",
                "Escolha o logo da Home, dos menus e da abertura do OwnTV.");
        String current = OwnBrand.mode(this);
        body.addView(
                label(
                        "Atual: "
                                + (current.equals("owntv")
                                        ? "Monograma OwnTV"
                                        : current.equals("custom")
                                                ? "Imagem personalizada"
                                                : "Elizandro Pacheco · EP"),
                        16));
        button(
                "Elizandro Pacheco · EP",
                () -> {
                    getSharedPreferences("own_brand", 0).edit().putString("mode", "ep").apply();
                    brand();
                });
        button(
                "Monograma OwnTV",
                () -> {
                    getSharedPreferences("own_brand", 0).edit().putString("mode", "owntv").apply();
                    brand();
                });
        button(
                "Imagem personalizada · local / USB",
                () -> {
                    imageGroup = "brand";
                    imageIndex = 0;
                    try {
                        startActivityForResult(
                                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                        .setType("image/*")
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .addFlags(
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        | Intent
                                                                .FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                                8103);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(
                                        this,
                                        "Instale um gerenciador de arquivos compatível",
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });
        button("Pré-visualizar abertura", () -> OwnBrand.preview(this));
        body.addView(
                label(
                        "A abertura animada aparece ao iniciar o aplicativo do zero. A animação não se repete ao voltar pelo botão Home. Para imagem personalizada, use PNG, JPEG ou WebP; PNG transparente é ideal para um logo.",
                        14));
        button(text(R.string.ep_back), this::customize);
    }

    private void news() {
        page = "news";
        android.content.SharedPreferences prefs = getSharedPreferences("own_news", 0);
        begin("Notícias e temas", "Escolha os assuntos e a velocidade das manchetes na Home.");
        boolean enabled = prefs.getBoolean("enabled", true);
        button(
                "Notícias · " + (enabled ? "ativadas" : "desativadas"),
                () -> {
                    prefs.edit().putBoolean("enabled", !enabled).apply();
                    news();
                });
        button(
                "Trocar manchete · " + prefs.getInt("intervalSeconds", 12) + " segundos",
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Tempo de cada manchete")
                                .setSingleChoiceItems(
                                        new String[] {
                                            "8 segundos",
                                            "12 segundos",
                                            "20 segundos",
                                            "30 segundos"
                                        },
                                        java.util.Arrays.asList(8, 12, 20, 30)
                                                .indexOf(prefs.getInt("intervalSeconds", 12)),
                                        (d, n) -> {
                                            prefs.edit()
                                                    .putInt(
                                                            "intervalSeconds",
                                                            new int[] {8, 12, 20, 30}[n])
                                                    .apply();
                                            d.dismiss();
                                            news();
                                        })
                                .show());
        EditText topics =
                field(
                        body,
                        "Temas separados por vírgulas · até 8",
                        prefs.getString("topics", "telecom, tecnologia, inteligência artificial"),
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        topics.setSingleLine(false);
        topics.setMinLines(2);
        topics.setMaxLines(3);
        topics.getLayoutParams().height = dp(76);
        button(
                "Salvar temas",
                () -> {
                    String[] entries = topics.getText().toString().split(",", -1);
                    if (entries.length > 8) {
                        topics.setError("Use no máximo 8 temas");
                        topics.requestFocus();
                        return;
                    }
                    ArrayList<String> clean = new ArrayList<>();
                    for (String entry : entries) {
                        String value = entry.trim();
                        if (value.isEmpty()) {
                            topics.setError("Preencha cada tema e remova vírgulas extras");
                            topics.requestFocus();
                            return;
                        }
                        clean.add(value);
                    }
                    prefs.edit()
                            .putString("topics", android.text.TextUtils.join(", ", clean))
                            .apply();
                    Toast.makeText(this, "Temas salvos nesta TV", Toast.LENGTH_SHORT).show();
                    news();
                });
        button(
                text(R.string.ep_back),
                () -> {
                    if (newsParent.equals("customize")) customize();
                    else settings();
                });
    }

    private ArrayList<String> packageList(String json) {
        ArrayList<String> result = new ArrayList<>();
        try {
            JSONArray entries = new JSONArray(json);
            for (int n = 0; n < entries.length(); n++) {
                String pkg = entries.optString(n).trim();
                if (!pkg.isEmpty() && !result.contains(pkg)) result.add(pkg);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private String appName(String pkg) {
        switch (pkg) {
            case "ep:ps5":
                return "PS5 · Fortnite";
            case "ep:cameras":
                return "Câmeras";
            case "ep:youtube":
                return "YouTube · inscrições";
            case "ep:settings":
                return "Configurações da TV";
            case "ep:wallpapers":
                return "Fundos EP";
            case "ep:original":
                return "Google TV original";
            case "ep:datetime":
                return "Data e hora";
            case "ep:about":
                return "Sobre o OwnTV";
            case "ep:customize":
                return "Personalizar";
            case "ep:news":
                return "Notícias e temas";
        }
        try {
            return getPackageManager()
                    .getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0))
                    .toString();
        } catch (Exception ignored) {
            return pkg;
        }
    }

    private void appIcon(Button button, String pkg) {
        try {
            Drawable icon = getPackageManager().getApplicationIcon(pkg);
            icon.setBounds(0, 0, dp(24), dp(24));
            button.setCompoundDrawables(icon, null, null, null);
        } catch (Exception ignored) {
        }
    }

    private void favoriteOrder() {
        page = "favorites";
        begin("Ordem dos favoritos", "Selecione um aplicativo para escolher sua posição na Home.");
        android.content.SharedPreferences prefs = getSharedPreferences("own_cards", 0);
        ArrayList<String> visible = packageList(prefs.getString("visibleFavorites", "[]"));
        ArrayList<String> ordered = new ArrayList<>();
        for (String pkg : packageList(prefs.getString("favoriteOrder", "[]")))
            if (visible.contains(pkg)) ordered.add(pkg);
        for (String pkg : visible) if (!ordered.contains(pkg)) ordered.add(pkg);
        if (ordered.isEmpty())
            body.addView(
                    label(
                            "Adicione favoritos em Personalizar apps na Home. Eles aparecerão aqui para ordenar.",
                            16));
        for (int n = 0; n < ordered.size(); n++) {
            final int index = n;
            String pkg = ordered.get(n);
            Button favorite =
                    button(
                            (n + 1) + " · " + appName(pkg),
                            () ->
                                    new OwnDialogBuilder(this)
                                            .setTitle(appName(pkg))
                                            .setItems(
                                                    new String[] {
                                                        "Mover antes de…",
                                                        "Mover depois de…",
                                                        "Mover para o início",
                                                        "Mover para o fim"
                                                    },
                                                    (d, choice) -> {
                                                        if (choice >= 2) {
                                                            moveFavorite(
                                                                    ordered,
                                                                    index,
                                                                    choice == 2
                                                                            ? 0
                                                                            : ordered.size() - 1);
                                                            return;
                                                        }
                                                        ArrayList<String> targets =
                                                                new ArrayList<>(ordered);
                                                        targets.remove(pkg);
                                                        if (targets.isEmpty()) {
                                                            Toast.makeText(
                                                                            this,
                                                                            "Este é o único favorito",
                                                                            Toast.LENGTH_SHORT)
                                                                    .show();
                                                            return;
                                                        }
                                                        String[] names = new String[targets.size()];
                                                        for (int i = 0; i < targets.size(); i++)
                                                            names[i] = appName(targets.get(i));
                                                        new OwnDialogBuilder(this)
                                                                .setTitle(
                                                                        choice == 0
                                                                                ? "Mover antes de"
                                                                                : "Mover depois de")
                                                                .setItems(
                                                                        names,
                                                                        (dialog, target) -> {
                                                                            int destination =
                                                                                    targets.indexOf(
                                                                                                    targets
                                                                                                            .get(
                                                                                                                    target))
                                                                                            + (choice
                                                                                                            == 1
                                                                                                    ? 1
                                                                                                    : 0);
                                                                            moveFavorite(
                                                                                    ordered,
                                                                                    index,
                                                                                    destination);
                                                                        })
                                                                .show();
                                                    })
                                            .show());
            appIcon(favorite, pkg);
        }
        button(text(R.string.ep_back), this::customize);
    }

    private void moveFavorite(ArrayList<String> ordered, int from, int to) {
        ArrayList<String> result = new ArrayList<>(ordered);
        String selected = result.remove(from);
        result.add(Math.max(0, Math.min(result.size(), to)), selected);
        android.content.SharedPreferences prefs = getSharedPreferences("own_cards", 0);
        // Preserve saved packages that are temporarily hidden or unavailable as well as every
        // visible favorite.
        for (String pkg : packageList(prefs.getString("favoriteOrder", "[]")))
            if (!result.contains(pkg)) result.add(pkg);
        for (String pkg : packageList(prefs.getString("visibleFavorites", "[]")))
            if (!result.contains(pkg)) result.add(pkg);
        prefs.edit().putString("favoriteOrder", new JSONArray(result).toString()).apply();
        favoriteOrder();
    }

    private void appArtwork() {
        begin(
                "Imagens dos aplicativos",
                "Substitua a imagem de qualquer app por um arquivo local ou USB.");
        java.util.LinkedHashMap<String, String> choices = new java.util.LinkedHashMap<>();
        for (android.content.pm.ResolveInfo info :
                getPackageManager()
                        .queryIntentActivities(
                                new Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
                                0))
            if (!info.activityInfo.packageName.equals(getPackageName()))
                choices.put(
                        info.activityInfo.packageName,
                        info.loadLabel(getPackageManager()).toString());
        for (java.util.Map.Entry<String, String> entry : choices.entrySet()) {
            Button app =
                    button(
                            entry.getValue(),
                            () -> {
                                new OwnDialogBuilder(this)
                                        .setTitle(entry.getValue())
                                        .setItems(
                                                new String[] {
                                                    "Escolher imagem local / USB",
                                                    "Restaurar imagem padrão"
                                                },
                                                (dialog, n) -> {
                                                    if (n == 1) {
                                                        getSharedPreferences("own_cards", 0)
                                                                .edit()
                                                                .remove("art:" + entry.getKey())
                                                                .apply();
                                                        appArtwork();
                                                        return;
                                                    }
                                                    imageGroup = "app:" + entry.getKey();
                                                    imageIndex = 0;
                                                    try {
                                                        startActivityForResult(
                                                                new Intent(
                                                                                Intent
                                                                                        .ACTION_OPEN_DOCUMENT)
                                                                        .setType("image/*")
                                                                        .addCategory(
                                                                                Intent
                                                                                        .CATEGORY_OPENABLE)
                                                                        .addFlags(
                                                                                Intent
                                                                                                .FLAG_GRANT_READ_URI_PERMISSION
                                                                                        | Intent
                                                                                                .FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                                                                8103);
                                                    } catch (ActivityNotFoundException e) {
                                                        Toast.makeText(
                                                                        this,
                                                                        "Instale um gerenciador de arquivos compatível",
                                                                        Toast.LENGTH_LONG)
                                                                .show();
                                                    }
                                                })
                                        .show();
                            });
            appIcon(app, entry.getKey());
        }
        button(text(R.string.ep_back), this::customize);
    }

    private void editCards(String group) {
        begin(
                group.equals("hero") ? "Cards da Home" : "Menu de jogos",
                "Selecione um card para editar, mover ou remover.");
        JSONArray cards = OwnCards.read(this, group);
        for (int n = 0; n < cards.length(); n++) {
            final int index = n;
            JSONObject item = cards.optJSONObject(n);
            if (item == null) continue;
            button(
                    (n + 1) + " · " + item.optString("title"),
                    () ->
                            new OwnDialogBuilder(this)
                                    .setTitle(item.optString("title"))
                                    .setItems(
                                            new String[] {
                                                "Editar título, descrição e ação",
                                                "Escolher imagem",
                                                "Transparência da imagem",
                                                "Mover para a esquerda / cima",
                                                "Mover para a direita / baixo",
                                                "Remover card",
                                                "Alinhar imagem de fundo"
                                            },
                                            (d, choice) -> {
                                                try {
                                                    if (choice == 0) {
                                                        editCard(group, index);
                                                        return;
                                                    }
                                                    if (choice == 1) {
                                                        chooseCardArt(group, index);
                                                        return;
                                                    }
                                                    if (choice == 2) {
                                                        new OwnDialogBuilder(this)
                                                                .setTitle("Intensidade da imagem")
                                                                .setItems(
                                                                        new String[] {
                                                                            "25% · discreta",
                                                                            "50%",
                                                                            "75%",
                                                                            "100%"
                                                                        },
                                                                        (x, v) -> {
                                                                            try {
                                                                                item.put(
                                                                                        "opacity",
                                                                                        (v + 1)
                                                                                                * .25);
                                                                                OwnCards.save(
                                                                                        this, group,
                                                                                        cards);
                                                                            } catch (
                                                                                    Exception
                                                                                            ignored) {
                                                                            }
                                                                            editCards(group);
                                                                        })
                                                                .show();
                                                        return;
                                                    }
                                                    if (choice == 6) {
                                                        new OwnDialogBuilder(this)
                                                                .setTitle("Enquadramento da imagem")
                                                                .setItems(
                                                                        new String[] {
                                                                            "Esquerda",
                                                                            "Centro",
                                                                            "Direita"
                                                                        },
                                                                        (x, alignChoice) -> {
                                                                            try {
                                                                                item.put(
                                                                                        "alignment",
                                                                                        new String
                                                                                                        [] {
                                                                                                    "left",
                                                                                                    "center",
                                                                                                    "right"
                                                                                                }
                                                                                                [
                                                                                                alignChoice]);
                                                                                OwnCards.save(
                                                                                        this, group,
                                                                                        cards);
                                                                            } catch (
                                                                                    Exception
                                                                                            ignored) {
                                                                            }
                                                                            editCards(group);
                                                                        })
                                                                .show();
                                                        return;
                                                    }
                                                    if (choice == 5) cards.remove(index);
                                                    else {
                                                        int to =
                                                                Math.max(
                                                                        0,
                                                                        Math.min(
                                                                                cards.length() - 1,
                                                                                index
                                                                                        + (choice
                                                                                                        == 3
                                                                                                ? -1
                                                                                                : 1)));
                                                        JSONObject other = cards.getJSONObject(to);
                                                        cards.put(to, item);
                                                        cards.put(index, other);
                                                    }
                                                    OwnCards.save(this, group, cards);
                                                    editCards(group);
                                                } catch (Exception ignored) {
                                                }
                                            })
                                    .show());
        }
        button(
                "Adicionar card",
                () -> {
                    try {
                        cards.put(
                                new JSONObject()
                                        .put("title", "Novo atalho")
                                        .put("subtitle", "")
                                        .put("action", "ep:inputs")
                                        .put("art", "game")
                                        .put("opacity", .7));
                        OwnCards.save(this, group, cards);
                        editCard(group, cards.length() - 1);
                    } catch (Exception ignored) {
                    }
                });
        button(text(R.string.ep_back), this::customize);
    }

    private void editCard(String group, int index) {
        JSONArray cards = OwnCards.read(this, group);
        JSONObject item = cards.optJSONObject(index);
        if (item == null) return;
        begin("Editar card", "Escolha um aplicativo ou atalho; as mudanças ficam salvas nesta TV.");
        EditText title = field(body, "Título", item.optString("title"), InputType.TYPE_CLASS_TEXT);
        EditText subtitle =
                field(body, "Descrição", item.optString("subtitle"), InputType.TYPE_CLASS_TEXT);
        EditText action =
                field(
                        body,
                        "Aplicativo ou ação",
                        item.optString("action"),
                        InputType.TYPE_CLASS_TEXT);
        action.setFocusable(false);
        button(
                "Escolher aplicativo ou atalho",
                () -> {
                    java.util.LinkedHashMap<String, String> choices =
                            new java.util.LinkedHashMap<>();
                    choices.put("ep:ps5", "PS5 · menu de jogos");
                    choices.put("ep:hdmi", "Abrir HDMI do PS5");
                    choices.put("ep:cameras", "Câmeras");
                    choices.put("ep:youtube", "YouTube · inscrições");
                    choices.put("ep:inputs", "Entradas HDMI");
                    choices.put("ep:settings", "Configurações da TV");
                    choices.put("ep:wallpapers", "Wallpapers");
                    choices.put("ep:game_mode", "Game Master");
                    for (android.content.pm.ResolveInfo info :
                            getPackageManager()
                                    .queryIntentActivities(
                                            new Intent(Intent.ACTION_MAIN)
                                                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
                                            0))
                        if (!info.activityInfo.packageName.equals(getPackageName()))
                            choices.put(
                                    info.activityInfo.packageName,
                                    info.loadLabel(getPackageManager()).toString());
                    String[] ids = choices.keySet().toArray(new String[0]),
                            names = choices.values().toArray(new String[0]);
                    new OwnDialogBuilder(this)
                            .setTitle("Ação do card")
                            .setItems(names, (d, n) -> action.setText(ids[n]))
                            .show();
                });
        button(
                "Salvar card",
                () -> {
                    try {
                        item.put("title", title.getText().toString().trim())
                                .put("subtitle", subtitle.getText().toString().trim())
                                .put("action", action.getText().toString());
                        OwnCards.save(this, group, cards);
                        editCards(group);
                    } catch (Exception ignored) {
                    }
                });
        button(text(R.string.ep_back), () -> editCards(group));
    }

    private String imageGroup;
    private int imageIndex;

    private void chooseCardArt(String group, int index) {
        String[] art = {"fortnite", "camera", "youtube", "game"};
        new OwnDialogBuilder(this)
                .setTitle("Imagem do card")
                .setItems(
                        new String[] {
                            "Fortnite · arte oficial",
                            "Câmera · ilustração EP",
                            "YouTube · ícone",
                            "Controle · ícone",
                            "Escolher arquivo local / USB"
                        },
                        (d, n) -> {
                            if (n == 4) {
                                imageGroup = group;
                                imageIndex = index;
                                try {
                                    startActivityForResult(
                                            new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                    .setType("image/*")
                                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                                    .addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                    | Intent
                                                                            .FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                                            8103);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(
                                                    this,
                                                    "Instale um gerenciador de arquivos compatível com o seletor do Android",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                                return;
                            }
                            saveCardArt(group, index, art[n]);
                        })
                .show();
    }

    private void saveCardArt(String group, int index, String art) {
        if (group.equals("brand")) {
            android.graphics.BitmapFactory.Options bounds =
                    new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream input = getContentResolver().openInputStream(Uri.parse(art))) {
                android.graphics.BitmapFactory.decodeStream(input, null, bounds);
            } catch (Exception ignored) {
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Toast.makeText(this, "Use uma imagem PNG, JPEG ou WebP válida", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            getSharedPreferences("own_brand", 0)
                    .edit()
                    .putString("mode", "custom")
                    .putString("custom", art)
                    .apply();
            brand();
            return;
        }
        if (group.startsWith("app:")) {
            getSharedPreferences("own_cards", 0)
                    .edit()
                    .putString("art:" + group.substring(4), art)
                    .apply();
            appArtwork();
            return;
        }
        try {
            JSONArray cards = OwnCards.read(this, group);
            cards.getJSONObject(index).put("art", art);
            OwnCards.save(this, group, cards);
        } catch (Exception ignored) {
        }
        editCards(group);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 8103
                && result == RESULT_OK
                && data != null
                && data.getData() != null
                && imageGroup != null) {
            Uri uri = data.getData();
            try {
                getContentResolver()
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                saveCardArt(imageGroup, imageIndex, uri.toString());
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível abrir a imagem", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openHdmi(int port) {
        try {
            TvInputManager m = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            for (TvInputInfo info : m.getTvInputList()) {
                String label =
                        info.loadLabel(this).toString().replace(" ", "").toUpperCase(Locale.ROOT);
                if (info.isPassthroughInput()
                        && (label.equals("HDMI" + port)
                                || info.getId().endsWith("/HW" + (14 + port)))) {
                    launch(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    TvContract.buildChannelUriForPassthroughInput(info.getId())));
                    if (page.equals("hdmi")) finish();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        inputChooser();
    }

    private void inputChooser() {
        try {
            List<TvInputInfo> inputs =
                    ((TvInputManager) getSystemService(TV_INPUT_SERVICE)).getTvInputList();
            ArrayList<TvInputInfo> list = new ArrayList<>();
            for (TvInputInfo i : inputs) if (i.isPassthroughInput()) list.add(i);
            String[] labels = new String[list.size()];
            for (int i = 0; i < labels.length; i++)
                labels[i] = list.get(i).loadLabel(this).toString();
            new OwnDialogBuilder(this)
                    .setTitle(R.string.ep_inputs)
                    .setItems(
                            labels,
                            (d, n) -> {
                                launch(
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                TvContract.buildChannelUriForPassthroughInput(
                                                        list.get(n).getId())));
                                if (page.equals("hdmi") || page.equals("inputs")) finish();
                            })
                    .setOnCancelListener(
                            d -> {
                                if (page.equals("hdmi") || page.equals("inputs")) finish();
                            })
                    .show();
        } catch (Exception e) {
            launch(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void youtube() {
        page = "youtube";
        begin(text(R.string.ep_youtube), text(R.string.ep_youtube_sub));
        button(
                text(R.string.ep_subscriptions),
                () -> youtubeLink("https://www.youtube.com/feed/subscriptions"));
        button(text(R.string.ep_youtube_home), () -> openApp("com.google.android.youtube.tv"));
        button(text(R.string.ep_back), this::finish);
    }

    private void youtubeLink(String url) {
        Intent i =
                new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .setPackage("com.google.android.youtube.tv");
        if (i.resolveActivity(getPackageManager()) != null) launch(i);
        else openApp("com.google.android.youtube.tv");
    }

    private void openApp(String pkg) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) launch(i);
        else
            new OwnDialogBuilder(this)
                    .setMessage(R.string.ep_app_missing)
                    .setPositiveButton(
                            R.string.ep_store,
                            (d, w) ->
                                    launch(
                                            new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id=" + pkg))))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
    }

    private void wallpapers() {
        page = "wallpapers";
        begin(text(R.string.ep_wallpapers), text(R.string.ep_wallpapers_sub));
        button(
                "Paisagens dos EUA · alternância automática",
                () -> {
                    getSharedPreferences("own_appearance", 0)
                            .edit()
                            .putString("mode", "slideshow")
                            .apply();
                    finish();
                });
        button(
                "Intervalo de alternância",
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Trocar wallpaper a cada")
                                .setItems(
                                        new String[] {
                                            "15 segundos",
                                            "30 segundos",
                                            "1 minuto",
                                            "3 minutos",
                                            "5 minutos",
                                            "10 minutos"
                                        },
                                        (d, n) -> {
                                            int[] seconds = {15, 30, 60, 180, 300, 600};
                                            getSharedPreferences("own_appearance", 0)
                                                    .edit()
                                                    .putInt("intervalSeconds", seconds[n])
                                                    .apply();
                                        })
                                .show());
        android.content.SharedPreferences prefs = getSharedPreferences("own_appearance", 0);
        body.addView(
                label(
                        OwnWallpapers.available(this).size()
                                + " fotografias locais · originais acima de 4K",
                        16));
        button(
                "Downloads diários · "
                        + (prefs.getBoolean("dailyDownloads", true) ? "ativados" : "pausados"),
                () -> {
                    prefs.edit()
                            .putBoolean("dailyDownloads", !prefs.getBoolean("dailyDownloads", true))
                            .apply();
                    OwnWallpapers.schedule(this);
                    wallpapers();
                });
        button(
                "Meta da coleção · " + prefs.getInt("downloadTarget", 100) + " fotos",
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Guardar até")
                                .setItems(
                                        new String[] {"25 fotos", "50 fotos", "100 fotos"},
                                        (d, n) -> {
                                            prefs.edit()
                                                    .putInt(
                                                            "downloadTarget",
                                                            new int[] {25, 50, 100}[n])
                                                    .apply();
                                            wallpapers();
                                        })
                                .show());
        button(
                "Fotos por dia · " + prefs.getInt("downloadsPerDay", 3),
                () ->
                        new OwnDialogBuilder(this)
                                .setTitle("Novos originais por dia")
                                .setItems(
                                        new String[] {"1 foto", "3 fotos", "5 fotos"},
                                        (d, n) -> {
                                            prefs.edit()
                                                    .putInt(
                                                            "downloadsPerDay",
                                                            new int[] {1, 3, 5}[n])
                                                    .apply();
                                            wallpapers();
                                        })
                                .show());
        body.addView(
                label(
                        prefs.getString(
                                "downloadStatus",
                                "Downloads com internet disponível; as paisagens continuam funcionando offline."),
                        13));
        JSONArray catalog = OwnWallpapers.catalog(this);
        java.util.HashSet<String> local = new java.util.HashSet<>(OwnWallpapers.available(this));
        for (int n = 0; n < catalog.length(); n++) {
            JSONObject photo = catalog.optJSONObject(n);
            if (photo == null) continue;
            String id = photo.optString("id");
            String asset = "asset:" + id + ".jpg";
            String disk =
                    new File(new File(getFilesDir(), "wallpapers"), id + ".jpg").getAbsolutePath();
            final String selected = local.contains(asset) ? asset : disk;
            if (!local.contains(selected)) continue;
            String title = photo.optString("label", photo.optString("title"));
            if (title.startsWith("File:")) title = title.substring(5);
            if (title.length() > 55) title = title.substring(0, 52) + "…";
            button(
                    title,
                    () -> {
                        prefs.edit()
                                .putString("mode", "collection")
                                .putString("selected", selected)
                                .apply();
                        finish();
                    });
        }
        button(
                "Créditos das fotografias",
                () -> {
                    try (InputStream in = getAssets().open("ep/wallpapers/CREDITS.txt")) {
                        String credits =
                                new String(OwnIO.read(in), java.nio.charset.StandardCharsets.UTF_8);
                        new OwnDialogBuilder(this)
                                .setTitle("Fotografias e licenças")
                                .setMessage(credits)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.ep_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });
        button(
                "Linhas EP · vetorial",
                () -> {
                    getSharedPreferences("own_appearance", 0)
                            .edit()
                            .putBoolean("photo", false)
                            .putString("mode", "vector")
                            .apply();
                    finish();
                });
        button(text(R.string.ep_back), this::finish);
    }

    private void cameras() {
        page = "cameras";
        begin(text(R.string.ep_cameras), text(R.string.ep_cameras_sub));
        button(
                text(R.string.ep_grid),
                () -> {
                    if (configured()) showGrid(gridOffset, 0);
                    else editCamera();
                });
        button(text(R.string.ep_camera_setup), this::editCamera);
        button(text(R.string.ep_mibo), () -> openApp("br.com.intelbras.mibocam"));
        button(text(R.string.ep_isic), () -> openApp("com.intelbras.isiclite"));
        button(text(R.string.ep_icsee), () -> openApp("com.xm.csee"));
        button(text(R.string.ep_back), this::finish);
    }

    private boolean configured() {
        return !camera.optString("host").isEmpty() && !camera.optString("password").isEmpty();
    }

    private EditText field(LinearLayout parent, String name, String value, int type) {
        TextView title = label(name, 14);
        parent.addView(title);
        EditText t = new EditText(this);
        t.setText(value);
        t.setSingleLine(true);
        t.setTextColor(Color.WHITE);
        t.setTextSize(17);
        t.setInputType(type);
        t.setSelectAllOnFocus(true);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        t.setBackground(shape(false));
        t.setOnFocusChangeListener((v, f) -> v.setBackground(shape(f)));
        parent.addView(
                t,
                new LinearLayout.LayoutParams(
                        Math.min(dp(520), getResources().getDisplayMetrics().widthPixels - dp(88)),
                        dp(48)));
        return t;
    }

    private void editCamera() {
        begin(text(R.string.ep_camera_setup), text(R.string.ep_camera_setup_sub));
        EditText host =
                field(
                        body,
                        text(R.string.ep_host),
                        camera.optString("host"),
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText user =
                field(
                        body,
                        text(R.string.ep_user),
                        camera.optString("user"),
                        InputType.TYPE_CLASS_TEXT);
        EditText password =
                field(
                        body,
                        text(R.string.ep_password),
                        camera.optString("password"),
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText count =
                field(
                        body,
                        text(R.string.ep_channels),
                        String.valueOf(camera.optInt("channels", 4)),
                        InputType.TYPE_CLASS_NUMBER);
        EditText port =
                field(
                        body,
                        text(R.string.ep_rtsp_port),
                        String.valueOf(camera.optInt("port", 554)),
                        InputType.TYPE_CLASS_NUMBER);
        button(
                text(R.string.ep_save),
                () -> {
                    try {
                        String h = host.getText().toString().trim();
                        int c = Integer.parseInt(count.getText().toString());
                        int p = Integer.parseInt(port.getText().toString());
                        if (h.isEmpty()
                                || !h.matches("[A-Za-z0-9.-]+")
                                || c < 1
                                || c > 64
                                || p < 1
                                || p > 65535
                                || user.getText().toString().isEmpty()
                                || password.getText().toString().isEmpty())
                            throw new IllegalArgumentException();
                        JSONObject v =
                                new JSONObject()
                                        .put("host", h)
                                        .put("user", user.getText().toString())
                                        .put("password", password.getText().toString())
                                        .put("channels", c)
                                        .put("port", p);
                        saveCamera(v);
                        camera = v;
                        gridOffset = 0;
                        cameras();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.ep_invalid_camera, Toast.LENGTH_LONG).show();
                    }
                });
        button(
                text(R.string.ep_forget_camera),
                () ->
                        new OwnDialogBuilder(this)
                                .setMessage(R.string.ep_forget_confirm)
                                .setPositiveButton(
                                        android.R.string.ok,
                                        (d, w) -> {
                                            getSharedPreferences("camera", 0)
                                                    .edit()
                                                    .clear()
                                                    .apply();
                                            camera = new JSONObject();
                                            cameras();
                                        })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show());
        button(text(R.string.ep_back), this::cameras);
    }

    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias("ep-camera")) {
            KeyGenerator g =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            g.init(
                    new KeyGenParameterSpec.Builder(
                                    "ep-camera",
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build());
            g.generateKey();
        }
        return (javax.crypto.SecretKey) ks.getKey("ep-camera", null);
    }

    private void saveCamera(JSONObject v) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] data = c.doFinal(v.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        getSharedPreferences("camera", 0)
                .edit()
                .putString("iv", android.util.Base64.encodeToString(c.getIV(), 2))
                .putString("data", android.util.Base64.encodeToString(data, 2))
                .apply();
    }

    private JSONObject readCamera() {
        try {
            File incoming = new File(getFilesDir(), "camera-import.json");
            if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
                            != 0
                    && incoming.exists()) {
                JSONObject initial =
                        new JSONObject(
                                new String(
                                        java.nio.file.Files.readAllBytes(incoming.toPath()),
                                        java.nio.charset.StandardCharsets.UTF_8));
                if (initial.optString("host").isEmpty()
                        || initial.optString("password").isEmpty()
                        || initial.optInt("channels") < 1
                        || initial.optInt("channels") > 64) throw new IllegalArgumentException();
                saveCamera(initial);
                incoming.delete();
            }
            android.content.SharedPreferences p = getSharedPreferences("camera", 0);
            if (!p.contains("data")) return new JSONObject();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(
                    Cipher.DECRYPT_MODE,
                    key(),
                    new GCMParameterSpec(
                            128, android.util.Base64.decode(p.getString("iv", ""), 2)));
            return new JSONObject(
                    new String(
                            c.doFinal(android.util.Base64.decode(p.getString("data", ""), 2)),
                            java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void releasePlayers() {
        if (cameraPanel != null) {
            cameraPanel.shutdown();
            cameraPanel = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showGrid(int offset, int only) {
        releasePlayers();
        grid = true;
        gridOffset = offset;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraPanel = new OwnCameraPanel(this, camera, this::cameras, savedCameraState);
        savedCameraState = null;
        setContentView(cameraPanel);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("ep_page", page);
        state.putString("news_parent", newsParent);
        state.putString("image_group", imageGroup);
        state.putInt("image_index", imageIndex);
        if (cameraPanel != null) state.putBundle("camera_panel", cameraPanel.saveState());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraPanel != null) cameraPanel.pause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (cameraPanel != null) cameraPanel.resume();
        else if (page.equals("datetime")) dateTime();
    }

    @Override
    protected void onDestroy() {
        releasePlayers();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (page.equals("news")) {
            if (newsParent.equals("customize")) customize();
            else settings();
            return;
        }
        if (page.equals("favorites")) {
            customize();
            return;
        }
        if (grid) {
            if (cameraPanel != null && cameraPanel.back()) return;
            cameras();
            return;
        }
        super.onBackPressed();
    }
}

package com.conreo.couchytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.conreo.couchytv.ui.OwnTVApp
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Bumped whenever a package is installed/removed so the app grid rescans. */
    var rescanTick by mutableIntStateOf(0)
        private set

    private var homeRequestTick by mutableIntStateOf(0)

    private val packageReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pkg = intent.data?.schemeSpecificPart
                // Removals always rescan (the app may be on the grid). For installs and
                // changes, skip the full scan unless the package is actually launchable.
                val launchable =
                    pkg != null &&
                        (packageManager.getLeanbackLaunchIntentForPackage(pkg) != null ||
                            packageManager.getLaunchIntentForPackage(pkg) != null)
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED || launchable) {
                    com.conreo.couchytv.data.AppRepository.invalidate()
                    rescanTick++
                }
            }
        }

    override fun attachBaseContext(newBase: Context) {
        // Read the chosen UI language synchronously from SharedPreferences — cheap,
        // and off the DataStore (a runBlocking DataStore read here blocked the main
        // thread and made language changes hang until a restart). LauncherApp keeps
        // this mirror in sync with the DataStore config and triggers recreate().
        val lang =
            newBase
                .getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                .getString(LOCALE_KEY, "")
                .orEmpty()
        super.attachBaseContext(if (lang.isEmpty()) newBase else applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(OwnBrand.theme(this))
        super.onCreate(savedInstanceState)

        // A home screen never exits on Back
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* no-op */
                }
            },
        )

        androidx.core.content.ContextCompat.registerReceiver(
            this,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            },
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        EpDefaults.prepare(this)

        setContent {
            OwnTVApp(rescanTick, homeRequestTick)
        }
        OwnBrand.showIntro(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME))
            homeRequestTick++
    }

    override fun onResume() {
        super.onResume()
        rescanTick++
        if (
            getSharedPreferences("own_ssh", MODE_PRIVATE).getBoolean("enabled", false) &&
                !OwnSshService.isRunning(this)
        ) {
            OwnSshService.start(this)
        }
        if (
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
                java.io.File(filesDir, "camera-import.json").exists()
        ) {
            startActivity(Intent(this, EpActivity::class.java).putExtra("page", "cameras"))
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            Actions.launchApp(this, "ep:settings")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    companion object {
        private const val LOCALE_PREFS = "locale"
        private const val LOCALE_KEY = "lang"

        /** Mirror the chosen language so attachBaseContext can read it synchronously. */
        fun persistLocale(context: Context, lang: String) {
            context
                .getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(LOCALE_KEY, lang)
                .apply()
        }

        fun currentLocalePref(context: Context): String =
            context
                .getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                .getString(LOCALE_KEY, "")
                .orEmpty()

        fun applyLocale(context: Context, lang: String): Context {
            val locale =
                if (lang.contains('-')) {
                    val parts = lang.split('-')
                    Locale(parts[0], parts.getOrElse(1) { "" })
                } else Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
}

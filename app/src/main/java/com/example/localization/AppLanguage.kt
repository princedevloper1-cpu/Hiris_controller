package com.example.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import java.util.Locale

/** Stores the app language and applies it to Android resources. */
object AppLanguage {
    private const val PREFS = "hiris_app_language"
    private const val KEY_LANGUAGE = "language_tag"

    fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            manager?.applicationLocales?.toLanguageTags()?.takeIf(String::isNotBlank)?.let { return it }
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun wrap(context: Context): Context {
        val tag = currentTag(context)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    fun setLanguage(activity: ComponentActivity, tag: String) {
        val normalized = if (tag == "fr") "fr" else "en"
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, normalized).apply()
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(normalized)
        } else {
            activity.recreate()
        }
    }
}

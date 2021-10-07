package com.davidfadare.notes.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.text.TextUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, GeneralFragment())
                .commit()

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            startActivity(Intent(this@SettingsActivity, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        return super.onOptionsItemSelected(item)
    }

    class GeneralFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences_general)

            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(KEY_PREF_RINGTONE), sBindPreferenceSummaryToValueListener)

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                onDestroy()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

    }

    companion object {

        private lateinit var instance: SettingsActivity
        var KEY_PREF_NOTE_SIZE = "pref_noteSize"
        var KEY_PREF_AUTO_SAVE = "pref_auto_save"
        var KEY_PREF_PASSWORD = "pref_pass"
        var KEY_PREF_PASSWORD_ENABLE = "pref_pass_enable"
        var KEY_PREF_LANGUAGE = "pref_language"
        var KEY_PREF_GEN_ENABLE = "pref_gen_enable"
        var KEY_PREF_NOTIFICATION = "notifications_custom"
        var KEY_PREF_RINGTONE = "pref_ringtone"
        var KEY_PREF_VIBRATE = "pref_vibrate"
        var KEY_PREF_RESTORE = "pref_restore"
        //TODO: FINAL: Add setting for constant restore. Requires pro.

        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {

                val index = preference.findIndexOfValue(stringValue)

                preference.setSummary(
                        if (index >= 0) preference.entries[index]
                        else null)

            } else if (preference is RingtonePreference) {
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary(R.string.pref_ringtone_silent)

                } else {
                    val ringtone = RingtoneManager.getRingtone(
                            preference.context, Uri.parse(stringValue))

                    try {
                        if (ringtone == null) {
                            preference.setSummary(null)
                        } else {
                            val name = ringtone.getTitle(preference.getContext())
                            preference.setSummary(name)
                        }
                    } catch (e: SecurityException) {
                        preference.setSummary(null)
                    }
                }

            } else {
                preference.summary = stringValue
            }

            true
        }

        private fun bindPreferenceSummaryToValue(preference: Preference, listener: Preference.OnPreferenceChangeListener) {
            preference.onPreferenceChangeListener = listener

            listener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.context).getString(preference.key, ""))
        }

        fun setLanguage(context: Context, language: String) {
            val lan = when (language.toLowerCase()) {
                "english" -> "en"
                "español" -> "es"
                "हिंदी" -> "hi"
                else -> "en"
            }

            val locale = Locale(lan)
            val systemLocale = Locale.getDefault()
            if (systemLocale != null && systemLocale == locale) {
                return
            }

            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }

        fun setLanguageReset(context: Activity, language: String) {
            val lan = when (language.toLowerCase()) {
                "english" -> "en"
                "español" -> "es"
                "हिंदी" -> "hi"
                else -> "en"
            }

            val locale = Locale(lan)
            val systemLocale = Locale.getDefault()
            if (systemLocale != null && systemLocale == locale) {
                return
            }

            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context.recreate()
        }
    }

}

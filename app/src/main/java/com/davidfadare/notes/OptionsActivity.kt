package com.davidfadare.notes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.createChooser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.davidfadare.notes.settings.*
import com.davidfadare.notes.util.Utility.Companion.playUri
import kotlinx.android.synthetic.main.settings_option.*

class OptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_option)

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        screen.setOnClickListener { startActivity(Intent(this@OptionsActivity, SettingsActivity::class.java)) }

        feedback.setOnClickListener {
            val intentReport = Intent(Intent.ACTION_SENDTO)
            val uriText = "mailto:" + Uri.encode("dfadr13@gmail.com") +
                    "?subject=" + Uri.encode(getString(R.string.feedback) + " " + BuildConfig.VERSION_NAME)
            val uri = Uri.parse(uriText)
            intentReport.data = uri
            startActivity(createChooser(intentReport, getString(R.string.share)))
        }

        rate.setOnClickListener {
            val u = Uri.parse("market://details?id=" + application.packageName)
            val goToMarket = Intent(Intent.ACTION_VIEW, u)
            val flags = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                -> {
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                }
                else -> {
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                }
            }
            goToMarket.addFlags(flags)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(playUri + application.packageName)))
            }
        }

        about.setOnClickListener { startActivity(Intent(this@OptionsActivity, AboutActivity::class.java)) }

        export.setOnClickListener { startActivity(Intent(this@OptionsActivity, ExportActivity::class.java)) }

        backup.setOnClickListener { startActivity(Intent(this@OptionsActivity, RestoreActivity::class.java)) }

        purchase.setOnClickListener { startActivity(Intent(this@OptionsActivity, PurchaseActivity::class.java)) }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
        }

        return super.onOptionsItemSelected(item)
    }

}

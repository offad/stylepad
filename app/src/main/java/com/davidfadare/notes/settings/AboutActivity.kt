package com.davidfadare.notes.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsActivity

class AboutActivity : LibsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val builder = LibsBuilder().withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
        builder.withAboutIconShown(true)
        builder.withAboutAppName(resources.getString(R.string.app_name))
        builder.withAboutVersionShown(true)
        builder.withAboutDescription(getString(R.string.pref_action_desc))
        builder.withActivityTitle(getString(R.string.about_activity))
        builder.withLicenseDialog(true)
        intent = builder.intent(this)

        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        return true
    }
}

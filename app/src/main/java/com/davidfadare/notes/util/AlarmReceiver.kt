package com.davidfadare.notes.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.davidfadare.notes.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent(context, com.davidfadare.notes.util.AlarmService::class.java)
        i.putExtra(context.getString(R.string.note_alarm), intent.getIntExtra(context.getString(R.string.note_alarm), 0))
        i.putExtra(context.getString(R.string.note_title), intent.getStringExtra(context.getString(R.string.note_title)))
        i.putExtra(context.getString(R.string.note_text), intent.getStringExtra(context.getString(R.string.note_text)))
        i.putExtra(context.getString(R.string.note_color), intent.getIntExtra(context.getString(R.string.note_color), ContextCompat.getColor(context, R.color.blue_note)))
        AlarmService.enqueueWork(context, i)
    }
}

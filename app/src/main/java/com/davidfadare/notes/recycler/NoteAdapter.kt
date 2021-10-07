package com.davidfadare.notes.recycler

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.preference.PreferenceManager
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.davidfadare.notes.R
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.settings.SettingsActivity
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(context: Context, objects: Array<Note>) : ArrayAdapter<Note>(context, 0, objects) {

    var mNoteTable: Array<Note>? = objects

    override fun getView(position: Int, cView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var convertView = cView

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val syncConnPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_NOTE_SIZE, true)

        if (convertView == null) {
            convertView = if (syncConnPref) {
                LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
            } else {
                LayoutInflater.from(context).inflate(R.layout.list_item_small, parent, false)
            }
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val currentNote = mNoteTable?.get(position)

        val noteName = currentNote?.title
        holder.titleView.text = noteName

        var noteText = currentNote?.text
        if (holder.previewView != null) {
            noteText = noteText?.replace("&nbsp;".toRegex(), " ")

            if (!TextUtils.isEmpty(noteText)) {
                noteText = noteText?.replace("\n".toRegex(), "<br>")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    holder.previewView.text = Html.fromHtml(noteText, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    holder.previewView.text = Html.fromHtml(noteText).toString()
                }
            } else if (TextUtils.isEmpty(noteText)) {
                holder.previewView.text = ""
            }
        }

        val noteDate = Date(currentNote?.noteDate ?: 0)
        val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(noteDate)
        holder.dateView.text = date

        val noteColor = currentNote?.noteColor
        val drawable = holder.listBorder.background as GradientDrawable
        drawable.setColor(noteColor ?: 0)

        val notePassword = currentNote?.password

        val syncGenPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_GEN_ENABLE, false)
        val passEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_PASSWORD_ENABLE, true)
        if (passEnabled && !syncGenPref && !TextUtils.isEmpty(notePassword)) {
            holder.passwordImage.visibility = View.VISIBLE
        } else if (passEnabled && syncGenPref) {
            holder.passwordImage.setImageResource(R.drawable.listitem_lock_master)
        } else {
            holder.passwordImage.visibility = View.INVISIBLE
        }

        val noteAlarm = currentNote?.alarmDate ?: 0
        if (noteAlarm > System.currentTimeMillis()) {
            holder.clockImage.visibility = View.VISIBLE
        } else {
            holder.clockImage.visibility = View.INVISIBLE
        }


        val notePin = currentNote?.pinStatus
        if (notePin == 1) {
            holder.pinImage.visibility = View.VISIBLE
        } else {
            holder.pinImage.visibility = View.INVISIBLE
        }

        return convertView!!
    }

    override fun getItemId(position: Int): Long {
        return mNoteTable?.get(position)?.id?.toLong() ?: super.getItemId(position)
    }


    class ViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
        val titleView: TextView = convertView.findViewById(R.id.title_text)
        val previewView: TextView? = convertView.findViewById(R.id.pre_text)
        val dateView: TextView = convertView.findViewById(R.id.date_view)
        val listBorder: View = convertView.findViewById(R.id.list_border)
        val passwordImage: ImageView = convertView.findViewById(R.id.lock_image)
        val clockImage: ImageView = convertView.findViewById(R.id.clock_image)
        val pinImage: ImageView = convertView.findViewById(R.id.pin_image)
    }
}

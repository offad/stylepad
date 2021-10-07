package com.davidfadare.notes.util

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.davidfadare.notes.R
import com.davidfadare.notes.data.NoteRepository
import com.davidfadare.notes.util.Utility.Companion.defaultQuery
import java.text.SimpleDateFormat
import java.util.*

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return NoteRemoteFactory(application, intent)
    }

    class NoteRemoteFactory(val context: Application, val intent: Intent?) : RemoteViewsFactory {

        private val databaseRepository = NoteRepository.getInstance(context)
        private var database = databaseRepository.queryOrderBy(defaultQuery.create())

        override fun onCreate() {
        }

        override fun getViewAt(position: Int): RemoteViews {
            val view = RemoteViews(context.packageName, R.layout.list_item_widget)

            view.setTextViewText(R.id.title_text, database[position].title)

            view.setInt(R.id.stub, "setColorFilter", database[position].noteColor)

            val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(database[position].noteDate)

            view.setTextViewText(R.id.date_view, date)

            val extras = Bundle()
            extras.putInt(WidgetProvider.EXTRA_ITEM, position)
            extras.putLong(WidgetProvider.EXTRA_NOTE_ID, getItemId(position))
            val fillInIntent = Intent()
            fillInIntent.putExtras(extras)
            view.setOnClickFillInIntent(R.id.widget_item, fillInIntent)

            return view
        }

        override fun getCount(): Int {
            return database.size
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun onDataSetChanged() {
            database = databaseRepository.queryOrderBy(defaultQuery.create())
        }

        override fun getItemId(position: Int): Long {
            return database[position].id.toLong()
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }
    }
}
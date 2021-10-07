package com.davidfadare.notes.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.davidfadare.notes.BuildConfig
import com.davidfadare.notes.MainActivity
import com.davidfadare.notes.R

class WidgetProvider : AppWidgetProvider() {
    companion object {
        const val OPEN_ACTION = "${BuildConfig.APPLICATION_ID}.widget.OPEN_ACTION"
        const val NEW_ACTION = "${BuildConfig.APPLICATION_ID}.widget.NEW_ACTION"
        const val EXTRA_ITEM = "${BuildConfig.APPLICATION_ID}.EXTRA_ITEM"
        const val EXTRA_NOTE_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_NOTE_ID"
        const val WIDGET_IDS_KEY = "${BuildConfig.APPLICATION_ID}.WIDGET_IDS_KEY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            OPEN_ACTION -> {
                val open = Intent(context, MainActivity::class.java)
                open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                open.action = OPEN_ACTION
                open.putExtra(EXTRA_NOTE_ID, intent.getLongExtra(EXTRA_NOTE_ID, 0))
                open.putExtra(EXTRA_ITEM, intent.getIntExtra(EXTRA_ITEM, 0))
                context.startActivity(open)
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                if (intent.hasExtra(WIDGET_IDS_KEY)) {
                    val ids = intent.extras?.getIntArray(WIDGET_IDS_KEY)
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
                }
            }
        }

        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_main)

        val svcIntent = Intent(context, WidgetService::class.java)
        svcIntent.data = Uri.fromParts("content", "$appWidgetId", null)

        views.setTextViewText(R.id.widget_title, context.resources.getString(R.string.app_name))
        views.setRemoteAdapter(R.id.widget_list, svcIntent)
        views.setEmptyView(R.id.widget_list, R.id.empty_view)

        val newIntent = Intent(context, MainActivity::class.java)
        newIntent.action = NEW_ACTION
        views.setImageViewResource(R.id.widget_button, R.drawable.menu_plus)
        views.setImageViewResource(R.id.widget_icon, R.drawable.app_icon)
        val newPendingIntent = PendingIntent.getActivity(context, 0, newIntent, 0)
        views.setOnClickPendingIntent(R.id.widget_button, newPendingIntent)

        val noteIntent = Intent(context, WidgetProvider::class.java)
        noteIntent.action = OPEN_ACTION
        noteIntent.data = Uri.parse(noteIntent.toUri(Intent.URI_INTENT_SCHEME))
        val pendingIntent = PendingIntent.getBroadcast(context, 0, noteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}
package com.davidfadare.notes.recycler

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.davidfadare.notes.R

class ColorAdapter(context: Context, vararg objects: String) : ArrayAdapter<String>(context, 0, objects) {

    private val mColorsArray: Array<out String> = objects

    override fun getView(position: Int, cView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var convertView = cView

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.color_item, parent, false)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val drawable = holder.playButton.background as GradientDrawable
        val color = Color.parseColor(mColorsArray[position])
        drawable.setColor(color)

        return convertView!!
    }

    class ViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
        val playButton: View = convertView.findViewById(R.id.color_button)
    }
}

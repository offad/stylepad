package com.davidfadare.notes.recycler

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.davidfadare.notes.R
import java.io.File

class ImageAdapter(context: Context, color: Int, vararg objects: String) : ArrayAdapter<String>(context, 0, objects) {

    var mImageLocations: Array<out String> = objects
    private var noteColor = color

    override fun getView(position: Int, cView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var convertView = cView

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val image = File(mImageLocations[position])

        val bitmap = BitmapFactory.decodeFile(image.toString())
        holder.imageView.setImageBitmap(bitmap)
        holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        holder.background.setBackgroundColor(noteColor)

        if(!image.exists()) convertView?.visibility = View.GONE

        return convertView!!
    }

    class ViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
        val imageView: ImageView = convertView.findViewById(R.id.grid_image)
        val background: FrameLayout = convertView.findViewById(R.id.grid_background)
    }
}
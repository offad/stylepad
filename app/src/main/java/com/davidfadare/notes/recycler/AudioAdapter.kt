package com.davidfadare.notes.recycler;

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.davidfadare.notes.R
import com.davidfadare.notes.util.Utility.Companion.getListItemByPosition
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AudioAdapter(context: Context, color: Int, vararg objects: String) : ArrayAdapter<String>(context, 0, objects) {

    private var mAudioLocations: Array<out String> = objects
    private var mediaPaused = true
    private var selected: Int = -1
    private var noteColor = color

    private val handler = Handler()
    private val mediaPlayer = MediaPlayer()
    private val mediaMetadataRetriever = MediaMetadataRetriever()
    private lateinit var runnable: Runnable

    override fun getView(position: Int, cView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var convertView = cView

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.audio_item, parent, false)

            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val audio = File(mAudioLocations[position])

        holder.titleText.text = audio.name
        holder.timeText.text = getDuration(audio)
        holder.dateText.text = getDate(audio)
        holder.divider.setBackgroundColor(noteColor)
        holder.playButton.setOnClickListener {
            if (mediaPaused) {
                if (position != selected) {
                    changeAudio(parent, holder, audio, position)
                } else if (mediaPaused) {
                    mediaPlayer.start()
                    mediaPaused = false
                    changeSeekBar(holder.progressBar, holder.timeText)
                }
            } else {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    if (position != selected) {
                        changeAudio(parent, holder, audio, position)
                    } else {
                        holder.playButton.setImageResource(R.drawable.dialog_play)
                        mediaPaused = true
                    }
                } else if (position != selected) {
                    changeAudio(parent, holder, audio, position)
                } else {
                    movePlayer(0, holder)
                }
            }
        }

        if(!audio.exists()) convertView?.visibility = View.GONE

        return convertView!!
    }

    private fun changeAudio(parent: ViewGroup, holder: ViewHolder, audio: File, position: Int) {
        if (selected != -1) {
            mediaPlayer.stop()
            mediaPlayer.reset()
            cleanPreviousView(parent)
        }
        setupPlayer(holder, audio)
        selected = position
    }


    fun stopAudio() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            mediaPaused = true
            notifyDataSetChanged()
        }
    }

    private fun setupPlayer(holder: ViewHolder, audio: File) {
        holder.titleText.setTextColor(noteColor)
        holder.playButton.setImageResource(R.drawable.dialog_pause)
        holder.progressBar.visibility = View.VISIBLE
        holder.progressBar.progressDrawable.colorFilter = PorterDuffColorFilter(noteColor, PorterDuff.Mode.SRC_IN)
        holder.progressBar.thumb.colorFilter = PorterDuffColorFilter(noteColor, PorterDuff.Mode.SRC_IN)
        holder.dateText.visibility = View.GONE
        holder.progressBar.progress = 0

        try {
            mediaPlayer.setDataSource(context, Uri.fromFile(audio))

            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
                mediaPaused = false
                holder.progressBar.max = mediaPlayer.duration
                changeSeekBar(holder.progressBar, holder.timeText)
            }

            mediaPlayer.setOnCompletionListener {
                holder.playButton.setImageResource(R.drawable.dialog_play)
                mediaPaused = true
            }

            holder.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        movePlayer(progress, holder)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    holder.timeText.text = String.format("%s / %s", convertTime(mediaPlayer.currentPosition), convertTime(mediaPlayer.duration))
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    holder.timeText.text = String.format("%s / %s", convertTime(mediaPlayer.currentPosition), convertTime(mediaPlayer.duration))
                }
            })

            mediaPlayer.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun movePlayer(progress: Int, holder: ViewHolder) {
        mediaPlayer.seekTo(progress)
        mediaPlayer.start()
        holder.playButton.setImageResource(R.drawable.dialog_pause)
        changeSeekBar(holder.progressBar, holder.timeText)
    }

    private fun cleanPreviousView(parent: ViewGroup) {
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        val audio = File(mAudioLocations[selected])
        val androidBlack = ContextCompat.getColor(context, android.R.color.black)

        val previousHolder = getListItemByPosition(selected, parent as ListView).tag as ViewHolder
        previousHolder.playButton.setImageResource(R.drawable.dialog_play)
        previousHolder.titleText.setTextColor(androidBlack)
        previousHolder.progressBar.progressDrawable.colorFilter = PorterDuffColorFilter(noteColor, PorterDuff.Mode.SRC_IN)
        previousHolder.progressBar.thumb.colorFilter = PorterDuffColorFilter(noteColor, PorterDuff.Mode.SRC_IN)
        previousHolder.timeText.text = getDuration(audio)
        previousHolder.progressBar.visibility = View.GONE
        previousHolder.dateText.visibility = View.VISIBLE

        notifyDataSetChanged()
    }

    private fun changeSeekBar(progressBar: SeekBar, textView: TextView) {
        progressBar.progress = mediaPlayer.currentPosition
        textView.text = String.format("%s / %s", convertTime(mediaPlayer.currentPosition), convertTime(mediaPlayer.duration))
        if (!mediaPaused) {
            runnable = Runnable {
                changeSeekBar(progressBar, textView)
            }
            handler.postDelayed(runnable, 100)
        }
    }

    private fun getDuration(audio: File): String? {
        var duration: String?
        try {
            mediaMetadataRetriever.setDataSource(context, Uri.fromFile(audio))
            val ms = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            duration = convertTime(ms)
        } catch (e: Exception) {
            e.printStackTrace()
            duration = null
        }
        return duration
    }

    private fun convertTime(ms: Int): String {
        val time = ms.toLong()
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(time),
                TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)))
    }

    private fun getDate(audio: File): String? {
        var date: String?
        try {
            val ms = audio.lastModified()
            date = if (SimpleDateFormat("yyyy", Locale.getDefault()).format(ms) != SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())) {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(ms)
            } else {
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(ms)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            date = null
        }
        return date
    }

    class ViewHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
        val playButton: ImageView = convertView.findViewById(R.id.audio_play)
        val titleText: TextView = convertView.findViewById(R.id.audio_title)
        val timeText: TextView = convertView.findViewById(R.id.audio_time)
        val dateText: TextView = convertView.findViewById(R.id.audio_date)
        val divider: View = convertView.findViewById(R.id.audio_divider)
        val progressBar: SeekBar = convertView.findViewById(R.id.audio_progress_bar)
    }
}

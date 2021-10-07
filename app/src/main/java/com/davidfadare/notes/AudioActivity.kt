package com.davidfadare.notes

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davidfadare.notes.EditorActivity.Companion.noteAudioTAG
import com.davidfadare.notes.EditorActivity.Companion.noteColorTAG
import com.davidfadare.notes.util.CircularProgressBar
import com.davidfadare.notes.util.Utility.Companion.changeWindowColor
import com.davidfadare.notes.util.Utility.Companion.createDialog
import com.davidfadare.notes.util.Utility.Companion.mediaStorageDir
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AudioActivity : AppCompatActivity() {

    private var locations: Array<String> = emptyArray()
    private var recordings: Array<String> = emptyArray()
    private var noteColor: Int = 0
    private var timeWhenPaused = 0L
    private var startRecording = true
    private var recording = false
    private var audioFile: String = ""

    private val handler = Handler()

    private lateinit var runnable: Runnable
    private lateinit var mediaRecorder: MediaRecorder

    var chronometer: Chronometer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        noteColor = intent.getIntExtra(noteColorTAG, 0)
        locations = intent.getStringArrayExtra(noteAudioTAG)
        if (noteColor == 0) {
            noteColor = ContextCompat.getColor(this, R.color.blue_note)
        }

        changeWindowColor(this, window, noteColor)

        super.onCreate(savedInstanceState)
        overridePendingTransition(0, android.R.anim.slide_out_right)
        setContentView(R.layout.activity_audio)

        onSetup()

        checkAudioPermissions(this)
    }

    private fun onSetup() {
        mediaRecorder = MediaRecorder()
        if (audioFile.isEmpty()) audioFile = getOutputMediaFile()?.path ?: return

        chronometer = findViewById<Chronometer>(R.id.audio_chronometer)
        val progressBar = findViewById<CircularProgressBar>(R.id.audio_progress_bar)
        progressBar.setColor(noteColor)

        val buttonLayout = findViewById<RelativeLayout>(R.id.audio_button_layout)
        buttonLayout.setBackgroundColor(noteColor)
        val titleLayout = findViewById<ViewGroup>(R.id.audio_title_layout)
        titleLayout.setBackgroundColor(noteColor)

        val recordButton = findViewById<FloatingActionButton>(R.id.audio_record_button)
        val stopButton = findViewById<ImageView>(R.id.audio_save_button)
        val clearButton = findViewById<ImageView>(R.id.audio_clear_button)

        val pauseImage = findViewById<AppCompatTextView>(R.id.audio_pause_image)
        val colorFilter = PorterDuffColorFilter(noteColor, PorterDuff.Mode.SRC_ATOP)
        pauseImage.compoundDrawables[1].colorFilter = colorFilter

        val editText = findViewById<AppCompatEditText>(R.id.audio_name)
        editText.setText(audioFile.substringAfterLast(File.separator))

        recordButton.backgroundTintList = ColorStateList.valueOf(noteColor)
        recordButton.setOnClickListener {
            if (startRecording) {
                setupMediaRecorder(audioFile)
                try {
                    mediaRecorder.prepare()
                    mediaRecorder.start()
                    recording = true
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                recordButton.setImageResource(R.drawable.dialog_pause)

                pauseImage.visibility = View.INVISIBLE
                chronometer?.visibility = View.VISIBLE
                stopButton.isEnabled = false
                stopButton.visibility = View.INVISIBLE

                progressBar.setProgress(0f)
                chronometer?.start()
                chronometer?.base = SystemClock.elapsedRealtime() + timeWhenPaused
                changeSeekBar(progressBar, chronometer)
            } else {
                if (recording) {
                    mediaRecorder.stop()
                    mediaRecorder.reset()
                    recording = false
                    recordings += audioFile
                    audioFile = getOutputMediaFile()?.path ?: ""
                }

                recordButton.setImageResource(R.drawable.dialog_record)

                pauseImage.visibility = View.VISIBLE
                chronometer?.visibility = View.INVISIBLE
                stopButton.isEnabled = true
                stopButton.visibility = View.VISIBLE

                timeWhenPaused = chronometer?.base?.minus(SystemClock.elapsedRealtime()) ?: 0
                pauseImage.text = convertTime(0 - timeWhenPaused)
                progressBar.setProgress(chronometer?.base?.toFloat() ?: 0F)
                chronometer?.stop()
            }

            startRecording = !startRecording
        }

        clearButton?.setOnClickListener {
            onBackPressed()
        }

        stopButton.setOnClickListener {
            mediaRecorder.release()
            onSaveAudio(editText.text.toString().trim())
        }

        if (recording) {
            setupMediaRecorder(audioFile)
            try {
                mediaRecorder.prepare()
                mediaRecorder.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            recordButton.setImageResource(R.drawable.dialog_pause)

            pauseImage.visibility = View.INVISIBLE
            chronometer?.visibility = View.VISIBLE
            stopButton.isEnabled = false
            stopButton.visibility = View.INVISIBLE

            progressBar.setProgress(0f)
            chronometer?.start()
            changeSeekBar(progressBar, chronometer)
        }

        chronometer?.base = SystemClock.elapsedRealtime() + timeWhenPaused
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (recording) {
            mediaRecorder.stop()
            mediaRecorder.reset()
            recordings += audioFile
            audioFile = getOutputMediaFile()?.path ?: ""
            timeWhenPaused = chronometer?.base?.minus(SystemClock.elapsedRealtime()) ?: 0
        }

        setContentView(R.layout.activity_audio)

        onSetup()
    }

    private fun changeSeekBar(progressBar: CircularProgressBar, chronometer: Chronometer?) {
        if (chronometer != null) progressBar.setProgress(SystemClock.elapsedRealtime() - chronometer.base.toFloat())
        progressBar.max = progressBar.getProgress().toInt() + 144
        if (recording) {
            runnable = Runnable {
                changeSeekBar(progressBar, chronometer)
            }
            handler.postDelayed(runnable, 144)
        } else {
            progressBar.setProgress(progressBar.max.toFloat())
        }
    }

    private fun setupMediaRecorder(output: String) {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
        mediaRecorder.setOutputFile(output)
    }

    private fun mergeMediaFiles(isAudio: Boolean, sourceFiles: Array<String>): Boolean {
        audioFile = getOutputMediaFile()?.path ?: return false
        try {
            val mediaKey: String = if (isAudio) "soun" else "vide"
            val listMovies = ArrayList<Movie>()
            for (filename: String in sourceFiles) {
                listMovies.add(MovieCreator.build(filename))
            }
            val listTracks = LinkedList<Track>()
            for (movie: Movie in listMovies) {
                for (track: Track in movie.tracks) {
                    if (track.handler == mediaKey) {
                        listTracks.add(track)
                    }
                }
            }
            val outputMovie = Movie()
            if (listTracks.isNotEmpty()) {
                outputMovie.addTrack(AppendTrack(*listTracks.toTypedArray()))
            }
            val container = DefaultMp4Builder().build(outputMovie)
            val fileChannel = RandomAccessFile(String.format(audioFile), "rw").channel
            container.writeContainer(fileChannel)
            fileChannel.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun onSaveAudio(name: String) {
        try {
            if (mergeMediaFiles(true, recordings)) {
                val from = File(audioFile)
                val to = File("$mediaStorageDir${File.separator}${name.removeSuffix(".3gp")}.3gp")
                from.renameTo(to)

                locations += to.path

                val returnIntent = Intent()
                returnIntent.putExtra(noteAudioTAG, locations)
                setResult(Activity.RESULT_OK, returnIntent)
            } else {
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
        } catch (e: IOException) {
            Toast.makeText(this, R.string.pref_header_permission, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
        } finally {
            finish()
        }
    }

    private fun getOutputMediaFile(): File? {
        val mediaFile = File(mediaStorageDir)
        if (!mediaFile.exists()) if (!mediaFile.mkdirs()) return null
        val timeStamp = SimpleDateFormat("ddMMyy_hhmmss", Locale.getDefault()).format(Date())
        val mAudioName = "AUD_$timeStamp.3gp"
        return File("${mediaFile.path}${File.separator}$mAudioName")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            EditorActivity.PERMISSION_REQUEST_AUDIO_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()

                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        createDialog(this, getString(R.string.unsaved_changes_dialog_msg), getString(R.string.keep_editing), getString(R.string.discard), discardButtonClickListener)

    }

    private fun checkAudioPermissions(context: Activity) {
        val writeExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val recordAudioResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)

        if (writeExternalStorage != PackageManager.PERMISSION_GRANTED || recordAudioResult != PackageManager.PERMISSION_GRANTED || readExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_EXTERNAL_STORAGE), EditorActivity.PERMISSION_REQUEST_AUDIO_CODE)
        }
    }

    private fun convertTime(time: Long): String {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(time),
                TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)))
    }
}

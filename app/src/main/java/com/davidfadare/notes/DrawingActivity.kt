package com.davidfadare.notes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davidfadare.notes.EditorActivity.Companion.noteColorTAG
import com.davidfadare.notes.EditorActivity.Companion.noteDrawingTAG
import com.davidfadare.notes.recycler.ColorAdapter
import com.davidfadare.notes.util.DrawView
import com.davidfadare.notes.util.Utility
import com.davidfadare.notes.util.Utility.Companion.changeWindowColor
import com.davidfadare.notes.util.Utility.RealPathUtil.getRealPath
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DrawingActivity : AppCompatActivity() {

    private var locations: Array<String> = emptyArray()
    private var position: Int = -1
    private var noteColor: Int = 0
    private var imageUri: Uri? = null

    private var drawView: DrawView? = null

    private var pictureFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        noteColor = intent.getIntExtra(noteColorTAG, 0)
        locations = intent.getStringArrayExtra(noteDrawingTAG)
        position = intent.getIntExtra(noteDrawingIDTAG, -1)
        if (noteColor == 0) {
            noteColor = ContextCompat.getColor(this, R.color.blue_note)
        }

        changeWindowColor(this, window, noteColor)

        super.onCreate(savedInstanceState)
        overridePendingTransition(0, android.R.anim.slide_out_right)
        setContentView(R.layout.activity_draw)

        imageUri = intent.getParcelableExtra(noteDrawingURITAG)

        if (savedInstanceState != null) {
            pictureFile = savedInstanceState.getString(noteDrawingURITAG, "")
        } else if (position != -1) {
            pictureFile = locations[position]
        }

        onSetup()

        checkDrawingPermissions(this)
    }

    private fun onSetup() {
        val clearButton = findViewById<ImageView>(R.id.drawing_clear_button)
        val undoButton = findViewById<ImageView>(R.id.drawing_undo_button)
        val redoButton = findViewById<ImageView>(R.id.drawing_redo_button)
        val paletteButton = findViewById<ImageView>(R.id.drawing_palette_button)
        val saveButton = findViewById<ImageView>(R.id.drawing_save_button)
        val buttonLayout = findViewById<LinearLayout>(R.id.drawing_button_layout)
        val drawingContainer = findViewById<FrameLayout>(R.id.drawing_container)

        buttonLayout.setBackgroundColor(noteColor)

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        val config = resources.configuration
        val currentBitmap = getBitmap(size)

        drawView = DrawView(this, currentBitmap)

        drawView?.requestFocus()

        drawingContainer?.addView(drawView)

        onChangeSize(config, size)

        if (pictureFile.isBlank()) pictureFile = getOutputMediaFile()?.path ?: return

        saveButton?.setOnClickListener {
            saveBitmap()
        }
        redoButton?.setOnClickListener {
            drawView?.redo()
        }
        undoButton?.setOnClickListener {
            drawView?.undo()
        }
        clearButton?.setOnClickListener {
            if (drawView?.edited == true) {
                drawView?.clear()
                drawView?.edited = false
                Toast.makeText(this, R.string.editor_drawing_cleared, Toast.LENGTH_SHORT).show()
            } else {
                onBackPressed()
            }
        }
        paletteButton?.setOnClickListener {
            showColorDialog(this)
        }
    }

    private fun onChangeSize(configuration: Configuration, size: Point) {
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                params.height = size.y
                params.width = size.y
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                params.height = size.x
                params.width = size.x
            }
        }

        params.gravity = Gravity.CENTER
        drawView?.layoutParams = params
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (imageUri == null) {
            try {
                val image = drawView?.getBitmap()
                val stream = FileOutputStream(pictureFile)
                image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            } catch (e: FileNotFoundException) {
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, R.string.pref_header_permission, Toast.LENGTH_SHORT).show()
            }
        } else {
            pictureFile = getRealPath(this, imageUri!!) ?: imageUri.toString()
        }

        outState.putString(noteDrawingURITAG, pictureFile)
    }

    private fun getBitmap(size: Point): Bitmap? {
        var imageBitmap: Bitmap? = null
        when {
            (pictureFile.isNotBlank()) -> {
                val option = BitmapFactory.Options()
                option.inMutable = true
                option.inPreferredConfig = Bitmap.Config.ARGB_8888
                imageBitmap = BitmapFactory.decodeFile(pictureFile, option)
            }
            (imageUri != null) -> {
                try {
                    val pathFile = getRealPath(this, imageUri!!)

                    val option = BitmapFactory.Options()
                    option.inMutable = true
                    option.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val selectedImage = BitmapFactory.decodeFile(pathFile, option)

                    val exifInterface = ExifInterface(pathFile)

                    val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)

                    val width = selectedImage.width
                    val height = selectedImage.height
                    val scaledWidth = size.x.toFloat() / width
                    val scaledHeight = size.x.toFloat() / height

                    val matrix = Matrix()
                    matrix.postScale(scaledWidth, scaledHeight)

                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_180 -> {
                            matrix.postRotate(180f)
                        }
                        ExifInterface.ORIENTATION_ROTATE_90 -> {
                            matrix.postRotate(90f)
                        }
                        ExifInterface.ORIENTATION_ROTATE_270 -> {
                            matrix.postRotate(270f)
                        }
                        else -> {
                            matrix.postRotate(0f)
                        }
                    }

                    imageBitmap = Bitmap.createBitmap(selectedImage!!, 0, 0, width, height, matrix, false)
                    selectedImage.recycle()

                } catch (e: Exception) {
                    Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
        return imageBitmap
    }

    private fun saveBitmap() {
        try {
            val image = drawView?.getBitmap()
            val stream = FileOutputStream(pictureFile)
            image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            if (position == -1) locations += File(pictureFile).path

            val returnIntent = Intent()
            returnIntent.putExtra(noteDrawingTAG, locations)
            setResult(Activity.RESULT_OK, returnIntent)
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
        if (position == -1) {
            val mediaFile = File(Utility.mediaStorageDir)
            if (!mediaFile.exists()) if (!mediaFile.mkdirs()) return null
            val timeStamp = SimpleDateFormat("ddMMyy_hhmmss", Locale.getDefault()).format(Date())
            val mImageName = "MI_$timeStamp.png"
            return File("${mediaFile.path}${File.separator}$mImageName")
        } else {
            return File(locations[position])
        }
    }

    private fun checkDrawingPermissions(context: Activity) {
        val writeExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val recordAudioResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)

        if (writeExternalStorage != PackageManager.PERMISSION_GRANTED || recordAudioResult != PackageManager.PERMISSION_GRANTED || readExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_EXTERNAL_STORAGE), EditorActivity.PERMISSION_REQUEST_DRAWING_CODE)
        }
    }

    @SuppressLint("InflateParams")
    private fun showColorDialog(context: Context) {
        var paintColor: Int = Color.BLACK
        val builder = AlertDialog.Builder(context)

        val colorArray = resources.getStringArray(R.array.color_options)
        val colorAdapter = ColorAdapter(context, *colorArray)

        val dialogView = layoutInflater
                .inflate(R.layout.color_selector, null)
        dialogView.setPadding(16, 16, 16, 16)

        builder.setView(dialogView)
        builder.setPositiveButton(R.string.done) { dialog, _ ->
            if (dialog != null) {
                drawView?.changePaint(paintColor)
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
            dialogInterface?.dismiss()
        }

        val items = dialogView.findViewById<GridView>(R.id.color_grid)

        items.choiceMode = GridView.CHOICE_MODE_SINGLE
        items.adapter = colorAdapter
        items.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ -> paintColor = Color.parseColor(colorArray[i]) }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            EditorActivity.PERMISSION_REQUEST_DRAWING_CODE -> {
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

        Utility.createDialog(this, getString(R.string.unsaved_changes_dialog_msg), getString(R.string.keep_editing), getString(R.string.discard), discardButtonClickListener)

    }

    override fun onDestroy() {
        drawView?.mBitmap?.recycle()
        drawView?.mBitmap = null
        super.onDestroy()
    }

    companion object {
        const val noteDrawingURITAG = "noteDrawingURI"
        const val noteDrawingIDTAG = "noteDrawingID"
    }
}
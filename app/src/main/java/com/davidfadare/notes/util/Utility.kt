package com.davidfadare.notes.util

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.CursorLoader
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.davidfadare.notes.BuildConfig
import com.davidfadare.notes.EditorActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.data.NoteViewModel
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.data.notedb.NoteDatabase.Companion.DATABASE_NAME
import com.davidfadare.notes.data.notedb.NoteDatabase.Companion.TABLE_NAME
import java.io.*
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class Utility {

    companion object {
        const val playUri: String = "https://play.google.com/store/apps/details?id="
        val mediaStorageDir = "${Environment.getExternalStorageDirectory()}${File.separator}Android${File.separator}data${File.separator}${BuildConfig.APPLICATION_ID}${File.separator}Files"

        val uri: Uri = Uri.parse("${Environment.getExternalStorageDirectory().path}${File.separator}${R.string.app_name}${File.separator}exports")

        val defaultProjection = arrayOf(Note.NoteEntry._ID, Note.NoteEntry.COLUMN_NOTE_NAME, Note.NoteEntry.COLUMN_NOTE_TEXT, Note.NoteEntry.COLUMN_NOTE_DATE, Note.NoteEntry.COLUMN_NOTE_COLOR, Note.NoteEntry.COLUMN_NOTE_PASSWORD, Note.NoteEntry.COLUMN_NOTE_ALARM_DATE, Note.NoteEntry.COLUMN_NOTE_PIN_STATUS, Note.NoteEntry.COLUMN_NOTE_DRAWING, Note.NoteEntry.COLUMN_NOTE_AUDIO)

        const val defaultSortOrder = Note.NoteEntry.COLUMN_NOTE_PIN_STATUS + " DESC, " + Note.NoteEntry.COLUMN_NOTE_DATE + " DESC"

        val defaultQuery: SupportSQLiteQueryBuilder = SupportSQLiteQueryBuilder.builder(TABLE_NAME)
                .columns(defaultProjection).orderBy(defaultSortOrder)

        fun changeWindowColor(context: Activity, window: Window?, color: Int = 0) {
            val noteColor = if (color == 0) {
                ContextCompat.getColor(context, R.color.blue_note)
            } else color

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = noteColor
            } else {
                changeTheme(noteColor, context)
            }
        }

        private fun changeTheme(color: Int, activity: Activity) {
            when (String.format("#%06X", 0xFFFFFF and color)) {
                "#F44336" -> activity.setTheme(R.style.RedTheme)
                "#FF9800" -> activity.setTheme(R.style.OrangeTheme)
                "#FFEB3B" -> activity.setTheme(R.style.YellowTheme)
                "#4CAF50" -> activity.setTheme(R.style.GreenTheme)
                "#2196F3" -> activity.setTheme(R.style.BlueTheme)
                "#9C27B0" -> activity.setTheme(R.style.PurpleTheme)
                "#E91E63" -> activity.setTheme(R.style.PinkTheme)
                "#E0E0E0" -> activity.setTheme(R.style.WhiteTheme)
                "#795548" -> activity.setTheme(R.style.BrownTheme)
                "#000000" -> activity.setTheme(R.style.BlackTheme)
                else -> activity.setTheme(R.style.BlueTheme)
            }
        }

        fun addBorder(bitmapImg: Bitmap, finalColor: Int): Bitmap {
            val borderSize = 20
            val bmpWithBorder = Bitmap.createBitmap(bitmapImg.width + borderSize *2, bitmapImg.height + borderSize *2, bitmapImg.config)
            val canvas = Canvas(bmpWithBorder)
            canvas.drawColor(finalColor)
            canvas.drawBitmap(bitmapImg, borderSize.toFloat(), borderSize.toFloat(), null)
            return bmpWithBorder
        }

        fun updateWidget(context: Context) {
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(ComponentName(context.applicationContext, WidgetProvider::class.java))
            intent.putExtra(WidgetProvider.WIDGET_IDS_KEY, ids)
            context.sendBroadcast(intent)
        }

        fun createDialog(context: Context, text: String?, positive_text: String?, negative_text: String?, discardButtonClickListener: DialogInterface.OnClickListener?, title: String = "", positiveButtonClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ -> dialog?.dismiss() }) {

            val builder = AlertDialog.Builder(context)
            val alertDialog: AlertDialog
            if (title.isNotEmpty()) builder.setTitle(title)
            builder.setMessage(text)
            builder.setPositiveButton(positive_text, positiveButtonClickListener)
            builder.setNegativeButton(negative_text, discardButtonClickListener)

            alertDialog = builder.create()
            alertDialog.show()
        }

        fun getListItemByPosition(pos: Int, listView: ListView): View {
            val firstListItemPosition = listView.firstVisiblePosition
            val lastListItemPosition = listView.lastVisiblePosition

            return if (pos < firstListItemPosition || pos > lastListItemPosition) {
                listView.adapter.getView(pos, null, listView)
            } else {
                val childIndex = pos - firstListItemPosition
                listView.getChildAt(childIndex)
            }
        }
    }

    object PermissionUtil {
        fun checkWritePermissions(context: Activity) {
            val writeExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (writeExternalStorage != PackageManager.PERMISSION_GRANTED || readExternalStorage != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE), EditorActivity.PERMISSION_REQUEST_CODE)
            }
        }

        fun checkAudioPermissions(context: Activity) {
            val writeExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readExternalStorage = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            val recordAudioResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)

            if (writeExternalStorage != PackageManager.PERMISSION_GRANTED || recordAudioResult != PackageManager.PERMISSION_GRANTED || readExternalStorage != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_EXTERNAL_STORAGE), EditorActivity.PERMISSION_REQUEST_DRAWING_CODE)
            }
        }
    }

    object ExportUtil {
        private fun copyFile(fromFile: FileInputStream, toFile: FileOutputStream) {
            var fromChannel: FileChannel? = null
            var toChannel: FileChannel? = null
            try {
                fromChannel = fromFile.channel
                toChannel = toFile.channel
                fromChannel!!.transferTo(0, fromChannel.size(), toChannel)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    fromChannel?.close()
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }

                try {
                    toChannel?.close()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }

            }
        }

        @Suppress("DEPRECATION")
        fun copyText(vararg cursor: Note, toFile: FileOutputStream) {
            var writer: OutputStreamWriter? = null
            try {
                writer = OutputStreamWriter(toFile)

                if (cursor.isNotEmpty()) {
                    for (note in cursor) {
                        var textString = note.text
                        textString = textString?.replace("&nbsp;".toRegex(), " ")
                        textString = textString?.replace("\n".toRegex(), "<br>")

                        textString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(textString, Html.FROM_HTML_MODE_LEGACY).toString()
                        } else {
                            Html.fromHtml(textString).toString()
                        }

                        val dateYear = SimpleDateFormat("dd MMM yyyy", Locale.US)
                        val date = dateYear.format(note.noteDate)

                        writer.write("$date \n\r ${note.title} \n\r $textString \n\r")
                        for (j in 0..20) {
                            writer.write("-")
                        }
                        writer.write("\n\r")
                    }
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (f: IOException) {
                f.printStackTrace()
            } catch (g: IndexOutOfBoundsException) {
                g.printStackTrace()
            } finally {
                try {
                    writer?.close()
                } catch (h: IOException) {
                    h.printStackTrace()
                }

            }
        }

        @Suppress("DEPRECATION")
        fun cursorToText(context: FragmentActivity): String {
            val noteModel = ViewModelProviders.of(context).get(NoteViewModel::class.java)

            val notes = noteModel.mDatabaseRepository.queryOrderBy(defaultQuery.create())

            var fileContent = ""

            try {
                for (note in notes) {
                    val nameString = note.title
                    var textString = note.text ?: ""

                    textString = textString.replace("&nbsp;".toRegex(), " ")
                    textString = textString.replace("\n".toRegex(), "<br>")
                    textString = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        -> {
                            Html.fromHtml(textString, Html.FROM_HTML_MODE_LEGACY).toString()
                        }
                        else -> {
                            Html.fromHtml(textString).toString()
                        }
                    }

                    val noteDate = note.noteDate
                    val dateYear = SimpleDateFormat("dd MMM yyyy", Locale.US)
                    val date = dateYear.format(noteDate)

                    fileContent += "$date\n\r$nameString\n\r$textString\n\r"
                    for (j in 0..20) {
                        fileContent += "-"
                    }
                    fileContent += "\n\r"
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (g: IndexOutOfBoundsException) {
                g.printStackTrace()
            }

            return fileContent
        }

        fun textToCursor(context: FragmentActivity, name: String, text: String) {
            val noteModel = ViewModelProviders.of(context).get(NoteViewModel::class.java)


            val newText = text.replace("\n".toRegex(), "\n\r").trim()
            val note = Note(name, newText, System.currentTimeMillis(), ContextCompat.getColor(context, R.color.blue_note), 0)

            val newID = noteModel.mDatabaseRepository.insert(note)

            if (newID == -1L) {
                Toast.makeText(context, context.getString(R.string.pref_note_import_failed_compatible), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.pref_note_import_successful), Toast.LENGTH_SHORT).show()
            }
            updateWidget(context)
        }

        fun getDeviceName(): String {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                capitalize(model)
            } else {
                capitalize("$manufacturer $model")
            }
        }

        private fun capitalize(str: String): String {
            if (TextUtils.isEmpty(str)) return str
            val arr = str.toCharArray()
            var capNext = true

            val phrase = StringBuilder()
            for (c in arr) {
                if (capNext && Character.isLetter(c)) {
                    phrase.append(Character.toUpperCase(c))
                    capNext = false
                    continue
                } else if (Character.isWhitespace(c)) {
                    capNext = true
                }
                phrase.append(c)
            }
            return phrase.toString()
        }

        @Throws(IOException::class)
        fun importDatabase(dbPath: String, context: FragmentActivity, override: Boolean): Boolean {
            val newDB = File(dbPath)
            val oldDbB = File(context.getDatabasePath(DATABASE_NAME).absolutePath)
            val noteModel = ViewModelProviders.of(context).get(NoteViewModel::class.java)

            if (newDB.exists()) {
                if (dbPath.endsWith(".db")) {
                    if (override) {
                        copyFile(FileInputStream(newDB), FileOutputStream(oldDbB))

                        val i = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(i)
                    } else {
                        val database = SQLiteDatabase.openOrCreateDatabase(newDB.absolutePath, null, null)

                        try {
                            val cursor = database.query(TABLE_NAME, defaultProjection, null, null, null, null, defaultSortOrder)

                            if (cursor != null) {
                                val noteColor = ContextCompat.getColor(context, R.color.blue_note)
                                val nameColumnIndex = cursor.getColumnIndex(Note.NoteEntry.COLUMN_NOTE_NAME)
                                val textColumnIndex = cursor.getColumnIndex(Note.NoteEntry.COLUMN_NOTE_TEXT)
                                val dateColumnIndex = cursor.getColumnIndex(Note.NoteEntry.COLUMN_NOTE_DATE)
                                val pinColumnIndex = cursor.getColumnIndex(Note.NoteEntry.COLUMN_NOTE_PIN_STATUS)
                                while (cursor.moveToNext()) {
                                    val note = Note(cursor.getString(nameColumnIndex), cursor.getString(textColumnIndex), cursor.getLong(dateColumnIndex), noteColor, cursor.getInt(pinColumnIndex))

                                    val id = noteModel.mDatabaseRepository.insert(note)
                                    if (id.toInt() == -1) {
                                        Log.e(context.packageName, "Failed to insert row for $id")
                                    }
                                }
                                Toast.makeText(context, context.getString(R.string.pref_note_import_successful), Toast.LENGTH_SHORT).show()
                                updateWidget(context)
                            } else {
                                Toast.makeText(context, context.getString(R.string.pref_note_import_failed_compatible), Toast.LENGTH_LONG).show()
                            }
                            cursor?.close()
                        } catch (e: SQLiteException) {
                            Toast.makeText(context, context.getString(R.string.pref_note_import_failed_file_type), Toast.LENGTH_LONG).show()
                            return false
                        }
                    }
                } else if (dbPath.endsWith(".txt")) {
                    val name = String.format(context.getString(R.string.pref_note_import_successful_from), Uri.parse(dbPath).lastPathSegment)
                    var text = ""

                    BufferedReader(FileReader(newDB)).use { r ->
                        r.lineSequence().forEach {
                            text += "$it\n\r"
                        }
                    }

                    val noteColor = ContextCompat.getColor(context, R.color.blue_note)

                    val note = Note(name, text, System.currentTimeMillis(), noteColor, 0)

                    val id = noteModel.mDatabaseRepository.insert(note)
                    if (id.toInt() == -1) {
                        Log.e(context.packageName, "Failed to insert row for $id")
                    } else {
                        Toast.makeText(context, context.getString(R.string.pref_note_import_successful), Toast.LENGTH_SHORT).show()
                    }
                    updateWidget(context)
                } else {
                    Toast.makeText(context, context.getString(R.string.pref_note_import_failed_file_type_2), Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            } else {
                Toast.makeText(context, context.getString(R.string.pref_note_import_failed_file_location), Toast.LENGTH_LONG).show()
            }
            return false
        }

        @Throws(IOException::class)
        fun exportDatabase(name: String, context: FragmentActivity, area: String): String? {
            val inFileName = context.getDatabasePath(DATABASE_NAME).path
            var result = ""

            val sd = Environment.getExternalStorageDirectory()
            val dir = File("${sd.absolutePath}${File.separator}Stylepad${File.separator}$area${File.separator}")

            val folderName = when {
                (name.endsWith(".txt")) -> {
                    name.removeSuffix(".txt")
                }
                (name.endsWith(".db")) -> {
                    name.removeSuffix(".db")
                }
                else -> {
                    Calendar.getInstance().time.toString()
                }
            }

            val folder = File("${sd.absolutePath}${File.separator}Stylepad${File.separator}$area${File.separator}$folderName${File.separator}")

            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!folder.exists()) folder.mkdirs()

            val noteModel = ViewModelProviders.of(context).get(NoteViewModel::class.java)

            val dbFile = File(inFileName)
            val outFile = File(folder, name)

            if (folder.exists() && sd.canWrite() && dbFile.exists()) {
                result = outFile.toString()

                val notes = noteModel.mDatabaseRepository.queryOrderBy(defaultQuery.create())

                if (name.endsWith(".txt")) {
                    copyText(*notes.toTypedArray(), toFile = FileOutputStream(outFile))
                } else if (name.endsWith(".db")) {
                    copyFile(FileInputStream(dbFile), FileOutputStream(outFile))
                }

                if (notes.isNotEmpty()) {
                    for (note in notes) {
                        exportLocations(note, folder)
                    }
                }
            }

            if (result.isEmpty()) result = context.resources.getString(R.string.pref_header_permission)

            return result
        }

        fun exportNote(name: String, context: FragmentActivity, id: Long): String? {
            val inFileName = context.getDatabasePath(DATABASE_NAME).path
            var result: String? = null

            val sd = Environment.getExternalStorageDirectory()
            val dir = File("${sd.absolutePath}${File.separator}Stylepad${File.separator}deleted${File.separator}")

            val folderName = when {
                name.endsWith(".txt") -> {
                    name.removeSuffix(".txt")
                }
                name.endsWith(".db") -> {
                    name.removeSuffix(".db")
                }
                else ->
                    Calendar.getInstance().time.toString()
            }

            val folder = File("${sd.absolutePath}${File.separator}Stylepad${File.separator}deleted${File.separator}$folderName${File.separator}")

            if (!dir.exists()) dir.mkdirs()
            if (!folder.exists()) folder.mkdirs()

            val dbFile = File(inFileName)
            val outFile = File(folder, name)

            if (dir.exists() && sd.canWrite() && dbFile.exists()) {
                result = outFile.toString()

                val noteModel = ViewModelProviders.of(context).get(NoteViewModel::class.java)
                val notes = noteModel.mDatabaseRepository.queryByID(id)
                val note = notes.value

                if (note != null) {
                    if (name.endsWith(".txt")) {
                        copyText(note, toFile = FileOutputStream(outFile))
                    } else if (name.endsWith(".db")) {
                        copyFile(FileInputStream(dbFile), FileOutputStream(outFile))
                    }

                    exportLocations(note, folder)
                }
            }

            return result
        }

        private fun exportLocations(database: Note, folder: File) {
            val drawingLocations = ByteArrayUtil.convertByteToString(database.images)
            for (string in drawingLocations) {
                val file = File(string)
                if (file.exists()) {
                    copyFile(FileInputStream(file), FileOutputStream(File(folder, string.substring(string.lastIndexOf(File.separator) + 1))))
                }
            }
            val audioLocations = ByteArrayUtil.convertByteToString(database.audio)
            for (string in audioLocations) {
                val file = File(string)
                if (file.exists()) {
                    copyFile(FileInputStream(file), FileOutputStream(File(folder, string.substring(string.lastIndexOf(File.separator) + 1))))
                }
            }
        }
    }

    object ByteArrayUtil {
        fun convertStringToByte(strings: Array<String>): ByteArray {
            val string = StringBuilder()
            for (str in strings) {
                string.append("$str|||")
            }
            return string.toString().toByteArray(Charset.defaultCharset())
        }

        fun convertByteToString(bytes: ByteArray?): Array<String> {
            return if (bytes != null) {
                val strings = String(bytes, Charset.defaultCharset())
                val list = strings.split("|||").toMutableList()
                list.removeAll(Collections.singleton(""))
                list.removeAll(Collections.singleton("|||"))
                list.toTypedArray()
            } else Array(0) { "" }
        }
    }

    object RealPathUtil {
        fun getRealPath(context: Context, fileUri: Uri): String? {
            return when (Build.VERSION.SDK_INT < 19) {
                true -> getRealPathFromURI_API11to18(context, fileUri)
                false -> getRealPathFromURI_API19(context, fileUri)
            }
        }

        @SuppressLint("NewApi")
        fun getRealPathFromURI_API11to18(context: Context, contentUri: Uri): String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var result: String? = null

            val cursorLoader = CursorLoader(context, contentUri, projection, null, null, null)
            val cursor = cursorLoader.loadInBackground()

            if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                result = cursor.getString(columnIndex)
                cursor.close()
            }
            return result
        }

        @SuppressLint("NewApi")
        fun getRealPathFromURI_API19(context: Context, uri: Uri): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                } else if (isDownloadsDocument(uri)) {

                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))

                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    val contentUri: Uri = if ("image" == type) {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        private fun getDataColumn(context: Context, uri: Uri, selection: String?,
                                  selectionArgs: Array<String>?): String? {

            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)

            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

    }
}

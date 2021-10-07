package com.davidfadare.notes.data.notedb

import android.net.Uri
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_ALARM_DATE
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_AUDIO
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_COLOR
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_DATE
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_DRAWING
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_NAME
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_PASSWORD
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_PIN_STATUS
import com.davidfadare.notes.data.notedb.Note.NoteEntry.COLUMN_NOTE_TEXT
import com.davidfadare.notes.data.notedb.Note.NoteEntry._ID
import com.davidfadare.notes.data.notedb.NoteDatabase.Companion.TABLE_NAME
import java.io.Serializable

@Entity(tableName = TABLE_NAME)
data class Note(@PrimaryKey(autoGenerate = true)
                @ColumnInfo(name = _ID)
                @NonNull
                val id: Int,

                @ColumnInfo(name = COLUMN_NOTE_NAME)
                @NonNull
                var title: String,

                @ColumnInfo(name = COLUMN_NOTE_TEXT)
                var text: String?,

                @ColumnInfo(name = COLUMN_NOTE_DATE)
                @NonNull
                var noteDate: Long = 0,

                @ColumnInfo(name = COLUMN_NOTE_COLOR)
                @NonNull
                var noteColor: Int,

                @ColumnInfo(name = COLUMN_NOTE_PASSWORD)
                var password: String?,

                @ColumnInfo(name = COLUMN_NOTE_ALARM_DATE)
                @NonNull
                var alarmDate: Long = 0,

                @ColumnInfo(name = COLUMN_NOTE_AUDIO, typeAffinity = ColumnInfo.BLOB)
                var audio: ByteArray?,

                @ColumnInfo(name = COLUMN_NOTE_DRAWING, typeAffinity = ColumnInfo.BLOB)
                var images: ByteArray?,

                @ColumnInfo(name = COLUMN_NOTE_PIN_STATUS)
                @NonNull
                var pinStatus: Int = 0): Serializable {

    @Ignore
    constructor(title: String, text: String?, noteDate: Long = 0, noteColor: Int, password: String?,alarmDate: Long = 0, audio: ByteArray?, images: ByteArray?, pinStatus: Int = 0) : this(0, title, text, noteDate, noteColor, password, alarmDate, audio, images, pinStatus)

    @Ignore
    constructor(title: String, text: String?, noteDate: Long = 0, noteColor: Int, pinStatus: Int = 0) : this(0, title, text, noteDate, noteColor, "", 0, null, null, pinStatus)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    object NoteEntry: Serializable {
        private const val CONTENT_AUTHORITY = "com.davidfadare.notes"
        private const val PATH_NOTES = "notes"

        private val BASE_CONTENT_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

        val CONTENT_URI: Uri = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NOTES)

        const val _ID = "_id"
        const val COLUMN_NOTE_NAME = "name"
        const val COLUMN_NOTE_TEXT = "text"
        const val COLUMN_NOTE_DATE = "date"
        const val COLUMN_NOTE_COLOR = "color"
        const val COLUMN_NOTE_PASSWORD = "password"
        const val COLUMN_NOTE_ALARM_DATE = "alarm_date"
        const val COLUMN_NOTE_AUDIO = "audio"
        const val COLUMN_NOTE_DRAWING = "drawing"
        const val COLUMN_NOTE_PIN_STATUS = "pin"
    }
}


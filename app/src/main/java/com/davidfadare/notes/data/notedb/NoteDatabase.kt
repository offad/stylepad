package com.davidfadare.notes.data.notedb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
        entities = [
            Note::class
        ],
        version = 2
)

abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "notepad.db"
        const val TABLE_NAME = "notes"

        @Volatile
        private var INSTANCE: NoteDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(SQL_CREATE_NOTES_TABLE)
                database.execSQL(SQL_INSERT_NOTES_TABLE)
                database.execSQL("DROP TABLE $TABLE_NAME")
                database.execSQL("ALTER TABLE ${TABLE_NAME}_new RENAME TO $TABLE_NAME")
            }
        }

        fun getInstance(context: Context): NoteDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: buildDatabase(context.applicationContext).also {
                                INSTANCE = it
                            }
                }

        private fun buildDatabase(appContext: Context): NoteDatabase {
            return Room.databaseBuilder(appContext, NoteDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
        }

        const val SQL_CREATE_NOTES_TABLE = ("CREATE TABLE ${TABLE_NAME}_new" +
                " (${Note.NoteEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "${Note.NoteEntry.COLUMN_NOTE_NAME} TEXT NOT NULL, " +
                "${Note.NoteEntry.COLUMN_NOTE_TEXT} TEXT, " +
                "${Note.NoteEntry.COLUMN_NOTE_DATE} INTEGER NOT NULL, " +
                "${Note.NoteEntry.COLUMN_NOTE_COLOR} INTEGER NOT NULL, " +
                "${Note.NoteEntry.COLUMN_NOTE_PASSWORD} TEXT, " +
                "${Note.NoteEntry.COLUMN_NOTE_ALARM_DATE} INTEGER NOT NULL, " +
                "${Note.NoteEntry.COLUMN_NOTE_AUDIO} BLOB, " +
                "${Note.NoteEntry.COLUMN_NOTE_DRAWING} BLOB, " +
                "${Note.NoteEntry.COLUMN_NOTE_PIN_STATUS} INTEGER NOT NULL)")

        const val SQL_INSERT_NOTES_TABLE = ("INSERT INTO ${TABLE_NAME}_new " +
                "(${Note.NoteEntry._ID}, ${Note.NoteEntry.COLUMN_NOTE_NAME}, ${Note.NoteEntry.COLUMN_NOTE_TEXT}, ${Note.NoteEntry.COLUMN_NOTE_DATE}, ${Note.NoteEntry.COLUMN_NOTE_COLOR}, ${Note.NoteEntry.COLUMN_NOTE_PASSWORD}, ${Note.NoteEntry.COLUMN_NOTE_ALARM_DATE}, ${Note.NoteEntry.COLUMN_NOTE_AUDIO}, ${Note.NoteEntry.COLUMN_NOTE_DRAWING}, ${Note.NoteEntry.COLUMN_NOTE_PIN_STATUS}) " +
                "SELECT ${Note.NoteEntry._ID}, ${Note.NoteEntry.COLUMN_NOTE_NAME}, ${Note.NoteEntry.COLUMN_NOTE_TEXT}, ${Note.NoteEntry.COLUMN_NOTE_DATE}, ${Note.NoteEntry.COLUMN_NOTE_COLOR}, ${Note.NoteEntry.COLUMN_NOTE_PASSWORD}, ${Note.NoteEntry.COLUMN_NOTE_ALARM_DATE}, ${Note.NoteEntry.COLUMN_NOTE_AUDIO}, ${Note.NoteEntry.COLUMN_NOTE_DRAWING}, ${Note.NoteEntry.COLUMN_NOTE_PIN_STATUS} FROM $TABLE_NAME")
    }

}

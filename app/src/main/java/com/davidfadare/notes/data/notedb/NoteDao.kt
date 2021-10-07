/**
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.davidfadare.notes.data.notedb

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.davidfadare.notes.data.notedb.NoteDatabase.Companion.TABLE_NAME

@Dao
interface NoteDao {

    @Insert
    fun insert(note: Note): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(note: Note): Int

    @Query("UPDATE $TABLE_NAME SET ${Note.NoteEntry.COLUMN_NOTE_NAME} = :title WHERE ${Note.NoteEntry._ID} = :id")
    fun update(title: String, id: Long)

    @Query("SELECT * FROM $TABLE_NAME")
    fun getNotes(): List<Note>

    @Query("SELECT * FROM $TABLE_NAME WHERE ${Note.NoteEntry._ID} = :id LIMIT 1")
    fun getNoteWithId(id: Long): LiveData<Note>

    @RawQuery
    fun getNotesOrderBy(query: SupportSQLiteQuery): List<Note>

    @Query("SELECT * FROM $TABLE_NAME WHERE ${Note.NoteEntry.COLUMN_NOTE_NAME} LIKE :title OR ${Note.NoteEntry.COLUMN_NOTE_TEXT} LIKE :text ORDER BY ${Note.NoteEntry.COLUMN_NOTE_PIN_STATUS} DESC, ${Note.NoteEntry.COLUMN_NOTE_DATE} DESC")
    fun getNotesWithText(title: String, text: String): List<Note>

    @Delete
    fun delete(note: Note): Int

    @Query("DELETE FROM $TABLE_NAME WHERE ${Note.NoteEntry._ID} = :id")
    fun delete(id: Long): Int

    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll(): Int

    @Transaction
    fun insert(vararg notes: Note) {
        notes.forEach {
            insert(it)
        }
    }
}
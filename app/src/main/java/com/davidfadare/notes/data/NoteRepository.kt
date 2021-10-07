package com.davidfadare.notes.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteQuery
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.data.notedb.NoteDao
import com.davidfadare.notes.data.notedb.NoteDatabase
import com.davidfadare.notes.util.Utility.Companion.defaultQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NoteRepository private constructor(private val application: Application) {

    private val localNoteCache = NoteDatabase.getInstance(application)
    private val noteDao: NoteDao = localNoteCache.noteDao()
    private val viewModelScope = CoroutineScope(Job() + Dispatchers.IO)

    val notesLiveData: MutableLiveData<List<Note>> by lazy {
        val notes: MutableLiveData<List<Note>> = MutableLiveData()
        viewModelScope.launch {
                notes.postValue(noteDao.getNotesOrderBy(defaultQuery.create()))}
        notes
    }

    fun query(): List<Note> {
        return QueryAsyncTask(noteDao).execute().get()
    }

    fun queryByID(id: Long): LiveData<Note> {
        return IDAsyncTask(noteDao).execute(id).get()
    }

    fun queryOrderBy(query: SupportSQLiteQuery): List<Note> {
        val notes = OrderAsyncTask(noteDao).execute(query).get()
        viewModelScope.launch {
            notesLiveData.postValue(notes)}
        return notes
    }

    fun queryOrderByText(text: String): List<Note> {
        val notes = SearchAsyncTask(noteDao).execute(text).get()
        viewModelScope.launch {
            notesLiveData.postValue(notes)}
        return notes
    }

    fun insert(note: Note): Long {
        val id = InsertAsyncTask(noteDao).execute(note).get()
        viewModelScope.launch {
            notesLiveData.postValue(queryOrderBy(defaultQuery.create()))}
        return id
    }

    fun update(vararg note: Note): Int {
        val affected = UpdateAsyncTask(noteDao).execute(*note).get()
        viewModelScope.launch {
            notesLiveData.postValue(queryOrderBy(defaultQuery.create()))}
        return affected
    }

    fun delete(note: Note): Int {
        val affected = DeleteAsyncTask(noteDao).execute(note).get()
        viewModelScope.launch {
            notesLiveData.postValue(queryOrderBy(defaultQuery.create()))}
        return affected
    }

    fun deleteAll(vararg note: Note): Int {
        val affected = DeleteAsyncTask(noteDao).execute(*note).get()
        viewModelScope.launch {
            notesLiveData.postValue(queryOrderBy(defaultQuery.create()))}
        return affected
    }

    private class QueryAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<Void, Void, List<Note>>() {

        override fun doInBackground(vararg params: Void): List<Note> {
            return mAsyncTaskDao.getNotes()
        }

    }

    private class IDAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<Long, Void, LiveData<Note>>() {

        override fun doInBackground(vararg params: Long?): LiveData<Note> {
            return mAsyncTaskDao.getNoteWithId(params[0] ?: 0)
        }

    }

    private class SearchAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<String, Void, List<Note>>() {

        override fun doInBackground(vararg params: String): List<Note> {
            return mAsyncTaskDao.getNotesWithText(params[0], params[0])
        }

    }

    private class OrderAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<SupportSQLiteQuery, Void, List<Note>>() {

        override fun doInBackground(vararg params: SupportSQLiteQuery): List<Note> {
            return mAsyncTaskDao.getNotesOrderBy(params[0])
        }

    }

    private class InsertAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<Note, Void, Long>() {

        override fun doInBackground(vararg params: Note): Long? {
            return mAsyncTaskDao.insert(params[0])
        }

    }

    private class UpdateAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<Note, Void, Int>() {

        override fun doInBackground(vararg params: Note): Int? {
            var affected = 0
            for(note in params){
                affected += mAsyncTaskDao.update(note)
            }
            return affected
        }
    }

    private class DeleteAsyncTask internal constructor(private val mAsyncTaskDao: NoteDao) : AsyncTask<Note, Void, Int>() {

        override fun doInBackground(vararg params: Note): Int? {
            var affected = 0
            params.forEach {
                affected += mAsyncTaskDao.delete(it)
            }
            return affected
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: NoteRepository? = null

        fun getInstance(application: Application): NoteRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: NoteRepository(application)
                                    .also { INSTANCE = it }
                }
    }
}

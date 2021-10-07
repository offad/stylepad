package com.davidfadare.notes.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.davidfadare.notes.data.notedb.Note

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    val notesLiveData: LiveData<List<Note>>

    val notePosition = MutableLiveData<Long>()
    val mDatabaseRepository: NoteRepository = NoteRepository.getInstance(application)

    init {
        notesLiveData = mDatabaseRepository.notesLiveData
    }

}
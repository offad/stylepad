package com.davidfadare.notes.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.davidfadare.notes.data.notedb.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class ChangeNoteViewModel(application: Application) : AndroidViewModel(application) {

    private val viewModelScope = CoroutineScope(Job() + Dispatchers.Main)

    val data = MutableLiveData<Uri>()
    val mDatabaseRepository = NoteRepository.getInstance(application)

    val noteTitle = MutableLiveData<String>()
    val noteText = MutableLiveData<String?>()
    val notePassword = MutableLiveData<String?>()
    val pinned = MutableLiveData<Int>()
    val noteColor = MutableLiveData<Int>()
    val noteAlarm = MutableLiveData<Long>()
    val noteDrawing = MutableLiveData<ByteArray>()
    val noteAudio = MutableLiveData<ByteArray>()

    var removeAlarm = MutableLiveData<Boolean>()
    var mNoteHasChanged = MutableLiveData<Boolean>()

    var initialised = false

    fun setupNote(notesLiveData: Note) {
        noteTitle.postValue(notesLiveData.title)
        noteText.postValue(notesLiveData.text)
        notePassword.postValue(notesLiveData.password)
        pinned.postValue(notesLiveData.pinStatus)
        noteColor.postValue(notesLiveData.noteColor)
        noteAlarm.postValue(notesLiveData.alarmDate)
        noteDrawing.postValue(notesLiveData.images)
        noteAudio.postValue(notesLiveData.audio)

        initialised = true
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }

}
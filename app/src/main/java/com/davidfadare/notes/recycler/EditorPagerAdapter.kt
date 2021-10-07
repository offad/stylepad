package com.davidfadare.notes.recycler

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.data.ChangeNoteViewModel
import com.davidfadare.notes.fragment.AudioPagerFragment
import com.davidfadare.notes.fragment.DrawingPagerFragment
import com.davidfadare.notes.fragment.EditorPagerFragment
import com.davidfadare.notes.fragment.InfoPagerFragment
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertByteToString


class EditorPagerAdapter internal constructor(fm: FragmentManager, context: FragmentActivity) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var noteTitle = ""
    private var noteText = ""
    private var notePassword = ""
    private var pinned = 0
    private var noteColor = 0
    private var noteAlarm = 0L
    private var noteDate = 0L
    var noteDrawingData: Array<String> = emptyArray()
    var noteAudioData: Array<String> = emptyArray()

    private var noteModel: ChangeNoteViewModel

    private val details: String
    private val info: String
    private val drawing: String
    private val audio: String

    var editorPagerFragment: EditorPagerFragment? = null
    var infoPagerFragment: InfoPagerFragment? = null
    var drawingPagerFragment: DrawingPagerFragment? = null
    var audioPagerFragment: AudioPagerFragment? = null

    init {
        context.let {
            noteModel = ViewModelProviders.of(it).get(ChangeNoteViewModel::class.java)
        }
        details = context.getString(com.davidfadare.notes.R.string.editor_insert_note_details)
        info = context.getString(com.davidfadare.notes.R.string.editor_insert_note_info)
        drawing = context.getString(com.davidfadare.notes.R.string.editor_insert_note_drawing)
        audio = context.getString(com.davidfadare.notes.R.string.editor_insert_note_audio)
        onSetupViewModel(context)
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> EditorPagerFragment.newInstance(noteTitle, noteText)
            1 -> InfoPagerFragment.newInstance(notePassword, noteAlarm, noteDate, pinned)
            2 -> DrawingPagerFragment.newInstance(noteDrawingData, noteColor)
            3 -> AudioPagerFragment.newInstance(noteAudioData, noteColor)
            else -> EditorPagerFragment.newInstance(noteTitle, noteText)
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        if (`object` is EditorPagerFragment) {
            `object`.notifyChange(noteTitle, noteText)
        } else if (`object` is InfoPagerFragment) {
            `object`.notifyChange(notePassword, noteAlarm, noteDate, pinned)
        } else if (`object` is DrawingPagerFragment) {
            `object`.notifyChange(noteDrawingData, noteColor)
        } else if (`object` is AudioPagerFragment) {
            `object`.notifyChange(noteAudioData, noteColor)
        }

        return super.getItemPosition(`object`)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val createdFragment = super.instantiateItem(container, position)
        when (position) {
            0 -> editorPagerFragment = createdFragment as EditorPagerFragment
            1 -> infoPagerFragment = createdFragment as InfoPagerFragment
            2 -> drawingPagerFragment = createdFragment as DrawingPagerFragment
            3 -> audioPagerFragment = createdFragment as AudioPagerFragment
        }
        return createdFragment
    }

    override fun getCount(): Int {
        return NUM_PAGES
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> details
            1 -> info
            2 -> drawing
            3 -> audio
            else -> null
        }
    }

    private fun onSetupViewModel(context: FragmentActivity) {
        noteModel.noteTitle.observe(context, Observer {
            it?.let {
                noteTitle = it
            }
        })
        noteModel.noteText.observe(context, Observer {
            it?.let {
                noteText = it
            }
        })
        noteModel.noteColor.observe(context, Observer {
            it?.let {
                noteColor = it
            }
        })
        noteModel.notePassword.observe(context, Observer {
            it?.let {
                notePassword = it
            }
        })
        noteModel.noteAlarm.observe(context, Observer {
            it?.let {
                noteAlarm = it
            }
        })
        noteModel.noteAudio.observe(context, Observer {
            it?.let {
                noteAudioData = convertByteToString(it)
            }
        })
        noteModel.noteDrawing.observe(context, Observer {
            it?.let {
                noteDrawingData = convertByteToString(it)
            }
        })
        noteModel.pinned.observe(context, Observer {
            it?.let {
                pinned = it
            }
        })
    }

    companion object {

        private const val NUM_PAGES = 4

    }
}

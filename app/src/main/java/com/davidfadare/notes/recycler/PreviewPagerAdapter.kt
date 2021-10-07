package com.davidfadare.notes.recycler

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.davidfadare.notes.R
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.fragment.AudioPreviewPagerFragment
import com.davidfadare.notes.fragment.DrawingPreviewPagerFragment
import com.davidfadare.notes.fragment.EditorPreviewPagerFragment
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertByteToString

class PreviewPagerAdapter internal constructor(fm: FragmentManager, cursor: Note?, context: Context) : FragmentStatePagerAdapter(fm) {

    private var mTitle: String = ""
    private var mText: String = ""
    private var mDrawingData: Array<String> = emptyArray()
    private var mAudioData: Array<String> = emptyArray()
    private var mNoteColor: Int = 0

    private val details: String
    private val drawing: String
    private val audio: String

    init {
        details = context.getString(R.string.editor_insert_note_details)
        drawing = context.getString(R.string.editor_insert_note_drawing)
        audio = context.getString(R.string.editor_insert_note_audio)
        if(cursor != null) setupCursor(cursor)
    }

    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> return EditorPreviewPagerFragment.newInstance(mTitle, mText)
            1 -> return DrawingPreviewPagerFragment.newInstance(mDrawingData, mNoteColor)
            2 -> return AudioPreviewPagerFragment.newInstance(mAudioData, mNoteColor)
            else -> return EditorPreviewPagerFragment.newInstance(mTitle, mText)
        }
    }

    override fun getCount(): Int {
        return NUM_PAGES
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> return details
            1 -> return drawing
            2 -> return audio
            else -> return null
        }
    }

    private fun setupCursor(cursor: Note) {
        mTitle = cursor.title
        mText = cursor.text ?: ""
        mDrawingData = convertByteToString(cursor.images)
        mAudioData = convertByteToString(cursor.audio)
        mNoteColor = cursor.noteColor
    }

    companion object {

        private const val NUM_PAGES = 3
    }
}

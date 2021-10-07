package com.davidfadare.notes.fragment

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.davidfadare.notes.R
import com.davidfadare.notes.recycler.AudioAdapter

class AudioPreviewPagerFragment : Fragment() {

    var data: Array<String>? = emptyArray()
    var noteColor: Int = 0
    private var listView: ListView? = null
    private var audioAdapter: AudioAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = arguments?.getStringArray("someAudio")
        noteColor = arguments?.getInt("noteColor") ?: 0
        if (noteColor == 0 && context != null) {
            noteColor = ContextCompat.getColor(context!!, R.color.blue_note)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_preview_audio, container, false) as ViewGroup

        val emptyView = rootView.findViewById<RelativeLayout>(R.id.empty_view)
        val emptyViewImage = rootView.findViewById<ImageView>(R.id.empty_note_image)
        emptyViewImage.drawable.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN)

        listView = rootView.findViewById(R.id.audio_list)
        listView?.emptyView = emptyView
        listView?.choiceMode = ListView.CHOICE_MODE_SINGLE
        if (data != null) {
            audioAdapter = AudioAdapter(context!!, noteColor, *data!!)
            listView?.adapter = audioAdapter
        }

        return rootView
    }

    override fun onPause() {
        audioAdapter?.stopAudio()
        super.onPause()
    }

    companion object {
        fun newInstance(text: Array<String>, color: Int): AudioPreviewPagerFragment {
            val fragment = AudioPreviewPagerFragment()
            val args = Bundle()
            args.putStringArray("someAudio", text)
            args.putInt("noteColor", color)
            fragment.arguments = args
            return fragment
        }
    }
}

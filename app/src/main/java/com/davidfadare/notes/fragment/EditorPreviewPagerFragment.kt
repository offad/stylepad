package com.davidfadare.notes.fragment

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.davidfadare.notes.R

class EditorPreviewPagerFragment : Fragment() {

    var title: String? = null
    var text: String? = null

    private lateinit var titleText: TextView
    private lateinit var textText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString("someTitle")
        text = arguments?.getString("someText")
        arguments?.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_preview_details, container, false) as ViewGroup

        titleText = rootView.findViewById(R.id.noteFragmentTitle)
        textText = rootView.findViewById(R.id.noteFragmentText)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        titleText.text = title
        textText.text = if (!TextUtils.isEmpty(text)) {
            text = text!!.replace("\n".toRegex(), "<br>")
            text = text!!.replace("&nbsp;".toRegex(), " ")
            Html.fromHtml(text)
        } else ""
    }

    companion object {
        fun newInstance(title: String?, text: String?): EditorPreviewPagerFragment {
            val fragment = EditorPreviewPagerFragment()
            val args = Bundle()
            args.putString("someTitle", title)
            args.putString("someText", text)
            fragment.arguments = args
            return fragment
        }
    }
}

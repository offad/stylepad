package com.davidfadare.notes.fragment

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.R
import com.davidfadare.notes.data.ChangeNoteViewModel

class EditorPagerFragment : Fragment() {

    private lateinit var noteModel: ChangeNoteViewModel

    var title: String? = null
    var text: String? = null

    private var mNameEditText: EditText? = null
    private var mTextEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString("someTitle")
        text = arguments?.getString("someText")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_note_details, container, false) as ViewGroup

        mNameEditText = rootView.findViewById(R.id.title_text)
        mTextEditText = rootView.findViewById(R.id.the_notes_text)

        if (!TextUtils.isEmpty(title)) {
            if (title == getString(R.string.editor_insert_title_empty)) {
                title = ""
            }
        }

        mNameEditText!!.setText(title)

        if (!TextUtils.isEmpty(text)) {
            text = text!!.replace("\n".toRegex(), "<br>")
            text = text!!.replace("&nbsp;".toRegex(), " ")
            mTextEditText!!.setText(Html.fromHtml(text))
        }

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(ChangeNoteViewModel::class.java)
        }

        mNameEditText?.setOnClickListener{
            noteModel.mNoteHasChanged.postValue(true)
        }
        mNameEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                title = editable.toString()
                noteModel.noteTitle.postValue(formatTextValue(title))
            }
        })

        mTextEditText?.setOnClickListener {
            noteModel.mNoteHasChanged.postValue(true)
        }
        mTextEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                text = editable.toString()
                noteModel.noteText.postValue(formatTextValue(text))
            }
        })
    }

    fun notifyChange(title: String?, text: String?){
        arguments?.putString("someTitle", title)
        arguments?.putString("someText", text)
        this.title = title
        this.text = text
    }

    private fun formatTextValue(value: String?): String  {
        var title = value?.replace("&nbsp;".toRegex(), " ")
        title = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            Html.fromHtml(title).toString()
        }
        return title
    }

    companion object {
        fun newInstance(title: String?, text: String?): EditorPagerFragment {
            val fragment = EditorPagerFragment()
            val args = Bundle()
            args.putString("someTitle", title)
            args.putString("someText", text)
            fragment.arguments = args
            return fragment
        }
    }
}

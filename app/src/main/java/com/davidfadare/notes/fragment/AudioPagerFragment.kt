package com.davidfadare.notes.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.AudioActivity
import com.davidfadare.notes.EditorActivity
import com.davidfadare.notes.EditorActivity.Companion.NEW_AUDIO_REQUEST
import com.davidfadare.notes.EditorActivity.Companion.noteAudioTAG
import com.davidfadare.notes.EditorActivity.Companion.noteColorTAG
import com.davidfadare.notes.R
import com.davidfadare.notes.billing.BillingViewModel
import com.davidfadare.notes.billing.localdb.AugmentedSkuDetails
import com.davidfadare.notes.data.ChangeNoteViewModel
import com.davidfadare.notes.recycler.AudioAdapter
import com.davidfadare.notes.util.Utility
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertByteToString
import com.davidfadare.notes.util.Utility.PermissionUtil.checkAudioPermissions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AudioPagerFragment : Fragment() {

    private lateinit var noteModel: ChangeNoteViewModel

    private var data: Array<String>? = emptyArray()
    var noteColor: Int = 0
    var fab: FloatingActionButton? = null
    private var listView: ListView? = null
    private var audioAdapter: AudioAdapter? = null

    private var premium: Boolean = false
    private var skuDetailsList = emptyList<AugmentedSkuDetails>()
    private lateinit var billingViewModel: BillingViewModel

    var emptyViewImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (data.isNullOrEmpty()) data = arguments?.getStringArray("someAudio")
        noteColor = arguments?.getInt(noteColorTAG) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_audio, container, false) as ViewGroup

        if (noteColor == 0) {
            noteColor = ContextCompat.getColor(requireContext(), R.color.blue_note)
        }

        val emptyView = rootView.findViewById<RelativeLayout>(R.id.empty_view)
        emptyViewImage = rootView.findViewById<ImageView>(R.id.empty_note_image)
        emptyViewImage?.drawable?.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN)

        listView = rootView.findViewById(R.id.audio_list)
        listView?.emptyView = emptyView
        onSetupListView()

        billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        billingViewModel.premiumLiveData.observe(this, Observer {
            it?.apply { premium = entitled }
        })
        billingViewModel.inappSkuDetailsListLiveData.observe(this, Observer {
            it?.let { skuDetailsList = it }
        })

        fab = rootView.findViewById(R.id.fab)
        fab?.backgroundTintList = ColorStateList.valueOf(noteColor)
        fab?.setOnClickListener {
            if (listView?.childCount ?: 0 > 0 || premium) {
                createAudio()
            } else {
                onPurchasePro()
            }
        }

        return rootView
    }

    private fun onSetupListView() {
        if (data != null) {
            audioAdapter = AudioAdapter(context!!, noteColor, *data!!)
            listView?.adapter = audioAdapter
        }
        listView?.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(ChangeNoteViewModel::class.java)
        }
        noteModel.noteAudio.observe(this, Observer {
            it?.let {
                data = convertByteToString(it)
                onSetupListView()
            }
        })
    }

    private fun onPurchasePro() {
        val discardButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
            Toast.makeText(context, " ", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        val positiveButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
            try {
                billingViewModel.makePurchase(activity as Activity, skuDetailsList.first())
            } catch (e: NoSuchElementException) {
                Toast.makeText(context, " ", Toast.LENGTH_LONG).show()
            }
        }

        Utility.createDialog(requireContext(), getString(R.string.premium_dialog_msg), getString(R.string.purchase_now), getString(R.string.cancel), discardButtonClickListener, getString(R.string.premium_prompt), positiveButtonClickListener)
    }

    private fun createAudio() {
        val intent = Intent(context, AudioActivity::class.java)
        intent.putExtra(noteColorTAG, noteColor)
        intent.putExtra(noteAudioTAG, data)
        activity?.startActivityForResult(intent, NEW_AUDIO_REQUEST)
    }

    override fun onStart() {
        super.onStart()
        if (isAdded) {
            checkAudioPermissions(requireActivity())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            NEW_AUDIO_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    data = resultData.getStringArrayExtra(noteAudioTAG)
                    if (isAdded) {
                        audioAdapter = AudioAdapter(requireContext(), noteColor, *data!!)
                        listView?.adapter = audioAdapter
                    }
                    arguments?.putStringArray("someAudio", data)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onPause() {
        audioAdapter?.stopAudio()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            EditorActivity.PERMISSION_REQUEST_AUDIO_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(context, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                    fab?.setOnClickListener {
                        Toast.makeText(context, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                        if (activity != null) (activity as EditorActivity).onBackPressed()
                    }
                }
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun notifyChange(text: Array<String>, color: Int){
        arguments?.putStringArray("someAudio", text)
        arguments?.putInt(noteColorTAG, color)
        data = text
        noteColor = color
        onSetupListView()
    }

    companion object {
        fun newInstance(text: Array<String>, color: Int): AudioPagerFragment {
            val fragment = AudioPagerFragment()
            val args = Bundle()
            args.putStringArray("someAudio", text)
            args.putInt(noteColorTAG, color)
            fragment.arguments = args
            return fragment
        }
    }
}

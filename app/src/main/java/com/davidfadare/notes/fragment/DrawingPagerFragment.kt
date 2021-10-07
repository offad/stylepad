package com.davidfadare.notes.fragment

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.DrawingActivity
import com.davidfadare.notes.DrawingActivity.Companion.noteDrawingIDTAG
import com.davidfadare.notes.DrawingActivity.Companion.noteDrawingURITAG
import com.davidfadare.notes.EditorActivity
import com.davidfadare.notes.EditorActivity.Companion.GALLERY_IMAGE_REQUEST
import com.davidfadare.notes.EditorActivity.Companion.NEW_IMAGE_REQUEST
import com.davidfadare.notes.EditorActivity.Companion.PERMISSION_REQUEST_DRAWING_CODE
import com.davidfadare.notes.EditorActivity.Companion.noteColorTAG
import com.davidfadare.notes.EditorActivity.Companion.noteDrawingTAG
import com.davidfadare.notes.R
import com.davidfadare.notes.billing.BillingViewModel
import com.davidfadare.notes.billing.localdb.AugmentedSkuDetails
import com.davidfadare.notes.data.ChangeNoteViewModel
import com.davidfadare.notes.recycler.ImageAdapter
import com.davidfadare.notes.util.Utility
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertByteToString
import com.davidfadare.notes.util.Utility.PermissionUtil.checkAudioPermissions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DrawingPagerFragment : Fragment() {

    private lateinit var noteModel: ChangeNoteViewModel

    var data: Array<String>? = emptyArray()
    var noteColor: Int = 0
    var fab: FloatingActionButton? = null
    private var gridView: GridView? = null
    private var imageAdapter: ImageAdapter? = null

    private var premium: Boolean = false
    private var skuDetailsList = emptyList<AugmentedSkuDetails>()
    private lateinit var billingViewModel: BillingViewModel

    var emptyViewImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (data.isNullOrEmpty()) data = arguments?.getStringArray("someData")
        noteColor = arguments?.getInt(noteColorTAG) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_drawing, container, false) as ViewGroup

        if (noteColor == 0) {
            noteColor = ContextCompat.getColor(requireContext(), R.color.blue_note)
        }

        val emptyView = rootView.findViewById<RelativeLayout>(R.id.empty_view)
        emptyViewImage = rootView.findViewById<ImageView>(R.id.empty_note_image)
        emptyViewImage?.drawable?.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN)

        gridView = rootView.findViewById(R.id.drawing_grid)
        gridView?.emptyView = emptyView
        onSetupGridView()
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
            if (gridView?.childCount ?: 0 > 0 || premium) {
                val alert = ViewDialog()
                alert.showDialog(activity!!)
            } else {
                onPurchasePro()
            }
        }
        return rootView
    }

    private fun onSetupGridView() {
        if (data != null) {
            imageAdapter = ImageAdapter(context!!, noteColor, *data!!)
            gridView?.adapter = imageAdapter
        }
        gridView?.choiceMode = GridView.CHOICE_MODE_SINGLE
        gridView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val intent = Intent(context, DrawingActivity::class.java)
            intent.putExtra(noteColorTAG, noteColor)
            intent.putExtra(noteDrawingTAG, data)
            intent.putExtra(noteDrawingIDTAG, i)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivityForResult(intent, NEW_IMAGE_REQUEST)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(ChangeNoteViewModel::class.java)
        }
        noteModel.noteDrawing.observe(this, Observer {
            it?.let {
                data = convertByteToString(it)
                onSetupGridView()
            }
        })
    }

    private fun onPurchasePro() {
        val discardButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
        }

        val positiveButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
            try {
                billingViewModel.makePurchase(activity as Activity, skuDetailsList.first())
            } catch (e: NoSuchElementException) {
                Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_LONG).show()
            }
        }

        Utility.createDialog(requireContext(), getString(R.string.premium_dialog_msg), getString(R.string.purchase_now), getString(R.string.cancel), discardButtonClickListener, getString(R.string.premium_prompt), positiveButtonClickListener)
    }

    private fun imageFromGallery() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, GALLERY_IMAGE_REQUEST)
    }

    private fun createImage(imageUri: Uri?) {
        val intent = Intent(context, DrawingActivity::class.java)
        intent.putExtra(noteColorTAG, noteColor)
        intent.putExtra(noteDrawingTAG, data)
        intent.putExtra(noteDrawingURITAG, imageUri)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        activity?.startActivityForResult(intent, NEW_IMAGE_REQUEST)
    }

    override fun onStart() {
        super.onStart()
        if (isAdded) {
            checkAudioPermissions(requireActivity())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            GALLERY_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    createImage(resultData?.data)
                }
            }
            NEW_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    data = resultData.getStringArrayExtra(noteDrawingTAG)
                    if (isAdded) {
                        imageAdapter = ImageAdapter(requireContext(), noteColor, *data!!)
                        gridView?.adapter = imageAdapter
                    }
                    arguments?.putStringArray("someData", data)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_DRAWING_CODE -> {
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
        arguments?.putStringArray("someData", text)
        arguments?.putInt(noteColorTAG, color)
        data = text
        noteColor = color
        onSetupGridView()
    }

    companion object {
        fun newInstance(text: Array<String>, color: Int): DrawingPagerFragment {
            val fragment = DrawingPagerFragment()
            val args = Bundle()
            args.putStringArray("someData", text)
            args.putInt(noteColorTAG, color)
            fragment.arguments = args
            return fragment
        }
    }

    inner class ViewDialog {
        fun showDialog(activity: Activity) {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_image_picker)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val dialogNew = dialog.findViewById<FloatingActionButton>(R.id.dialog_new_image)
            dialogNew.setBackgroundColor(noteColor)
            dialogNew.setOnClickListener {
                createImage(null)
                dialog.dismiss()
            }

            val dialogGallery = dialog.findViewById<FloatingActionButton>(R.id.dialog_gallery_image)
            dialogNew.setBackgroundColor(noteColor)
            dialogGallery.setOnClickListener {
                imageFromGallery()
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}

package com.davidfadare.notes.fragment

import android.animation.Animator
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.davidfadare.notes.BuildConfig
import com.davidfadare.notes.EditorActivity
import com.davidfadare.notes.EditorActivity.Companion.OPEN_NEW_ACTIVITY
import com.davidfadare.notes.EditorActivity.Companion.PERMISSION_REQUEST_CODE
import com.davidfadare.notes.EditorActivity.Companion.noteColorTAG
import com.davidfadare.notes.EditorActivity.Companion.noteID
import com.davidfadare.notes.EditorActivity.Companion.noteItem
import com.davidfadare.notes.MainActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.data.NoteViewModel
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.recycler.PreviewPagerAdapter
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertByteToString
import com.davidfadare.notes.util.Utility.Companion.changeWindowColor
import com.davidfadare.notes.util.Utility.ExportUtil.exportNote
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class NoteViewerFragment : Fragment() {

    private var vFillOverlay: View? = null

    private var mNoteId: Long = -1
    private var mTitle: String = ""
    private var mText: String = ""
    private var mImages: Array<String> = emptyArray()
    private var mAudio: Array<String> = emptyArray()
    private var mNoteColor: Int = 0

    private lateinit var noteModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        vFillOverlay = activity?.findViewById(R.id.vAnimationFill)

        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(NoteViewModel::class.java)
        }
        mNoteId = noteModel.notePosition.value ?: -1L
        noteModel.notePosition.observe(this, Observer {
            it?.apply {
                mNoteId = it
            }
        })

        onSetupNote()
    }

    private fun onSetupNote() {
        if(mNoteId == -1L){
            return
        }
        val note = getNoteInTable(noteModel.notesLiveData.value, mNoteId.toInt())

        val dateText = requireActivity().findViewById<TextView>(R.id.noteFragmentDate)
        val timeText = requireActivity().findViewById<TextView>(R.id.noteFragmentTime)
        val viewPagerTab = requireActivity().findViewById<TabLayout>(R.id.preview_pager_tab)

        var mPagerAdapter = PreviewPagerAdapter(fragmentManager!!, null, context!!)
        val mPager = requireActivity().findViewById<ViewPager>(R.id.notePager)
        mPager.adapter = mPagerAdapter
        viewPagerTab?.setupWithViewPager(mPager)

        mTitle = note?.title ?: ""
        mText = note?.text ?: ""
        mNoteColor = note?.noteColor ?: 0

        mImages = convertByteToString(note?.images ?: byteArrayOf())
        mAudio = convertByteToString(note?.audio ?: byteArrayOf())
        val noteDate = note?.noteDate ?: 0L

        if (isAdded && activity != null) {
            val dateDay = SimpleDateFormat("dd", Locale.US)
            val dateYear = SimpleDateFormat("MMM yyyy", Locale.US)
            val timeFormat = SimpleDateFormat("hh:mm aa", Locale.US)

            val date = dateDay.format(noteDate).toUpperCase()
            val datePart = dateYear.format(noteDate).toUpperCase()
            val spannableString1 = SpannableString(date)
            spannableString1.setSpan(AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_1)), 0, date.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            val spannableString2 = SpannableString(datePart)
            spannableString2.setSpan(AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_2)), 0, datePart.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            val dayText = TextUtils.concat(spannableString1, " ", spannableString2)
            val timedText = timeFormat.format(noteDate).toUpperCase()

            val noteBar = activity?.findViewById<View>(R.id.noteColorView)
            val toolbar = (activity as MainActivity).toolbar

            try {
                mPagerAdapter = PreviewPagerAdapter(fragmentManager!!, note, requireContext())
                mPager?.adapter = mPagerAdapter

                viewPagerTab?.setupWithViewPager(mPager)
                val tabIcons = intArrayOf(R.drawable.pager_text, R.drawable.pager_image, R.drawable.pager_audio)
                for (i in 0 until (viewPagerTab?.tabCount ?: 0)) {
                    viewPagerTab?.getTabAt(i)?.setIcon(tabIcons[i])
                }

                toolbar?.setBackgroundColor(mNoteColor)
                noteBar?.setBackgroundColor(mNoteColor)
                viewPagerTab?.setSelectedTabIndicatorColor(mNoteColor)
                viewPagerTab?.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(mPager) {
                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                        super.onTabUnselected(tab)
                        val tabColor = ContextCompat.getColor(context!!, android.R.color.darker_gray)
                        tab?.icon?.setColorFilter(tabColor, PorterDuff.Mode.SRC_IN)
                    }

                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        super.onTabSelected(tab)
                        tab?.icon?.setColorFilter(mNoteColor, PorterDuff.Mode.SRC_IN)
                    }
                })
                viewPagerTab?.setTabTextColors(android.R.color.darker_gray, mNoteColor)

                dateText.text = dayText
                timeText.text = timedText

                changeWindowColor(requireActivity(), activity?.window, mNoteColor)

            } catch (e: NullPointerException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getNoteInTable(list: List<Note>?, id: Int): Note?{
        if(list.isNullOrEmpty() || id == -1) return null
        for(note in list){
            if(note.id == id) return note
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.preview_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_delete -> deleteNote()
            R.id.nav_edit -> changeActivity()
            R.id.nav_send -> sendNote()
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_NEW_ACTIVITY && isAdded) {
            if (resultCode == RESULT_OK) {
                val noteUri = data?.data
                try {
                    val id = ContentUris.parseId(noteUri)
                    updateNoteView(id)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(context, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                } else {
                    deleteNote()
                }
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        if (isAdded) {
            if (getString(R.string.screen_type) == "phone") {
                try {
                    val actionBar = (activity as MainActivity).supportActionBar
                    actionBar!!.setDisplayHomeAsUpEnabled(true)
                    val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
                    fab?.hide()
                } catch (e: NullPointerException) {
                    Log.e(this.javaClass.name, e.message)
                }

            }
        }
        super.onResume()
    }

    fun updateNoteView(id: Long) {
        noteModel.notePosition.postValue(id)

        onSetupNote()
    }

    private fun sendNote() {
        val files: ArrayList<Uri> = arrayListOf()
        for (string in mImages) {
            val imageLocation = File(string)
            val imageUri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider", imageLocation)
            files.add(imageUri)
        }
        for (string in mAudio) {
            val audioLocation = File(string)
            val audioUri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider", audioLocation)
            files.add(audioUri)
        }

        val intentShare = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "*/*"
        }

        intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intentShare.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        intentShare.putExtra(Intent.EXTRA_TEXT, mTitle + "\n" + mText + "\n")

        startActivity(Intent.createChooser(intentShare, getString(R.string.share_note_prompt)))
    }

    private fun deleteNote() {
        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                if (noteModel.notePosition.value?.toInt() != -1) {
                    val result = exportNote(getString(R.string.list_view_note_deleted_name, Calendar.getInstance().timeInMillis), requireActivity(), mNoteId)
                    val note = getNoteInTable(noteModel.notesLiveData.value, mNoteId.toInt())

                    var rowsAffected = 0
                    if (note != null) {
                        rowsAffected = noteModel.mDatabaseRepository.delete(note)
                    }

                    if (rowsAffected == 0) {
                        Toast.makeText(context, getString(R.string.editor_delete_note_failed),
                                Toast.LENGTH_SHORT).show()

                    } else {
                        val view = activity!!.findViewById<View>(R.id.coordinator_layout)
                        if (result.isNullOrBlank()) Snackbar.make(view, getString(R.string.pref_header_permission), Snackbar.LENGTH_LONG).show() else Snackbar.make(view, getString(R.string.editor_delete_note_successful, result), Snackbar.LENGTH_LONG).show()
                    }
                    activity?.onBackPressed()
                }
            }

            showDialog(discardButtonClickListener, getString(R.string.clear_prompt_single), getString(R.string.delete), getString(R.string.cancel))
        }
    }

    private fun changeActivity() {
        if (noteModel.notePosition.value?.toInt() != -1) {
            val note = getNoteInTable(noteModel.notesLiveData.value, mNoteId.toInt())
            val intent = Intent(context, EditorActivity::class.java)
            intent.putExtra(noteColorTAG, mNoteColor)
            intent.putExtra(noteItem, note)
            intent.putExtra(noteID, mNoteId)
            val edit = activity?.findViewById<View>(R.id.nav_edit)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && (edit != null) and (vFillOverlay != null)) {
                val location = IntArray(2)
                edit!!.getLocationOnScreen(location)
                intent.putExtra("cx", location[0])
                intent.putExtra("cy", location[1])

                circularReveal(vFillOverlay!!, location[0], location[1], intent)
            } else {
                startActivityForResult(intent, OPEN_NEW_ACTIVITY)
            }

        }
    }

    @TargetApi(21)
    private fun circularReveal(view: View, expansionPointX: Int, expansionPointY: Int, intent: Intent) {
        val shortAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val finalRadius = max(view.width, view.height).toFloat()
        view.visibility = View.VISIBLE
        view.setBackgroundColor(mNoteColor)
        val circularReveal = ViewAnimationUtils.createCircularReveal(view, expansionPointX, expansionPointY, 0f, finalRadius * 1.1f)
        circularReveal.duration = shortAnimationDuration
        circularReveal.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {
                startActivityForResult(intent, OPEN_NEW_ACTIVITY)
            }

            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })
        circularReveal.start()
    }

    private fun showDialog(discardButtonClickListener: DialogInterface.OnClickListener?, text: String?, positive_text: String?, negative_text: String?) {

        val builder = AlertDialog.Builder(context)
        val alertDialog: AlertDialog
        builder.setMessage(text)
        builder.setPositiveButton(positive_text, discardButtonClickListener)
        builder.setNegativeButton(negative_text) { dialog, _ ->
            dialog?.dismiss()
        }

        alertDialog = builder.create()
        alertDialog.show()
    }
}

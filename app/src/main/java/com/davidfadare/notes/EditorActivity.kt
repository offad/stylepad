package com.davidfadare.notes

import android.animation.Animator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.davidfadare.notes.billing.BillingViewModel
import com.davidfadare.notes.data.ChangeNoteViewModel
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.data.notedb.Note.NoteEntry.CONTENT_URI
import com.davidfadare.notes.recycler.ColorAdapter
import com.davidfadare.notes.recycler.EditorPagerAdapter
import com.davidfadare.notes.settings.SettingsActivity
import com.davidfadare.notes.util.AlarmReceiver
import com.davidfadare.notes.util.Utility.ByteArrayUtil.convertStringToByte
import com.davidfadare.notes.util.Utility.Companion.changeWindowColor
import com.davidfadare.notes.util.Utility.Companion.createDialog
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.ogaclejapan.smarttablayout.SmartTabLayout

class EditorActivity : AppCompatActivity() {

    private var mPager: ViewPager? = null
    private var mPagerAdapter: EditorPagerAdapter? = null

    private var mCurrentNoteData: Uri? = null

    private lateinit var note: Note
    private lateinit var noteModel: ChangeNoteViewModel

    private var noteTitle = ""
    private var noteText = ""
    private var notePassword = ""
    private var pinned = 0
    private var noteColor = 0
    private var noteAlarm = 0L
    private var noteDrawing: ByteArray? = null
    private var noteAudio: ByteArray? = null
    private var removeAlarm = false
    private var mNoteHasChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        noteModel = ViewModelProviders.of(this).get(ChangeNoteViewModel::class.java)

        noteColor = intent.getIntExtra(noteColorTAG, 0)
        noteColor = if (noteColor == 0) {
            ContextCompat.getColor(this, R.color.blue_note)
        } else noteColor

        changeWindowColor(this, window, noteColor)

        super.onCreate(savedInstanceState)
        overridePendingTransition(0, android.R.anim.slide_out_right)
        setContentView(R.layout.activity_edit)

        onSetupViewModel()

        onSetup()

        onSetupAds()
    }

    private fun onSetup() {
        val fill = findViewById<View>(R.id.vEditFill)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val viewPagerTab = findViewById<SmartTabLayout>(R.id.view_pager_tab)
        mPager = findViewById(R.id.pager)

        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(noteColor)
        viewPagerTab!!.setBackgroundColor(noteColor)
        fill.setBackgroundColor(noteColor)
        fill.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                val cx = intent.getIntExtra("cx", size.x)
                val cy = intent.getIntExtra("cy", size.y)

                circularHide(fill, cx, cy)
            }
        }

        val actionBar = supportActionBar
        actionBar?.setHomeAsUpIndicator(R.drawable.menu_close)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val id = intent.getLongExtra(noteID, -1)

        if (id == -1L) {
            title = getString(R.string.editor_activity_title_new_note)
            invalidateOptionsMenu()

            mPagerAdapter = EditorPagerAdapter(supportFragmentManager,this)
        } else {
            title = getString(R.string.editor_activity_title_edit_note)
            mCurrentNoteData = ContentUris.withAppendedId(CONTENT_URI, id)
            mPagerAdapter = EditorPagerAdapter(supportFragmentManager,this@EditorActivity)

            note = intent.getSerializableExtra(noteItem) as Note
            note.noteColor = noteColor

            if(!noteModel.initialised) noteModel.setupNote(note)
        }

        mPager?.adapter = mPagerAdapter
        viewPagerTab.setViewPager(mPager)
    }

    private fun onSetupViewModel() {
        noteModel.noteTitle.observe(this, Observer {
            it?.let {
                noteTitle = it
            }
        })
        noteModel.noteText.observe(this, Observer {
            it?.let {
                noteText = it
            }
        })
        noteModel.noteColor.observe(this, Observer {
            it?.let {
                noteColor = it
            }
        })
        noteModel.notePassword.observe(this, Observer {
            it?.let {
                notePassword = it
            }
        })
        noteModel.noteAlarm.observe(this, Observer {
            it?.let {
                noteAlarm = it
            }
        })
        noteModel.noteAudio.observe(this, Observer {
            it?.let {
                noteAudio = it
            }
        })
        noteModel.noteDrawing.observe(this, Observer {
            it?.let {
                noteDrawing = it
            }
        })
        noteModel.pinned.observe(this, Observer {
            it?.let {
                pinned = it
            }
        })
        noteModel.mNoteHasChanged.observe(this, Observer {
            it?.let {
                mNoteHasChanged = it
            }
        })
        noteModel.removeAlarm.observe(this, Observer {
            it?.let {
                removeAlarm = it
            }
        })
    }

    private fun onSetupAds() {
        val layout = findViewById<LinearLayout>(R.id.edit_layout)

        val billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        billingViewModel.queryPurchases()

        val mAdView = findViewById<AdView>(R.id.adView)
        mAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(i: Int) {
                mAdView?.visibility = View.GONE
                super.onAdFailedToLoad(i)
            }

            override fun onAdLoaded() {
                mAdView?.visibility = View.VISIBLE
                super.onAdLoaded()
            }
        }

        billingViewModel.premiumLiveData.observe(this, Observer {
            it?.apply {
                if (entitled) {
                    mAdView?.visibility = View.GONE
                    layout.removeView(mAdView)
                }
            }
        })

        val adRequest = AdRequest.Builder()
                .build()
        mAdView?.loadAd(adRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (!mNoteHasChanged) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                    noteModel.mNoteHasChanged.postValue(false)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                createDialog(this, getString(R.string.unsaved_changes_dialog_msg), getString(R.string.keep_editing), getString(R.string.discard), discardButtonClickListener)
            }
            R.id.color -> {
                val edit = findViewById<View>(R.id.color)
                val location = IntArray(2)
                edit.getLocationOnScreen(location)
                intent.putExtra("cx", location[0])
                intent.putExtra("cy", location[1])
                showColorDialog()
            }
            R.id.save -> try {
                saveNote()
            } catch (e: RuntimeException) {
                Toast.makeText(this, getString(R.string.editor_insert_note_failed), Toast.LENGTH_SHORT).show()
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition()
                } else {
                    NavUtils.navigateUpFromSameTask(this)
                }
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            NEW_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val drawingData = data.getStringArrayExtra(noteDrawingTAG)

                    noteModel.noteDrawing.postValue(convertStringToByte(drawingData))
                    noteModel.mNoteHasChanged.postValue(true)

                    mPagerAdapter?.noteDrawingData = drawingData
                    mPagerAdapter?.notifyDataSetChanged()
                    mPagerAdapter?.drawingPagerFragment?.onActivityResult(NEW_IMAGE_REQUEST, resultCode, data)

                    if (noteModel.noteTitle.value.isNullOrBlank()) {
                        Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, R.string.editor_saved_complete, Toast.LENGTH_LONG).show()
                    }
                }
            }
            NEW_AUDIO_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val audioData = data.getStringArrayExtra(noteAudioTAG)

                    noteModel.noteAudio.postValue(convertStringToByte(audioData))
                    noteModel.mNoteHasChanged.postValue(true)

                    mPagerAdapter?.noteAudioData = audioData
                    mPagerAdapter?.notifyDataSetChanged()
                    mPagerAdapter?.audioPagerFragment?.onActivityResult(NEW_AUDIO_REQUEST, resultCode, data)

                    if (noteModel.noteTitle.value.isNullOrBlank()) {
                        Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, R.string.editor_saved_complete, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_DRAWING_CODE -> {
                mPagerAdapter?.drawingPagerFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            PERMISSION_REQUEST_AUDIO_CODE -> {
                mPagerAdapter?.audioPagerFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun saveNote() {
        if (!mSaveNote) return

        val checkAlarm = java.lang.Long.valueOf(noteAlarm)
        var setAlarm = false

        if (mCurrentNoteData == null && TextUtils.isEmpty(noteTitle) && TextUtils.isEmpty(noteText)) {
            val title = when {
                (noteDrawing?.isNotEmpty() == true) -> getString(R.string.editor_insert_note_drawing)
                (noteAudio?.isNotEmpty() == true) -> getString(R.string.editor_insert_note_audio)
                else -> {
                    Toast.makeText(this, getString(R.string.editor_insert_note_empty), Toast.LENGTH_SHORT).show()
                    return
                }
            }
            noteModel.noteTitle.postValue(title)
            noteTitle = title
        } else if (TextUtils.isEmpty(noteTitle)) {
            val title = getString(R.string.editor_insert_title_empty)
            noteModel.noteTitle.postValue(title)
            noteTitle = title
        }

        if (removeAlarm) {
            noteModel.noteAlarm.postValue(0)
            noteAlarm = 0
            cancelCurrentNoteAlarm()
        } else if (checkAlarm != noteAlarm && noteAlarm > System.currentTimeMillis()) {
            setAlarm = true
        }

        if (noteColor == 0) {
            noteModel.noteColor.postValue(ContextCompat.getColor(this, R.color.blue_note))
        }

        note = if (::note.isInitialized) {
            Note(note.id, noteTitle, noteText, System.currentTimeMillis(), noteColor, notePassword, noteAlarm, noteAudio, noteDrawing, pinned)
        } else {
            Note(noteTitle, noteText, System.currentTimeMillis(), noteColor, notePassword, noteAlarm, noteAudio, noteDrawing, pinned)
        }

        noteModel.mNoteHasChanged.postValue(false)
        mNoteHasChanged = false

        if (mCurrentNoteData == null) {
            val id = noteModel.mDatabaseRepository.insert(note)

            if (id == -1L) {
                Toast.makeText(this, getString(R.string.editor_insert_note_failed),
                        Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
            } else {
                if (setAlarm) setCurrentNoteAlarm(noteAlarm)

                val result = Intent()
                result.data = mCurrentNoteData
                setResult(Activity.RESULT_OK, result)
                Toast.makeText(this, getString(R.string.editor_insert_note_successful),
                        Toast.LENGTH_SHORT).show()

            }
        } else {
            val rowsAffected = noteModel.mDatabaseRepository.update(note)

            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_insert_note_failed),
                        Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
            } else {
                if (setAlarm) setCurrentNoteAlarm(noteAlarm)

                val result = Intent()
                result.data = mCurrentNoteData
                setResult(Activity.RESULT_OK, result)
                Toast.makeText(this, getString(R.string.editor_insert_note_successful),
                        Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setCurrentNoteAlarm(time: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notifyIntent = Intent(this, AlarmReceiver::class.java)
        notifyIntent.putExtra(getString(R.string.note_alarm), ContentUris.parseId(mCurrentNoteData).toInt())
        notifyIntent.putExtra(getString(R.string.note_title), noteTitle)
        notifyIntent.putExtra(getString(R.string.note_text), noteText)
        notifyIntent.putExtra(getString(R.string.note_color), noteColor)

        val alarmIntent = PendingIntent.getBroadcast(this, ContentUris.parseId(mCurrentNoteData).toInt(), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, alarmIntent)
    }

    private fun cancelCurrentNoteAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notifyIntent = Intent(this, AlarmReceiver::class.java)
        notifyIntent.putExtra(getString(R.string.note_alarm), ContentUris.parseId(mCurrentNoteData).toInt())
        notifyIntent.putExtra(getString(R.string.note_title), noteTitle)
        notifyIntent.putExtra(getString(R.string.note_text), noteText)
        notifyIntent.putExtra(getString(R.string.note_color), noteColor)

        val alarmIntent = PendingIntent.getBroadcast(this, ContentUris.parseId(mCurrentNoteData).toInt(), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.cancel(alarmIntent)
    }

    override fun onResume() {
        changeWindowColor(this, window, noteColor)
        super.onResume()
    }

    override fun onPause() {
        overridePendingTransition(0, android.R.anim.slide_out_right)
        super.onPause()
    }

    override fun onDestroy() {
        val syncConnPref = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_PREF_AUTO_SAVE, true)
        if (syncConnPref && mNoteHasChanged) {
            saveNote()
        }

        if (!mSaveNote) mSaveNote = true

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (mPager?.currentItem == 0) {
            if (!mNoteHasChanged) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                    noteModel.mNoteHasChanged.postValue(false)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                createDialog(this, getString(R.string.unsaved_changes_dialog_msg), getString(R.string.keep_editing), getString(R.string.discard), discardButtonClickListener)
            }
        } else {
            try {
                mPager?.currentItem = mPager?.currentItem?.minus(1) ?: 0
            } catch (e: IllegalStateException) {
                Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showColorDialog() {
        val builder = AlertDialog.Builder(this)

        val colorArray = resources.getStringArray(R.array.color_options)
        val colorAdapter = ColorAdapter(this, *colorArray)

        val dialogView = layoutInflater
                .inflate(R.layout.color_selector, null)
        dialogView.setPadding(16, 16, 16, 16)

        builder.setView(dialogView)
        builder.setPositiveButton(R.string.done) { dialog, _ ->
            if (dialog != null) {
                noteModel.mNoteHasChanged.postValue(true)
                if (mCurrentNoteData == null) mSaveNote = false
                dialog.dismiss()
                mPagerAdapter?.notifyDataSetChanged()
                this@EditorActivity.recreate()
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
            dialogInterface?.dismiss()
        }

        val items = dialogView.findViewById<GridView>(R.id.color_grid)

        items.choiceMode = GridView.CHOICE_MODE_SINGLE
        items.adapter = colorAdapter
        items.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val color = Color.parseColor(colorArray[i])
            noteModel.noteColor.postValue(color)
            intent.putExtra(noteColorTAG, color)
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    @TargetApi(21)
    private fun circularHide(view: View, expansionPointX: Int, expansionPointY: Int) {
        val shortAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val finalRadius = maxOf(view.width, view.height).toFloat()
        view.setBackgroundColor(noteColor)
        val circularHide = ViewAnimationUtils.createCircularReveal(view, expansionPointX, expansionPointY, finalRadius * 1.1f, 0f)
        circularHide.interpolator = AccelerateDecelerateInterpolator()
        circularHide.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}

            override fun onAnimationEnd(animator: Animator) {
                view.visibility = View.INVISIBLE
            }

            override fun onAnimationCancel(animator: Animator) {}

            override fun onAnimationRepeat(animator: Animator) {}
        })
        circularHide.duration = shortAnimationDuration
        circularHide.start()
    }

    companion object {
        const val noteColorTAG = "noteColor"
        const val noteDrawingTAG = "noteDrawing"
        const val noteAudioTAG = "noteAudio"
        const val noteID = "noteID"
        const val noteItem = "noteContents"

        var mSaveNote: Boolean = true

        const val OPEN_NEW_ACTIVITY = 17
        const val GALLERY_IMAGE_REQUEST = 24
        const val NEW_IMAGE_REQUEST = 32
        const val NEW_AUDIO_REQUEST = 33
        const val PERMISSION_REQUEST_CODE = 41
        const val PERMISSION_REQUEST_AUDIO_CODE = 42
        const val PERMISSION_REQUEST_DRAWING_CODE = 43
    }
}

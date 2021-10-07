package com.davidfadare.notes.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.R
import com.davidfadare.notes.data.ChangeNoteViewModel
import com.davidfadare.notes.settings.SettingsActivity
import com.davidfadare.notes.util.Utility
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class InfoPagerFragment : Fragment() {

    private lateinit var noteModel: ChangeNoteViewModel

    var password: String? = null
    var alarmDate: Long? = 0
    var pinStatus: Int? = 0
    var noteDate: Long? = 0

    private var mLastClickTime: Long = 0

    private var pinInputImage: ImageView? = null
    private var lockInputImage: ImageView? = null
    private var mAlarmClearButton: ImageView? = null

    private var pinInputView: LinearLayout? = null
    private var lockInputView: LinearLayout? = null
    private var mAlarmEditView: LinearLayout? = null

    private var mAlarmEditText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        password = arguments?.getString("somePass")
        noteDate = arguments?.getLong("someDate")
        alarmDate = arguments?.getLong("someAlarm")
        pinStatus = arguments?.getInt("somePin")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_info, container, false) as ViewGroup

        pinInputView = rootView.findViewById(R.id.pin_layout)
        pinInputImage = rootView.findViewById(R.id.pin_input)
        lockInputImage = rootView.findViewById(R.id.password_input)
        lockInputView = rootView.findViewById<LinearLayout>(R.id.password_layout)

        val mDateEditText = rootView.findViewById<TextView>(R.id.date_text)
        val mTimeEditText = rootView.findViewById<TextView>(R.id.time_text)

        if (noteDate == 0L) {
            noteDate = System.currentTimeMillis()
        }

        if (mDateEditText != null) {
            val format = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
            val date = format.format(noteDate)
            mDateEditText.text = date
        }

        if (mTimeEditText != null) {
            val format = SimpleDateFormat("hh:mm a", Locale.US)
            val date = format.format(noteDate).toUpperCase()
            mTimeEditText.text = date
        }

        mAlarmEditView = rootView.findViewById<LinearLayout>(R.id.alarm_layout)
        mAlarmEditText = rootView.findViewById<TextView>(R.id.alarm_text)
        mAlarmClearButton = rootView.findViewById(R.id.alarm_clear_button)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(ChangeNoteViewModel::class.java)
        }
        if (pinInputView != null && pinInputImage != null) {
            switchPin(false)
            pinInputView?.setOnClickListener(View.OnClickListener {
                noteModel.mNoteHasChanged.postValue(true)
                if (SystemClock.elapsedRealtime() - mLastClickTime < 700) {
                    return@OnClickListener
                }

                mLastClickTime = SystemClock.elapsedRealtime()

                switchPin(true)
            })
        }

        if (lockInputView != null && lockInputImage != null) {
            val syncConnPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_GEN_ENABLE, false)
            val passEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_PASSWORD_ENABLE, true)

            if (passEnabled || !syncConnPref) {
                switchLock()
            }

            lockInputView!!.setOnClickListener(View.OnClickListener {
                noteModel.mNoteHasChanged.postValue(true)
                if (SystemClock.elapsedRealtime() - mLastClickTime < 700) {
                    return@OnClickListener
                }

                mLastClickTime = SystemClock.elapsedRealtime()

                if (!passEnabled || syncConnPref) {
                    Toast.makeText(context, getString(R.string.pref_passwords_disabled), Toast.LENGTH_LONG).show()
                } else {
                    openPasswordDialog()
                }
            })
        }

        if (mAlarmEditView != null && mAlarmEditText != null) {
            setNoteAlarm()
            mAlarmEditView?.setOnClickListener(View.OnClickListener {
                noteModel.mNoteHasChanged.postValue(true)
                if (SystemClock.elapsedRealtime() - mLastClickTime < 700) {
                    return@OnClickListener
                }

                mLastClickTime = SystemClock.elapsedRealtime()

                openAlarmDialog()
            })
        }
    }

    private fun setNoteAlarm() {
        if (alarmDate ?: 0 < System.currentTimeMillis()) {
            mAlarmEditText?.text = getString(R.string.editor_note_alarm_disabled)
            mAlarmClearButton?.visibility = View.GONE
            mAlarmClearButton?.setOnClickListener {}
        } else {
            val formatDate = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val formatTime = SimpleDateFormat("hh:mm a", Locale.US)

            val date = formatDate.format(alarmDate)
            val time = formatTime.format(alarmDate)
            mAlarmEditText?.text = getString(R.string.editor_note_alarm_time, date, time)
            mAlarmClearButton?.visibility = View.VISIBLE
            mAlarmClearButton?.setOnClickListener {
                openRemoveAlarmDialog()
            }
        }
    }

    private fun switchPin(change: Boolean) {
        if (change) {
            when (pinStatus) {
                0 -> pinStatus = 1
                1 -> pinStatus = 0
            }
        }

        when (pinStatus) {
            0 -> pinInputImage!!.setImageResource(R.drawable.infofragment_pin_off)
            1 -> pinInputImage!!.setImageResource(R.drawable.listitem_pin)
        }

        noteModel.pinned.postValue(pinStatus ?: 0)
    }

    private fun switchLock() {
        if (TextUtils.isEmpty(password)) {
            lockInputImage!!.setImageResource(R.drawable.infofragment_lock_open)
        } else {
            lockInputImage!!.setImageResource(R.drawable.listitem_lock)
        }
    }

    private fun openRemoveAlarmDialog() {
        val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
            noteModel.noteAlarm.postValue(0)
            noteModel.removeAlarm.postValue(true)
            setNoteAlarm()
        }

        Utility.createDialog(requireContext(), getString(R.string.remove_alarm_dialog_msg), getString(R.string.remove), getString(R.string.cancel), discardButtonClickListener)
    }

    private fun openAlarmDialog() {
        val dateTimeDialogFragment = SwitchDateTimeDialogFragment.newInstance(
                "",
                getString(R.string.ok),
                getString(R.string.cancel),
                getString(R.string.clean)
        )

        dateTimeDialogFragment.startAtCalendarView()
        dateTimeDialogFragment.setHighlightAMPMSelection(true)
        dateTimeDialogFragment.setTimeZone(TimeZone.getDefault())
        dateTimeDialogFragment.minimumDateTime = Date()
        dateTimeDialogFragment.setDefaultDateTime(Date())

        try {
            dateTimeDialogFragment.simpleDateMonthAndDayFormat = SimpleDateFormat("dd MMMM", Locale.US)
        } catch (e: SwitchDateTimeDialogFragment.SimpleDateMonthAndDayFormatException) {
            Log.e(InfoPagerFragment::class.java.name, e.message)
        }

        dateTimeDialogFragment.setOnButtonClickListener(object : SwitchDateTimeDialogFragment.OnButtonClickListener {
            override fun onPositiveButtonClick(date: Date) {
                if (date.time > System.currentTimeMillis()) {
                    alarmDate = date.time
                    noteModel.noteAlarm.postValue(alarmDate ?: 0)
                    noteModel.removeAlarm.postValue(false)
                    setNoteAlarm()
                    Toast.makeText(activity, getString(R.string.editor_saved_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, getString(R.string.editor_note_alarm_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNegativeButtonClick(date: Date) {
                dateTimeDialogFragment.dismiss()
            }
        })

        dateTimeDialogFragment.show(activity!!.supportFragmentManager, SwitchDateTimeDialogFragment::class.java.name)
    }

    private fun openPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setMessage(getString(R.string.password_set_dialog_msg))

        val editText = EditText(activity)
        editText.setSingleLine()

        val view = LayoutInflater.from(context).inflate(R.layout.password_dialog, null)
        val layout = view.findViewById<LinearLayout>(R.id.password_layout)
        layout.addView(editText)

        builder.setView(view)
        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            if (dialog != null) {
                password = editText.text.toString()
                noteModel.notePassword.postValue(password)
                switchLock()
                dialog.dismiss()
            }
        }
        builder.setNegativeButton(getString(R.string.discard)) { dialog, _ ->
            dialog?.dismiss()
        }

        val alertDialog = builder.create()

        alertDialog.show()
    }

    fun notifyChange(pass: String?, alarm: Long, date: Long, pinned: Int){
        arguments?.putString("somePass", pass)
        arguments?.putLong("someAlarm", alarm)
        arguments?.putLong("someDate", date)
        arguments?.putInt("somePin", pinned)
        password = pass
        alarmDate = alarm
        pinStatus = pinned
        noteDate = date

        switchPin(false)
        switchLock()
        setNoteAlarm()
    }

    companion object {
        fun newInstance(pass: String?, alarm: Long, date: Long, pinned: Int): InfoPagerFragment {
            val fragment = InfoPagerFragment()
            val args = Bundle()
            args.putString("somePass", pass)
            args.putLong("someAlarm", alarm)
            args.putLong("someDate", date)
            args.putInt("somePin", pinned)
            fragment.arguments = args
            return fragment
        }
    }

}

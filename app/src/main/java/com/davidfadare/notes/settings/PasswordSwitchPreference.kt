package com.davidfadare.notes.settings

import android.content.Context
import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.davidfadare.notes.R

class PasswordSwitchPreference : SwitchPreference {
    private var passwordEditText: EditText? = null
    private val builder = AlertDialog.Builder(context)
    private var dialog: AlertDialog? = null
    private var content: View? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        content = LayoutInflater.from(getContext()).inflate(R.layout.password_dialog, null)

        passwordEditText = EditText(context, attrs)

        builder.setView(content)
        builder.setTitle(context.resources.getString(R.string.pref_master_key_confirm))

        builder.setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.setPositiveButton(context.resources.getString(R.string.ok)) { dialog, _ ->
            val password = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(SettingsActivity.KEY_PREF_PASSWORD, "0")
            if (passwordEditText!!.text.toString() != password) {
                Toast.makeText(getContext(), context.resources.getString(R.string.list_view_note_password_incorrect), Toast.LENGTH_SHORT).show()
            } else {
                isChecked = !isChecked
                persistBoolean(isChecked)
            }
            dialog.cancel()
        }

        dialog = builder.create()
    }

    override fun onBindView(view: View) {
        detachView(passwordEditText!!, content)

        val layout = content!!.findViewById<LinearLayout>(R.id.password_layout)
        if (layout != null) {
            if (passwordEditText!!.parent == null) {
                layout.addView(passwordEditText)
            }
        }

        passwordEditText!!.setText("")
        passwordEditText!!.hint = context.resources.getString(R.string.pref_header_password)

        super.onBindView(view)
    }

    private fun detachView(view: View, newParent: View?) {
        val oldParent = view.parent
        if (oldParent !== newParent && oldParent != null) {
            (oldParent as ViewGroup).removeView(view)
        }
    }

    override fun onClick() {
        dialog!!.show()
    }
}

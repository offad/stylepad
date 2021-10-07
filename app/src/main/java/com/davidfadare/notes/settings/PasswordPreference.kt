package com.davidfadare.notes.settings

import android.app.AlertDialog
import android.content.Context
import android.preference.DialogPreference
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.davidfadare.notes.R

class PasswordPreference : DialogPreference {
    private var oldPassword: String? = null
    private var password = ""
    private var oldPasswordEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmEditText: EditText? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context, attrs)
    }

    override fun onBindDialogView(view: View) {
        detachView(oldPasswordEditText!!, view)
        detachView(passwordEditText!!, view)
        detachView(confirmEditText!!, view)

        val layout = view.findViewById<LinearLayout>(R.id.password_layout)
        layout.addView(oldPasswordEditText)
        layout.addView(passwordEditText)
        layout.addView(confirmEditText)

        passwordEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                verifyInput()
            }
        })

        confirmEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                verifyInput()
            }
        })

        oldPasswordEditText!!.setText("")
        oldPasswordEditText!!.hint = context.getString(R.string.pref_note_password_old)
        passwordEditText!!.setText("")
        passwordEditText!!.hint = context.getString(R.string.pref_note_password_new)
        confirmEditText!!.setText("")
        confirmEditText!!.hint = context.getString(R.string.pref_note_password_repeat)

        super.onBindDialogView(view)
    }


    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (!positiveResult) {
            return
        }

        if ((oldPasswordEditText!!.text.toString() == oldPassword || oldPasswordEditText!!.text.toString() == context.resources.getString(R.string.reset)) && password != "") {
            persistString(password)
            Toast.makeText(context, context.resources.getString(R.string.pref_master_key_changed), Toast.LENGTH_LONG).show()
        } else {
            if (password == "") {
                Toast.makeText(context, context.resources.getString(R.string.pref_master_key_empty), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, context.resources.getString(R.string.pref_master_key_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun init(context: Context, attrs: AttributeSet) {
        dialogLayoutResource = R.layout.password_dialog
        oldPassword = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_PASSWORD, "0")
        oldPasswordEditText = EditText(context, attrs)
        passwordEditText = EditText(context, attrs)
        confirmEditText = EditText(context, attrs)
    }

    private fun detachView(view: View, newParent: View) {
        val oldParent = view.parent
        if (oldParent !== newParent && oldParent != null) {
            (oldParent as ViewGroup).removeView(view)
        }
    }

    private fun verifyInput() {
        val newPassword = passwordEditText!!.text.toString()
        val confirmedPassword = confirmEditText!!.text.toString()

        var passwordOk = false
        if (newPassword == confirmedPassword && newPassword != "") {
            passwordOk = true
            password = newPassword
        }

        val dialog = dialog as AlertDialog?
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = passwordOk
    }
}

package com.davidfadare.notes.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.util.Utility.Companion.uri
import com.davidfadare.notes.util.Utility.ExportUtil.cursorToText
import com.davidfadare.notes.util.Utility.ExportUtil.getDeviceName
import com.davidfadare.notes.util.Utility.ExportUtil.importDatabase
import com.davidfadare.notes.util.Utility.ExportUtil.textToCursor
import com.davidfadare.notes.util.Utility.PermissionUtil.checkWritePermissions
import com.davidfadare.notes.util.Utility.RealPathUtil.getRealPath
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.*

class RestoreActivity : AppCompatActivity() {

    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var mOpenFileId: String? = null

    private var mFileTitleEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_restore)

        mFileTitleEditText = findViewById(R.id.file_title_edittext)
        mFileTitleEditText?.setText(getPreferences(Context.MODE_PRIVATE).getString(PREF_TAG, getDeviceName()))
        mFileTitleEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                getPreferences(Context.MODE_PRIVATE).edit().putString(PREF_TAG, editable.toString()).apply()
            }
        })

        findViewById<Button>(R.id.import_btn).setOnClickListener { openFilePicker() }
        findViewById<Button>(R.id.upload_btn).setOnClickListener { uploadFile() }

        findViewById<Button>(R.id.import_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT, uri)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_DEFAULT)

            val sIntent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            sIntent.putExtra("CONTENT_TYPE", "*/*")
            sIntent.addCategory(Intent.CATEGORY_DEFAULT)

            val chooserIntent: Intent = if (packageManager.resolveActivity(sIntent, 0) != null) {
                Intent.createChooser(sIntent, getString(R.string.open_file)).putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent))
            } else {
                Intent.createChooser(intent, getString(R.string.open_file))
            }

            try {
                startActivityForResult(chooserIntent, PICKFILE_REQUEST_CODE)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.install_file_msg), Toast.LENGTH_SHORT).show()
            }

        }

        checkWritePermissions(this)

        requestSignIn()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
            }
            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            PICKFILE_REQUEST_CODE -> {
                if (resultData?.data != null) {
                    val path = getRealPath(this, resultData.data!!)
                    if (path != null) {
                        importDatabase(path, this@RestoreActivity, false)
                    } else {
                        Toast.makeText(this@RestoreActivity, getString(R.string.invalid_file_msg), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@RestoreActivity, getString(R.string.invalid_file_msg), Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_CODE_SIGN_IN -> {
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData)
                } else {
                    Toast.makeText(this@RestoreActivity, getString(R.string.error_occurred), Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_CODE_OPEN_DOCUMENT -> {
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    val uri = resultData.data
                    if (uri != null) {
                        openFileFromFilePicker(uri)
                    } else {
                        Toast.makeText(this@RestoreActivity, getString(R.string.pref_note_import_failed_file_location), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        return true
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener { googleAccount ->

                    val credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE))
                    credential.selectedAccount = googleAccount.account
                    val googleDriveService = Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            GsonFactory(),
                            credential)
                            .setApplicationName(getString(R.string.app_name))
                            .build()

                    mDriveServiceHelper = DriveServiceHelper(googleDriveService)
                }
                .addOnFailureListener { exception -> Log.e(PREF_TAG, "Unable to sign in.", exception) }
    }

    private fun openFilePicker() {
        if (mDriveServiceHelper != null) {
            val pickerIntent = (mDriveServiceHelper as DriveServiceHelper).createFilePickerIntent()

            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT)
        }
    }

    private fun openFileFromFilePicker(uri: Uri) {
        if (mDriveServiceHelper != null) {
            (mDriveServiceHelper as DriveServiceHelper).openFileUsingStorageAccessFramework(contentResolver, uri)
                    .addOnSuccessListener { nameAndContent ->
                        val name = nameAndContent.first
                        val content = nameAndContent.second
                        textToCursor(this, name, content)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, R.string.pref_note_import_failed_compatible, Toast.LENGTH_SHORT).show()}
        }
    }

    private fun uploadFile() {
        if (mDriveServiceHelper != null) {
            var fileName = mFileTitleEditText?.text?.toString()
            if (fileName.isNullOrBlank()) {
                fileName = getDeviceName()
            }
            val fileContent = cursorToText(this)

            findViewById<ProgressBar>(R.id.restore_progress).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.btn_layout).visibility = View.GONE
            findViewById<ProgressBar>(R.id.restore_progress).progress = 50

            (mDriveServiceHelper as DriveServiceHelper).createFile(fileName, fileContent)
                    .addOnSuccessListener { nameAndContent ->
                        mOpenFileId = nameAndContent?.first()?.toString()
                        findViewById<ProgressBar>(R.id.restore_progress).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.btn_layout).visibility = View.VISIBLE
                        Toast.makeText(this, R.string.pref_header_export_successful, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        findViewById<ProgressBar>(R.id.restore_progress).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.btn_layout).visibility = View.VISIBLE
                        Toast.makeText(this, R.string.pref_header_export_failed, Toast.LENGTH_SHORT).show()
                    }
        }
    }

    companion object {
        const val PICKFILE_REQUEST_CODE = 18
        const val PERMISSION_REQUEST_CODE = 20
        const val REQUEST_CODE_SIGN_IN = 28
        const val REQUEST_CODE_OPEN_DOCUMENT = 30

        const val PREF_TAG = "RestoreActivity"
    }
}

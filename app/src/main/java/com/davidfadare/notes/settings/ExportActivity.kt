package com.davidfadare.notes.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.davidfadare.notes.EditorActivity.Companion.PERMISSION_REQUEST_CODE
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.util.Utility.ExportUtil.exportDatabase
import com.davidfadare.notes.util.Utility.PermissionUtil.checkWritePermissions

class ExportActivity : AppCompatActivity() {

    internal lateinit var exportOptions: Array<String>
    internal lateinit var result: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_export)

        exportOptions = resources.getStringArray(R.array.pref_export_options)
        result = exportOptions[0]

        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, exportOptions)

        val textPath: TextView = findViewById(R.id.export_path)
        val fileEdit: EditText = findViewById(R.id.file_name_edit)
        val options: Spinner = findViewById(R.id.export_options)
        val progressBar: ProgressBar = findViewById(R.id.export_progress)

        options.adapter = adapter
        options.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                result = exportOptions[i]
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        val exportButton: Button = findViewById(R.id.export_button)
        exportButton.setOnClickListener {
            var name = fileEdit.text.toString()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this@ExportActivity, getString(R.string.pref_header_export_failed_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            exportButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 50

            name = changeExportNote(name)

            val handler = Handler()
            handler.postDelayed(object : Runnable {
                override fun run() {
                    try {
                        val result = exportDatabase(name, this@ExportActivity, "exports")
                        if (result == resources.getString(R.string.pref_header_permission)) {
                            textPath.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            exportButton.visibility = View.VISIBLE
                            Toast.makeText(this@ExportActivity, getString(R.string.pref_header_export_failed), Toast.LENGTH_LONG).show()
                        } else {
                            val path = getString(R.string.pref_header_export_tag, result)
                            progressBar.visibility = View.GONE
                            exportButton.visibility = View.VISIBLE
                            textPath.visibility = View.VISIBLE
                            textPath.text = path
                            Toast.makeText(this@ExportActivity, getString(R.string.pref_header_export_successful), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(this.javaClass.name, "Export failed", e)
                    }

                }
            }, 1000)
        }

        checkWritePermissions(this)
    }

    private fun changeExportNote(name: String): String{
        var fileName = name
        fileName = fileName.replace("\\s".toRegex(), "_").trim { it <= ' ' }
        fileName = fileName.replace("\\.".toRegex(), "_").trim { it <= ' ' }
        if (result == exportOptions[0]) {
            fileName += ".txt"
        } else if (result == exportOptions[1]) {
            fileName += ".db"
        }

        return fileName
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this@ExportActivity, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        return true
    }


}
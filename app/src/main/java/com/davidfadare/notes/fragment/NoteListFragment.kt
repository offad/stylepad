package com.davidfadare.notes.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.ListFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.EditorActivity.Companion.PERMISSION_REQUEST_CODE
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.data.NoteViewModel
import com.davidfadare.notes.data.notedb.Note
import com.davidfadare.notes.recycler.NoteAdapter
import com.davidfadare.notes.settings.SettingsActivity
import com.davidfadare.notes.util.Utility.ExportUtil.exportDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class NoteListFragment : ListFragment() {

    private lateinit var mCallback: OnNoteSelectedListener
    private lateinit var adapter: NoteAdapter
    internal var fab: FloatingActionButton? = null
    private var notesTable = emptyList<Note>()

    private lateinit var noteModel: NoteViewModel

    interface OnNoteSelectedListener {
        fun onNoteSelected(id: Long)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val note = notesTable[position]

        var password: String? = note.password

        val syncConnPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_GEN_ENABLE, false)
        val passEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_PASSWORD_ENABLE, true)

        if (syncConnPref) {
            password = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_PREF_PASSWORD, "0")
        }

        if (TextUtils.isEmpty(password)) {
            mCallback.onNoteSelected(id)
        } else {
            if (passEnabled) {
                showPasswordCheckDialog(id, position, password, syncConnPref)
            } else {
                mCallback.onNoteSelected(id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val manager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val search = menu.findItem(R.id.search_action_view)
        var searchView: androidx.appcompat.widget.SearchView? = null

        try {
            searchView = MenuItemCompat.getActionView(search) as androidx.appcompat.widget.SearchView
        } catch (e: NullPointerException) {
            Log.e(tag, "Failed to initialise search functions.")
        }
        //TODO: Add filter. Requires pro.
        search?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                if (fab != null) {
                    fab!!.hide()
                }
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                if (fab != null) {
                    fab!!.show()
                }
                return true
            }
        })
        searchView?.setSearchableInfo(manager.getSearchableInfo(activity?.componentName))
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val notes = noteModel.mDatabaseRepository.queryOrderByText("%$query%").toTypedArray()

                if (notes.isNullOrEmpty()) {
                    Toast.makeText(activity, getString(R.string.list_view_note_not_found), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, getString(R.string.list_view_note_found, notes.size), Toast.LENGTH_LONG).show()
                }

                adapter = NoteAdapter(requireContext(), notes)
                listView.adapter = adapter
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val notes = noteModel.mDatabaseRepository.queryOrderByText("%$newText%").toTypedArray()

                adapter = NoteAdapter(requireContext(), notes)
                listView.adapter = adapter

                return false
            }
        })

        if (!notesTable.isNullOrEmpty()) {
            menu.findItem(R.id.delete_all).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> deleteAllNotes()
            R.id.nav_settings -> {
                val i = Intent(activity, OptionsActivity::class.java)
                startActivity(i)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        adapter = NoteAdapter(context, notesTable.toTypedArray())

        try {
            mCallback = context as OnNoteSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnNoteSelectedListener")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let {
            noteModel = ViewModelProviders.of(it).get(NoteViewModel::class.java)
        }

        noteModel.notesLiveData.observe(this, Observer {
            it?.let {
                notesTable = it
                adapter = NoteAdapter(requireContext(), notesTable.toTypedArray())
                listView.adapter = adapter
            }
        })

        adapter = NoteAdapter(requireContext(), notesTable.toTypedArray())

        val emptyView = activity?.findViewById<View>(R.id.empty_view)
        listView.emptyView = emptyView
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        fab = activity?.findViewById(R.id.fab)
        if (fab != null) {
            listView.setOnScrollListener(object : AbsListView.OnScrollListener {
                private var lastPosition = 0

                override fun onScrollStateChanged(absListView: AbsListView, i: Int) {}

                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    if (lastPosition == firstVisibleItem) {
                        return
                    }

                    if (firstVisibleItem > lastPosition) {
                        fab?.hide()
                    } else {
                        fab?.show()
                    }

                    lastPosition = firstVisibleItem
                }
            })
        }
    }

    private fun deleteAllNotes() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            val discardButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                val result = exportDatabase(getString(R.string.list_view_notes_deleted_name, Calendar.getInstance().timeInMillis), requireActivity(), "deleted")
                val view = activity?.findViewById<View>(R.id.coordinator_layout)
                if (result == resources.getString(R.string.pref_header_permission)) {
                    Snackbar.make(view!!, getString(R.string.pref_header_permission), Snackbar.LENGTH_LONG).show()
                } else {
                    val rowsDeleted = noteModel.mDatabaseRepository.deleteAll(*notesTable.toTypedArray())
                    Snackbar.make(view!!, getString(R.string.list_view_notes_delete_exported_name, rowsDeleted, result), Snackbar.LENGTH_LONG).show()
                }
            }

            showUnsavedChangesDialog(discardButtonClickListener)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(context, getString(R.string.pref_header_permission), Toast.LENGTH_LONG).show()
                } else {
                    deleteAllNotes()
                }
            }

            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showUnsavedChangesDialog(
            discardButtonClickListener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.clear_note_prompt)
        builder.setPositiveButton(R.string.ok, discardButtonClickListener)
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog?.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showPasswordCheckDialog(id: Long, position: Int, password: String?, masterEnabled: Boolean) {
        val builder = AlertDialog.Builder(context)
        val editText = EditText(context)
        editText.setSingleLine()

        val container: LinearLayout = LayoutInflater.from(context).inflate(R.layout.password_dialog, null) as LinearLayout
        container.addView(editText)

        builder.setView(container)

        if (masterEnabled)
            builder.setTitle(getString(R.string.key_enter_dialog_msg))
        else {
            builder.setTitle(getString(R.string.password_enter_dialog_msg))
        }
        builder.setPositiveButton(getString(R.string.ok)) { dialogInterface, _ ->
            if (editText.text.toString() == password) {

                mCallback.onNoteSelected(id)
                listView.setItemChecked(position, true)

            } else if (editText.text.toString() == getString(R.string.reset)) {
                for (note in notesTable) {
                    note.password = ""
                }
                val rowsAffected = noteModel.mDatabaseRepository.update(*notesTable.toTypedArray())

                if (rowsAffected == 0) {
                    Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.list_view_note_password_reset), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, getString(R.string.list_view_note_password_incorrect), Toast.LENGTH_SHORT).show()
                dialogInterface?.dismiss()
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog?.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

}

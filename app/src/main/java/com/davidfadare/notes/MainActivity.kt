package com.davidfadare.notes

import android.animation.Animator
import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.davidfadare.notes.billing.BillingViewModel
import com.davidfadare.notes.data.NoteViewModel
import com.davidfadare.notes.fragment.NoteListFragment
import com.davidfadare.notes.fragment.NoteViewerFragment
import com.davidfadare.notes.util.Utility.Companion.changeWindowColor
import com.davidfadare.notes.util.WidgetProvider
import com.davidfadare.notes.util.WidgetProvider.Companion.EXTRA_ITEM
import com.davidfadare.notes.util.WidgetProvider.Companion.EXTRA_NOTE_ID
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), NoteListFragment.OnNoteSelectedListener {
    private var fragmentManager: FragmentManager? = null
    private var mToolbar: Toolbar? = null
    private var info: Intent? = null
    val toolbar: Toolbar?
        get() {
            mToolbar = findViewById(R.id.main_toolbar)
            return mToolbar
        }

    private lateinit var noteModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MainTheme)

        changeWindowColor(this, window)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.app_bar_main)

        noteModel = ViewModelProviders.of(this).get(NoteViewModel::class.java)

        onSetup(savedInstanceState)

        val billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        billingViewModel.queryPurchases()

        MobileAds.initialize(this, resources.getString(R.string.app_ad_id))

        onSetupAds(billingViewModel)
    }

    private fun onSetup(savedInstanceState: Bundle?) {
        createNotificationChannel()
        mToolbar = findViewById(R.id.main_toolbar)
        if (mToolbar != null) {
            setSupportActionBar(mToolbar)
        }

        setTitle(R.string.my_notepad)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val fill = findViewById<View>(R.id.vAnimationFill)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && fill != null) {
                val location = IntArray(2)
                fab.getLocationOnScreen(location)
                val intent = Intent(this@MainActivity, EditorActivity::class.java)
                intent.putExtra("cx", location[0])
                intent.putExtra("cy", location[1])

                circularReveal(fill, location[0], location[1], intent)
            } else {
                val intent = Intent(this@MainActivity, EditorActivity::class.java)
                startActivity(intent)
            }
        }

        fragmentManager = supportFragmentManager
        if (getString(R.string.screen_type) == "phone") {
            fragmentManager!!.addOnBackStackChangedListener(object : FragmentManager.OnBackStackChangedListener {
                override fun onBackStackChanged() {
                    try {
                        if (fragmentManager!!.backStackEntryCount > 0) {
                            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                            fab.hide()
                        } else {
                            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                            fab.show()
                        }
                    } catch (e: NullPointerException) {
                        Log.e(this.javaClass.name, e.message)
                    }

                }
            })
        }

        if (findViewById<View>(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return
            }

            val noteListFragment = NoteListFragment()
            noteListFragment.arguments = intent.extras
            fragmentManager!!.beginTransaction()
                    .replace(R.id.fragment_container, noteListFragment).commit()

        }
    }

    private fun onSetupAds(billingViewModel: BillingViewModel) {
        val layout = findViewById<LinearLayout>(R.id.main_layout)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (fragmentManager!!.backStackEntryCount > 0) {
            if (mToolbar != null) {
                val mNoteColor = ContextCompat.getColor(this, R.color.blue_note)
                mToolbar!!.setBackgroundColor(mNoteColor)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val noteColor = ContextCompat.getColor(this@MainActivity, R.color.blue_note)
                    val window = window
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    window.statusBarColor = noteColor
                }

                try {
                    supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                } catch (e: NullPointerException) {
                    Log.e(this.javaClass.name, e.message)
                }

                fragmentManager!!.popBackStack()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (getString(R.string.screen_type) == "phone") {
            try {
                if (fragmentManager!!.backStackEntryCount > 0) {
                    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                } else {
                    supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                }
            } catch (e: NullPointerException) {
                Log.e(this.javaClass.name, e.message)
            }

        }
        val fill = findViewById<View>(R.id.vAnimationFill)
        if (fill != null) {
            fill.visibility = View.INVISIBLE
        }

        val listFragment: NoteListFragment? = fragmentManager?.findFragmentById(R.id.list_fragment) as NoteListFragment?

        if (listFragment != null) {
            fragmentManager?.beginTransaction()
                    ?.detach(listFragment)
                    ?.attach(listFragment)
                    ?.commit()
        }

        val fragment: NoteViewerFragment? = fragmentManager?.findFragmentById(R.id.viewer_fragment) as NoteViewerFragment?

        if (fragment != null) {
            fragmentManager?.beginTransaction()
                    ?.detach(fragment)
                    ?.attach(fragment)
                    ?.commit()
        }

        when (info?.action) {
            WidgetProvider.OPEN_ACTION -> {
                val extras: Bundle? = info?.extras
                if (extras != null) {
                    val id = extras.getLong(EXTRA_NOTE_ID, 0)
                    val position = extras.getInt(EXTRA_ITEM, 0)
                    var frag = supportFragmentManager.findFragmentById(R.id.list_fragment)
                    if (getString(R.string.screen_type) == "phone") {
                        frag = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    }
                    if (frag is NoteListFragment) {
                        frag.onListItemClick(frag.listView, frag.listView.getChildAt(position), position, id)
                        info = null
                    }
                }
            }
            WidgetProvider.NEW_ACTION -> {
                val fab = findViewById<FloatingActionButton>(R.id.fab)
                fab.performClick()
                info = null
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        info = intent
        super.onNewIntent(intent)
    }

    override fun onNoteSelected(id: Long) {
        noteModel.notePosition.postValue(id)
        val articleFrag: NoteViewerFragment? = supportFragmentManager.findFragmentById(R.id.viewer_fragment) as NoteViewerFragment?
        if (articleFrag != null) {
            articleFrag.updateNoteView(id)
        } else {

            val newFragment = NoteViewerFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(R.anim.enter, R.anim.exit)
            transaction.replace(R.id.fragment_container, newFragment)
            transaction.addToBackStack(null)

            transaction.commit()
        }
    }

    @TargetApi(21)
    private fun circularReveal(view: View, expansionPointX: Int, expansionPointY: Int, intent: Intent) {
        val shortAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val finalRadius = maxOf(view.width, view.height).toFloat()
        view.visibility = View.VISIBLE
        val mNoteColor = ContextCompat.getColor(this, R.color.blue_note)
        view.setBackgroundColor(mNoteColor)
        val circularReveal = ViewAnimationUtils.createCircularReveal(view, expansionPointX, expansionPointY, 0f, finalRadius * 1.1f)
        circularReveal.duration = shortAnimationDuration
        circularReveal.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {

            }

            override fun onAnimationEnd(animator: Animator) {
                startActivity(intent)
            }

            override fun onAnimationCancel(animator: Animator) {

            }

            override fun onAnimationRepeat(animator: Animator) {

            }
        })
        circularReveal.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.editor_alarm_channel_name)
            val descriptionText = getString(R.string.editor_alarm_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(name, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            notificationManager.createNotificationChannel(channel)
        }
    }
}


package com.davidfadare.notes.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davidfadare.notes.OptionsActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.billing.BillingViewModel
import com.davidfadare.notes.billing.localdb.AugmentedSkuDetails
import com.davidfadare.notes.recycler.SkuDetailsAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class PurchaseActivity : AppCompatActivity() {

    private lateinit var billingViewModel: BillingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_purchase)
        val inappAdapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
                onPurchase(item)
            }
        }
        val skuRecycler = findViewById<RecyclerView>(R.id.inapp_inventory)
        attachAdapterToRecyclerView(skuRecycler, inappAdapter)

        billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        billingViewModel.queryPurchases()
        billingViewModel.inappSkuDetailsListLiveData.observe(this, Observer {
            it?.let { inappAdapter.setSkuDetailsList(it) }
        })

        onSetupAds()
    }

    private fun attachAdapterToRecyclerView(recyclerView: RecyclerView, skuAdapter: SkuDetailsAdapter) {
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = skuAdapter
        }
    }

    private fun onSetupAds() {
        val layout = findViewById<LinearLayoutCompat>(R.id.purchase_layout)

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

    private fun onPurchase(item: AugmentedSkuDetails) {
        billingViewModel.makePurchase(this as Activity, item)
        Log.d(this.toString(), "starting purchase flow for SkuDetail:\n ${item}")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startActivity(Intent(this, OptionsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        return true
    }
}

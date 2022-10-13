package com.woocommerce.android.iapshowcase

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory

class IAPShowcaseActivity : AppCompatActivity() {
    private val iapManager = IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
        IAPShowcaseNetwork(),
        this,
        IAPDebugLogWrapper()
    )

    private val viewModel: IAPShowcaseViewModel by viewModels {
        IAPShowcaseViewModel.Factory(iapManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iapshowcase)

        setupObservers()

        findViewById<Button>(R.id.btnStartPurchase).setOnClickListener {
            viewModel.purchasePlan()
        }
        findViewById<Button>(R.id.btnFetchAgain).setOnClickListener {
            viewModel.fetchProductAndPurchaseInfo()
        }
    }

    private fun setupObservers() {
        viewModel.purchaseStatusInfo.observe(this) {
            findViewById<TextView>(R.id.tvPurchaseStatusInfo).text = it
        }
        viewModel.productInfo.observe(this) {
            findViewById<TextView>(R.id.tvProductInfoTitle).text = it.localizedTitle
            findViewById<TextView>(R.id.tvProductInfoDescription).text = it.localizedDescription
            findViewById<TextView>(R.id.tvProductInfoPrice).text = it.displayPrice
        }
        viewModel.productInfoFetchingError.observe(this) {
            findViewById<TextView>(R.id.tvProductInfoTitle).text = null
            findViewById<TextView>(R.id.tvProductInfoDescription).text = null
            findViewById<TextView>(R.id.tvProductInfoPrice).text = null
            findViewById<TextView>(R.id.tvProductInfoError).text = it
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchPurchases()
    }
}

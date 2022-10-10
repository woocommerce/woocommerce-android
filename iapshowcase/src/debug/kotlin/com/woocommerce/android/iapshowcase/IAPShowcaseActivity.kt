package com.woocommerce.android.iapshowcase

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.public.IAPSitePurchasePlanFactory

class IAPShowcaseActivity : AppCompatActivity() {
    private val iapManager = IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
        IAPShowcaseStore(),
        this,
        IAPDebugLogWrapper()
    )

    private val viewModel: IAPShowcaseViewModel by viewModels {
        IAPShowcaseViewModel.Factory(iapManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iapshowcase)

        findViewById<Button>(R.id.btnStartPurchase).setOnClickListener {
            viewModel.purchasePlan()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchPurchases()
    }
}

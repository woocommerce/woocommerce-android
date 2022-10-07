package com.woocommerce.android.iapshowcase

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.public.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.public.LogWrapper
import com.woocommerce.android.iap.public.model.IAPProduct
import fake.`package`.name.`for`.sync.R

class IAPShowcaseActivity : AppCompatActivity() {
    private val logWrapper = object : LogWrapper {
        override fun w(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun d(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun e(tag: String, message: String) {
            Log.e(tag, message)
        }
    }

    private val iapManager = IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
        IAPShowcaseStore(),
        this,
        logWrapper
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

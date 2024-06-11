package com.woocommerce.android.iapshowcase

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.iapshowcase.purchase.IAPShowcasePurchaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IAPShowcaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iapshowcase)
    }

    fun openIAPPurchaseFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcIapShowcase, IAPShowcasePurchaseFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }
}

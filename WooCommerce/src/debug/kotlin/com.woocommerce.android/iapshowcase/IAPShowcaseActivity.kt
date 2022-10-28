package com.woocommerce.android.iapshowcase

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.R
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory

private const val MILLION = 1_000_000.0

class IAPShowcaseActivity : AppCompatActivity() {
    private val viewModel: IAPShowcaseViewModel by viewModels(null) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                IAPShowcaseViewModel(IapManagerProvider.instance) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iapshowcase)

        setupObservers()

        IapManagerProvider.instance.initIAPWithNewActivity(this)

        findViewById<Button>(R.id.btnFetchProductInfo).setOnClickListener {
            viewModel.fetchWPComPlanProduct()
        }
        findViewById<Button>(R.id.btnStartPurchase).setOnClickListener {
            viewModel.purchasePlan(findViewById<EditText>(R.id.etRemoteBlogId).text?.toString()?.toLong() ?: 1L)
        }
        findViewById<Button>(R.id.btnCheckIfPlanPurchased).setOnClickListener {
            viewModel.checkIfWPComPlanPurchased()
        }
        findViewById<Button>(R.id.btnCheckIfIapSupported).setOnClickListener {
            viewModel.checkIfIAPSupported()
        }
    }

    private fun setupObservers() {
        viewModel.productInfo.observe(this) {
            findViewById<TextView>(R.id.tvProductInfoTitle).text = it.localizedTitle
            findViewById<TextView>(R.id.tvProductInfoDescription).text = it.localizedDescription
            findViewById<TextView>(R.id.tvProductInfoPrice).text = "${it.price / MILLION} ${it.currency}"
        }
        viewModel.iapEvent.observe(this) {
            Log.w("IAP_SHOWCASE", it)
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }
}

object IapManagerProvider {
    val instance = IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(IAPDebugLogWrapper())
}

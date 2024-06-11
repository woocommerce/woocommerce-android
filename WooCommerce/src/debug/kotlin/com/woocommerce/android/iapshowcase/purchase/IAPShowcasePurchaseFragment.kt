package com.woocommerce.android.iapshowcase.purchase

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.R
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.pub.network.SandboxTestingConfig
import com.woocommerce.android.iapshowcase.IAPDebugLogWrapper
import com.woocommerce.android.iapshowcase.IapMobilePayApiProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IAPShowcasePurchaseFragment : Fragment(R.layout.fragment_iap_showcase_purchase) {
    @Inject
    lateinit var mobilePayAPIProvider: IapMobilePayApiProvider

    @Inject
    lateinit var debugLogWrapper: IAPDebugLogWrapper

    private val viewModel: IAPShowcasePurchaseViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IAPShowcasePurchaseViewModel(
                    IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
                        this@IAPShowcasePurchaseFragment.requireActivity().application,
                        debugLogWrapper,
                        mobilePayAPIProvider::buildMobilePayAPI,
                        object : SandboxTestingConfig {
                            override val isDebug: Boolean
                                get() = true
                            override val iapTestingSandboxUrl: String
                                get() = ""
                        }
                    )
                ) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(view)
        setupObservers(view)

        view.findViewById<Button>(R.id.btnFetchProductInfo).setOnClickListener {
            viewModel.fetchWPComPlanProduct()
        }
        view.findViewById<Button>(R.id.btnStartPurchase).setOnClickListener {
            viewModel.purchasePlan(IAPActivityWrapper(requireActivity() as AppCompatActivity))
        }
        view.findViewById<Button>(R.id.btnCheckIfPlanPurchased).setOnClickListener {
            viewModel.checkIfWPComPlanPurchased()
        }
    }

    private fun setupToolbar(view: View) {
        with(view.findViewById<Toolbar>(R.id.tbBack)) {
            setNavigationOnClickListener {
                this@IAPShowcasePurchaseFragment.requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupObservers(view: View) {
        viewModel.productInfo.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tvProductInfoTitle).text = it.localizedTitle
            view.findViewById<TextView>(R.id.tvProductInfoDescription).text = it.localizedDescription
            view.findViewById<TextView>(R.id.tvProductInfoPrice).text = "${it.price} ${it.currency}"
        }
        viewModel.iapEvent.observe(viewLifecycleOwner) {
            Log.w("IAP_SHOWCASE", it)
            Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
        }
        viewModel.iapLoading.observe(viewLifecycleOwner) {
            view.findViewById<View>(R.id.lpiLoading).isVisible = it
        }
    }

    companion object {
        fun newInstance() = IAPShowcasePurchaseFragment()
    }
}

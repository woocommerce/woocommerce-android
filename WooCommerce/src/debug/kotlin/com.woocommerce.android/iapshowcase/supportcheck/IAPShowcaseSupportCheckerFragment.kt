package com.woocommerce.android.iapshowcase.supportcheck

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.R
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iapshowcase.IAPDebugLogWrapper
import com.woocommerce.android.iapshowcase.IAPShowcaseActivity

class IAPShowcaseSupportCheckerFragment : Fragment(R.layout.fragment_iap_showcase_support_checker) {
    private val viewModel: IAPShowcaseSupportCheckerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                IAPShowcaseSupportCheckerViewModel(
                    IAPSitePurchasePlanFactory.createIAPPurchaseWpComPlanSupportChecker(
                        this@IAPShowcaseSupportCheckerFragment.requireActivity().application,
                        IAPDebugLogWrapper(),
                    )
                ) as T
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers(view)

        view.findViewById<Button>(R.id.btnCheckIfIapSupported).setOnClickListener {
            viewModel.checkIfIAPSupported()
        }
        view.findViewById<Button>(R.id.btnStartPurchaseFlow).setOnClickListener {
            (requireActivity() as IAPShowcaseActivity).openIAPPurchaseFragment()
        }
    }

    private fun setupObservers(view: View) {
        viewModel.iapEvent.observe(viewLifecycleOwner) {
            Log.w("IAP_SHOWCASE", it)
            Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
        }
        viewModel.iapLoading.observe(viewLifecycleOwner) {
            view.findViewById<View>(R.id.lpiLoading).isVisible = it
        }
    }
}

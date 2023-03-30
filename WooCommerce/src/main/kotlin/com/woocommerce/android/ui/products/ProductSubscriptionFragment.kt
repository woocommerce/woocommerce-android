package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductSubscriptionBinding
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductSubscriptions
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductSubscriptionFragment : BaseProductFragment(R.layout.fragment_product_subscription) {
    companion object {
        val TAG: String = ProductSubscriptionFragment::class.java.simpleName
    }

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    private val navArgs: ProductSubscriptionFragmentArgs by navArgs()

    private var _binding: FragmentProductSubscriptionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductSubscriptionBinding.bind(view)
        viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
        initializeViews(navArgs.subscription)
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitProductSubscriptions -> findNavController().navigateUp()
            else -> event.isHandled = false
        }
    }

    private fun initializeViews(subscription: SubscriptionDetails) {
        val period = subscription.period.getPeriodString(requireContext(), subscription.periodInterval)
        with(binding.priceValue) {
            text = getString(
                R.string.product_subscription_description,
                currencyFormatter.formatCurrency(subscription.price, viewModel.currencyCode, true),
                subscription.periodInterval.toString(),
                period
            )
        }
        binding.expireValue.text = if (subscription.length != null) {
            getString(R.string.subscription_period, subscription.length.toString(), period)
        } else {
            getString(R.string.subscription_never_expire)
        }
        binding.feeValue.text = if (subscription.signUpFee != null) {
            currencyFormatter.formatCurrency(subscription.signUpFee, viewModel.currencyCode, true)
        } else {
            getString(R.string.subscription_no_sign_up_fee)
        }
        binding.freeTrialValue.text = if (subscription.trialLength != null && subscription.trialPeriod != null) {
            getString(
                R.string.subscription_period,
                subscription.trialLength.toString(),
                subscription.trialPeriod.getPeriodString(requireContext(), subscription.trialLength)
            )
        } else {
            getString(R.string.subscription_no_trial)
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_subscription_title)

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductSubscriptions)
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

package com.woocommerce.android.ui.products

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductSubscriptionBinding
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductSubscriptions
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Date
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
        initializeViews(navArgs.subscription, navArgs.saleDetails)
    }

    private fun onEventReceived(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitProductSubscriptions -> findNavController().navigateUp()
            else -> event.isHandled = false
        }
    }

    private fun initializeViews(subscription: SubscriptionDetails, sale: SaleDetails?) {
        val period = subscription.period.getPeriodString(requireContext(), subscription.periodInterval)
        initializeSubscriptionDetails(subscription, period)
        initializeSaleDetails(sale, period, subscription.periodInterval.toString())
    }

    private fun initializeSubscriptionDetails(subscription: SubscriptionDetails, period: String) {
        with(binding.priceValue) {
            text = getString(
                R.string.product_subscription_description,
                currencyFormatter.formatCurrency(subscription.price, viewModel.currencyCode, true),
                subscription.periodInterval.toString(),
                period
            )
        }
        binding.expireValue.text = if (subscription.length != null && subscription.length > 0) {
            getString(R.string.subscription_period, subscription.length.toString(), period)
        } else {
            getString(R.string.subscription_never_expire)
        }
        binding.feeValue.text = if (subscription.signUpFee != null && subscription.signUpFee > BigDecimal.ZERO) {
            currencyFormatter.formatCurrency(subscription.signUpFee, viewModel.currencyCode, true)
        } else {
            getString(R.string.subscription_no_sign_up_fee)
        }
        binding.freeTrialValue.text =
            if (subscription.trialLength != null && subscription.trialPeriod != null && subscription.trialLength > 0) {
                getString(
                    R.string.subscription_period,
                    subscription.trialLength.toString(),
                    subscription.trialPeriod.getPeriodString(requireContext(), subscription.trialLength)
                )
            } else {
                getString(R.string.subscription_no_trial)
            }
    }

    private fun initializeSaleDetails(sale: SaleDetails?, period: String, periodInterval: String) {
        sale?.let {
            binding.saleTitle.isVisible = true
            binding.saleDivider.isVisible = true
            binding.saleSpace.isVisible = sale.salePrice != null && sale.isSaleScheduled
            if (sale.salePrice != null) {
                with(binding.saleValue) {
                    isVisible = true
                    text = getString(
                        R.string.product_subscription_description,
                        currencyFormatter.formatCurrency(sale.salePrice, viewModel.currencyCode, true),
                        periodInterval,
                        period
                    )
                }
            }
            if (sale.isSaleScheduled) {
                with(binding.saleDates) {
                    isVisible = true
                    text = getSalesDates(sale)
                }
            }
        }
    }

    private fun getSalesDates(sale: SaleDetails): String? {
        return when {
            // only start date is set
            (sale.saleStartDateGmt != null && sale.saleEndDateGmt == null) -> {
                resources.getString(R.string.product_sale_date_from, sale.saleStartDateGmt.formatToMMMddYYYY())
            }
            // only end date is set
            (sale.saleStartDateGmt == null && sale.saleEndDateGmt != null) -> {
                resources.getString(R.string.product_sale_date_to, sale.saleEndDateGmt.formatToMMMddYYYY())
            }
            // both dates are set
            (sale.saleStartDateGmt != null && sale.saleEndDateGmt != null) -> {
                resources.getString(
                    R.string.product_sale_date_from_to,
                    sale.saleStartDateGmt.formatToMMMddYYYY(),
                    sale.saleEndDateGmt.formatToMMMddYYYY()
                )
            }
            else -> null
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

@Parcelize
data class SaleDetails(
    val salePrice: BigDecimal?,
    val isSaleScheduled: Boolean,
    val saleStartDateGmt: Date?,
    val saleEndDateGmt: Date?
) : Parcelable

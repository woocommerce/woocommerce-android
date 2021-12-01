package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogSimplePaymentsBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsDialog : DialogFragment(R.layout.dialog_simple_payments) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: SimplePaymentsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            val isLandscape = DisplayUtils.isLandscape(requireContext())
            val widthRatio = if (isLandscape) WIDTH_RATIO_LANDSCAPE else WIDTH_RATIO
            val heightRatio = if (isLandscape) HEIGHT_RATIO_LANDSCAPE else HEIGHT_RATIO

            window.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * widthRatio).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * heightRatio).toInt()
            )
        }

        val binding = DialogSimplePaymentsBinding.bind(view)
        binding.editPrice.initView(viewModel.currencyCode, viewModel.decimals, currencyFormatter)
        binding.buttonDone.setOnClickListener {
            viewModel.onDoneButtonClicked()
        }
        binding.imageClose.setOnClickListener {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIMPLE_PAYMENTS_FLOW_CANCELED)
            findNavController().navigateUp()
        }

        setupObservers(binding)
    }

    private fun setupObservers(binding: DialogSimplePaymentsBinding) {
        binding.editPrice.value.observe(
            this,
            {
                viewModel.currentPrice = it
            }
        )

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    ActivityUtils.hideKeyboardForced(binding.editPrice)
                    uiMessageResolver.showSnack(event.message)
                }
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }
            new.createdOrder.takeIfNotEqualTo(old?.createdOrder) { order ->
                navigateBackWithResult(KEY_SIMPLE_PAYMENTS_RESULT, order!!)
            }
            new.isProgressShowing.takeIfNotEqualTo(old?.isProgressShowing) { show ->
                binding.progressBar.isVisible = show
                binding.buttonDone.text = if (show) "" else getString(R.string.done)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    companion object {
        const val KEY_SIMPLE_PAYMENTS_RESULT = "simple_payments_result"
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
        private const val HEIGHT_RATIO_LANDSCAPE = 0.9
        private const val WIDTH_RATIO_LANDSCAPE = 0.6
    }
}

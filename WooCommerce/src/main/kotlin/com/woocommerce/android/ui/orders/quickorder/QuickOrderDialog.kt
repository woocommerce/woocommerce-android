package com.woocommerce.android.ui.orders.quickorder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogQuickOrderBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class QuickOrderDialog : DialogFragment(R.layout.dialog_quick_order) {
    @Inject internal lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: QuickOrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            window.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * WIDTH_RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * HEIGHT_RATIO).toInt()
            )
        }

        val binding = DialogQuickOrderBinding.bind(view)
        binding.editPrice.initView(viewModel.currencyCode, viewModel.decimals, currencyFormatter)
        binding.buttonDone.setOnClickListener {
            // TODO nbradbury create the order
        }
        binding.imageClose.setOnClickListener {
            findNavController().navigateUp()
        }

        setupObservers(binding)
    }

    private fun setupObservers(binding: DialogQuickOrderBinding) {
        binding.editPrice.value.observe(
            this,
            {
                viewModel.currentPrice = it
            }
        )

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }
            new.createdOrder.takeIfNotEqualTo(old?.createdOrder) { order ->
                returnResult(order!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun returnResult(order: Order) {
        navigateBackWithResult(KEY_QUICK_ORDER_RESULT, order)
    }

    companion object {
        const val KEY_QUICK_ORDER_RESULT = "quick_order_result"
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
    }
}

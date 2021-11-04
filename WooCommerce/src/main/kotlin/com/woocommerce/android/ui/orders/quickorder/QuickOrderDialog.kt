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
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class QuickOrderDialog : DialogFragment(R.layout.dialog_quick_order) {
    @Inject
    internal lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: QuickOrderViewModel by viewModels()
    private var currentPrice = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            window.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * RATIO).toInt()
            )
        }

        val binding = DialogQuickOrderBinding.bind(view)
        binding.imageClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.editPrice.initView(viewModel.currencyCode, viewModel.decimals, currencyFormatter)
        binding.editPrice.value.observe(
            this,
            {
                binding.buttonDone.isEnabled = it > BigDecimal.ZERO
                currentPrice = it
            }
        )

        binding.buttonDone.setOnClickListener {
            returnResult()
        }

        ActivityUtils.showKeyboard(binding.editPrice)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun returnResult() {
        navigateBackWithResult(KEY_QUICK_ORDER_RESULT, currentPrice)
    }

    companion object {
        const val KEY_QUICK_ORDER_RESULT = "quick_order_result"
        private const val RATIO = 0.95 // TODO nbradbury tablet?
    }
}

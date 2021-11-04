package com.woocommerce.android.ui.orders.quickorder

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogQuickOrderBinding
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class QuickOrderDialog : DialogFragment(R.layout.dialog_quick_order) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val navArgs: QuickOrderDialogArgs by navArgs()
    private var currentPrice = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireDialog().setTitle(R.string.quickorder_dialog_title)
        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            window.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * RATIO).toInt()
            )
        }

        val binding = DialogQuickOrderBinding.bind(view)

        val toolbar = binding.toolbar.toolbar
        toolbar.setTitle(R.string.quickorder_dialog_title)
        toolbar.navigationIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_gridicons_cross_24dp)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.editPrice.initView(navArgs.currency, navArgs.decimals, currencyFormatter)
        binding.editPrice.setValue(BigDecimal.ZERO)
        binding.editPrice.value.observe(
            this,
            Observer {
                binding.buttonDone.isEnabled = it > BigDecimal.ZERO
                currentPrice = it
            }
        )

        ActivityUtils.showKeyboard(binding.editPrice)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    companion object {
        const val RATIO = 0.95
    }
}

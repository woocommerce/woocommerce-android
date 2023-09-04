package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaxRateSelectorFragment : Fragment() {
    private val viewModel: TaxRateSelectorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                TaxRateSelectorScreen(
                    viewModel.viewState,
                    viewModel::onEditTaxRatesInAdminClicked,
                    viewModel::onInfoIconClicked,
                    viewModel::onTaxRateSelected,
                )
            }
        }
    }
}

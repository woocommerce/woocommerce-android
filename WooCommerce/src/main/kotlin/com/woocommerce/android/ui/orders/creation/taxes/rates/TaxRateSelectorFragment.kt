package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorFragmentDirections.Companion.actionTaxRateSelectorFragmentToTaxRatesInfoDialogFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaxRateSelectorFragment : BaseFragment() {
    private val viewModel: TaxRateSelectorViewModel by viewModels<TaxRateSelectorViewModel>()
    private val args: TaxRateSelectorFragmentArgs by navArgs()

    override val activityAppBarStatus = AppBarStatus.Hidden

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
                    viewModel::onDismissed,
                    viewModel::onLoadMore,
                    viewModel::onEditTaxRatesInAdminClicked,
                    viewModel::onAutoRateSwitchStateChanged,
                )
            }
        }
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TaxRateSelectorViewModel.TaxRateSelected -> {
                    navigateBackWithResult(KEY_SELECTED_TAX_RATE, event.taxRate)
                }
                is TaxRateSelectorViewModel.EditTaxRatesInAdmin -> {
                    args.dialogState.taxRatesSettingsUrl.let {
                        ChromeCustomTabUtils.launchUrl(requireContext(), it)
                        findNavController().navigateUp()
                    }
                }
                is TaxRateSelectorViewModel.ShowTaxesInfoDialog -> {
                    actionTaxRateSelectorFragmentToTaxRatesInfoDialogFragment(args.dialogState).also {
                        findNavController().navigate(it)
                    }
                }
                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    companion object {
        const val KEY_SELECTED_TAX_RATE = "key_selected_tax_rate"
    }
}

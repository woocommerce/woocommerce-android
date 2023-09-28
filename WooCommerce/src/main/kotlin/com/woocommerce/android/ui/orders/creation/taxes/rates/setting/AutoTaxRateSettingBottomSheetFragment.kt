package com.woocommerce.android.ui.orders.creation.taxes.rates.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutoTaxRateSettingBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val sharedViewModel: OrderCreateEditViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_creations)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    AutoTaxRateSettingBottomSheetScreen(
                        sharedViewModel.viewStateData.liveData.observeAsState(),
                        {
                            dismiss()
                            sharedViewModel.onSetNewTaxRateClicked()
                        },
                        {
                            dismiss()
                            sharedViewModel.onStopUsingTaxRateClicked()
                        }
                    )
                }
            }
        }
    }
}

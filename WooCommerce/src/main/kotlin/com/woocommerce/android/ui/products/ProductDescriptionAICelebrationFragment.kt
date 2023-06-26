package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class ProductDescriptionAICelebrationFragment : WCBottomSheetDialogFragment() {
    private val viewModel: ProductDescriptionAICelebrationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    ProductDescriptionAICelebrationBottomSheet(viewModel = viewModel)
                }
            }
        }
    }
}

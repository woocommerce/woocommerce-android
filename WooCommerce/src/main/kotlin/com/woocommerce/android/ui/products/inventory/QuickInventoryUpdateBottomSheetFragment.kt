package com.woocommerce.android.ui.products.inventory

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class QuickInventoryUpdateBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val viewModel: ScanToUpdateInventoryViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_main)
    private val args: QuickInventoryUpdateBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooTheme {
                    QuickInventoryUpdateBottomSheet(args.productInfo)
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onBottomSheetDismissed()
    }
}

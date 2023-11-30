package com.woocommerce.android.ui.products.inventory

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BarcodeScanningFragment() {
    private val viewModel: ScanToUpdateInventoryViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_main)

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override val isContinuousScanningEnabled = true

    override fun onScannedResult(status: CodeScannerStatus) {
        viewModel.onBarcodeScanningResult(status)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is MultiLiveEvent.Event.ShowUiStringSnackbar -> {
                    uiMessageResolver.showSnack(it.message)
                }
//                is ScanToUpdateInventoryViewModel.OpenInventoryUpdateBottomSheet -> {
//                    findNavController()
//                        .navigate(actionScanToUpdateInventoryBarcodeScannerFragmentToQuickInventoryUpdateBottomSheet())
//                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun getScannerOverlay(): @Composable BoxScope.() -> Unit = {
        val viewState = viewModel.viewState.collectAsState()
        val isBottomSheetShown = viewState.value is ScanToUpdateInventoryViewModel.ViewState.ProductLoaded
        if (isBottomSheetShown) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = viewModel::onBottomSheetDismissed
            ) {
                QuickInventoryUpdateBottomSheet(
                    state = viewState,
                    onIncrementQuantityClicked = { /*TODO*/ },
                    onManualQuantityEntered = {},
                    onUpdateQuantityClicked = {}
                )
            }
        }
    }
}

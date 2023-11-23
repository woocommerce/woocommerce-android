package com.woocommerce.android.ui.products.inventory

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryBarcodeScannerFragmentDirections.Companion.actionScanToUpdateInventoryBarcodeScannerFragmentToQuickInventoryUpdateBottomSheet
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
        lifecycleScope.launch {
            viewModel.viewState.collect {
                if (it is ScanToUpdateInventoryViewModel.ViewState.Result) {
                    actionScanToUpdateInventoryBarcodeScannerFragmentToQuickInventoryUpdateBottomSheet(
                        it.product
                    ).let {
                        findNavController().navigate(it)
                    }
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is MultiLiveEvent.Event.ShowUiStringSnackbar -> {
                    uiMessageResolver.showSnack(it.message)
                }
            }
        }
    }
}

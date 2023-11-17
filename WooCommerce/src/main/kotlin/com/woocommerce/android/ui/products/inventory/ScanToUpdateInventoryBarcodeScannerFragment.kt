package com.woocommerce.android.ui.products.inventory

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryBarcodeScannerFragmentDirections.Companion.actionScanToUpdateInventoryBarcodeScannerFragmentToQuickInventoryUpdateBottomSheet
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BarcodeScanningFragment() {
    private val viewModel: ScanToUpdateInventoryViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_main)

    override val isContinuousScanningEnabled = true

    override fun onScannedResult(status: CodeScannerStatus) {
        viewModel.onBarcodeScanningResult(status)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is ScanToUpdateInventoryViewModel.OpenQuickInventoryUpdateBottomSheet -> {
                    actionScanToUpdateInventoryBarcodeScannerFragmentToQuickInventoryUpdateBottomSheet().let {
                        findNavController().navigate(it)
                    }
                }
            }
        }
    }
}

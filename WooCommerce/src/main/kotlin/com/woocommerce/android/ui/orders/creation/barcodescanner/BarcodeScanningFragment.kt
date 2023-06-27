package com.woocommerce.android.ui.orders.creation.barcodescanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BarcodeScanningFragment : BaseFragment(R.layout.fragment_barcode_scanning) {

    @Inject lateinit var codeScanner: GoogleMLKitCodeScanner
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    BarcodeScannerScreen(codeScanner) { codeScannerStatus ->
                        lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                                codeScannerStatus.collect { status ->
                                    navigateBackWithResult(KEY_BARCODE_SCANNING_SCAN_STATUS, status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
    }
}

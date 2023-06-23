package com.woocommerce.android.ui.orders.creation.barcodescanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class BarcodeScanningFragment : BaseFragment(R.layout.fragment_barcode_scanning) {

    private val viewModel: BarcodeScanningViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    BarcodeScannerScreen {
                        viewModel.startScan(it)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
               is MultiLiveEvent.Event.ExitWithResult<*> -> {
                   val data = when (event.data) {
                       is CodeScannerStatus.Success -> {
                           ToastUtils.showToast(context, event.data.code).show()
                           event.data.code
                       }
                       is CodeScannerStatus.Failure -> {
                           event.data.error
                       }
                       else -> {
                           null
                       }
                   }
                   navigateBackWithResult("barcode", data)
               }
            }
        }
    }
}

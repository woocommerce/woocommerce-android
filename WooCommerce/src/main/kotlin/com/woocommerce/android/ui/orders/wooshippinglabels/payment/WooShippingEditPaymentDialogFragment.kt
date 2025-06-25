package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.SnackbarHostState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WooShippingEditPaymentDialogFragment : WCBottomSheetDialogFragment() {
    private val viewModel: WooShippingEditPaymentViewModel by viewModels()

    private val snackbarHostState = SnackbarHostState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                WooTheme {
                    WooShippingEditPaymentScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
        disableDraggingOnWebViewState()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> showSnackbar(getString(event.message))
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun showSnackbar(message: String) {
        viewLifecycleOwner.lifecycleScope.launch { snackbarHostState.showSnackbar(message) }
    }

    private fun disableDraggingOnWebViewState() {
        viewModel.viewState.map { it.javaClass }
            .distinctUntilChanged()
            .observe(viewLifecycleOwner) {
                val dialog = requireDialog() as BottomSheetDialog
                dialog.behavior.isDraggable =
                    it != WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView::class.java
            }
    }
}

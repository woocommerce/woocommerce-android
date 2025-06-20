package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooShippingEditPaymentDialogFragment : WCBottomSheetDialogFragment() {
    private val viewModel: WooShippingEditPaymentViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                WooTheme {
                    WooShippingEditPaymentScreen(
                        viewModel = viewModel
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
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
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

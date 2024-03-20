package com.woocommerce.android.ui.products.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class ProductSelectorDialogFragment : DialogFragment() {
    companion object {
        const val PRODUCT_SELECTOR_RESULT = "product-selector-result"
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.55f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.6f
    }

    @Inject lateinit var navigator: ProductNavigator

    private val viewModel: ProductSelectorViewModel by viewModels()

    private val args by navArgs<ProductSelectorDialogFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog_RoundedCorners_NoMinWidth)
        } else {
            /* This draws the dialog as full screen */
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }

        if (args.selectionHandling != ProductSelectorViewModel.SelectionHandling.SIMPLE) {
            // If we want to support the other handling, we need to make all of the destinations as dialogs
            error("The dialog version of the product selector supports only simple selection handling")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.product_selector_compose_view

            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    ProductSelectorScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            dialog?.window?.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> {
                    navigateBackWithResult(
                        PRODUCT_SELECTOR_RESULT,
                        event.data as Collection<SelectedItem>
                    )
                }
                is ProductNavigationTarget -> navigator.navigate(this, event)
                is Exit -> findNavController().navigateUp()
            }
        }
    }
}

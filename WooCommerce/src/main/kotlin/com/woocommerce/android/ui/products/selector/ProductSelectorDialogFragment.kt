package com.woocommerce.android.ui.products.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.edgeToEdgeForInLandscape
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductSelectorDialogFragment : DialogFragment() {
    companion object {
        const val PRODUCT_SELECTOR_RESULT = "product-selector-result"
    }

    @Inject
    lateinit var navigator: ProductNavigator

    private val viewModel: ProductSelectorViewModel by viewModels()

    private val args by navArgs<ProductSelectorDialogFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Slide)

        if (args.selectionHandling != ProductSelectorViewModel.SelectionHandling.SIMPLE) {
            // If we want to support the other handling, we need to make all of the destinations as dialogs
            error("The dialog version of the product selector supports only simple selection handling")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.product_selector_compose_view

            edgeToEdgeForInLandscape()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    ProductSelectorScreen(viewModel = viewModel, handleInsets = true)
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
        dialog?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
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

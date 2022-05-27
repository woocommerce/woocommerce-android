package com.woocommerce.android.ui.products.variations.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.VariationSelectionResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates.observable

@AndroidEntryPoint
class VariationSelectorFragment : BaseFragment(), BackPressListener {
    companion object {
        const val VARIATION_SELECTOR_RESULT = "variation-selector-result"
    }

    private val viewModel: VariationSelectorViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            hasShadow = false
        )

    private var screenTitle: String by observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateActivityTitle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    VariationSelectorScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewSate.observe(viewLifecycleOwner) {
            screenTitle = it.productName
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> {
                    navigateBackWithResult(VARIATION_SELECTOR_RESULT, event.data as VariationSelectionResult)
                }
            }
        }
    }

    override fun getFragmentTitle() = screenTitle

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackPress()
        return false
    }
}

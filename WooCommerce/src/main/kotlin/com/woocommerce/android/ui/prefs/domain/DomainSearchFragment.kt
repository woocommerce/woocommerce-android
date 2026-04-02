package com.woocommerce.android.ui.prefs.domain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.prefs.domain.DomainSearchViewModel.NavigateToDomainRegistration
import com.woocommerce.android.ui.prefs.domain.DomainSearchViewModel.ShowCheckoutWebView
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DomainSearchFragment : BaseFragment() {
    private val viewModel: DomainSearchViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    DomainPickerScreen(viewModel, viewModel::onDomainSuggestionSelected)
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
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is ShowUiStringSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowCheckoutWebView -> showCheckoutWebView(event.domain)
                is NavigateToDomainRegistration -> navigateToDomainRegistration(event.domain, event.productId)
            }
        }
    }

    private fun showCheckoutWebView(domain: String) {
        findNavController().navigateSafely(
            DomainSearchFragmentDirections
                .actionDomainSearchFragmentToDomainRegistrationCheckoutFragment(domain)
        )
    }

    private fun navigateToDomainRegistration(domain: String, productId: Int) {
        findNavController().navigateSafely(
            DomainSearchFragmentDirections.actionDomainSearchFragmentToDomainRegistrationDetailsFragment(
                DomainProductDetails(domain, productId)
            )
        )
    }
}

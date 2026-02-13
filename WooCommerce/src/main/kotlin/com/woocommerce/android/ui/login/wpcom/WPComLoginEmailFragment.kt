package com.woocommerce.android.ui.login.wpcom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.wpcom.WPComLoginEmailViewModel.ShowMagicLinkScreen
import com.woocommerce.android.ui.login.wpcom.WPComLoginEmailViewModel.ShowPasswordScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WPComLoginEmailFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: WPComLoginEmailViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    WPComLoginEmailScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowPasswordScreen -> {
                    navigateToPasswordScreen(event)
                }

                is ShowMagicLinkScreen -> {
                    navigateToMagicLinkScreen(event)
                }

                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun navigateToPasswordScreen(event: ShowPasswordScreen) {
        findNavController().navigateSafely(
            WPComLoginEmailFragmentDirections
                .actionWPComLoginEmailFragmentToWPComLoginPasswordFragment(
                    jetpackStatus = event.jetpackStatus,
                    emailOrUsername = event.emailOrUsername
                )
        )
    }

    private fun navigateToMagicLinkScreen(event: ShowMagicLinkScreen) {
        findNavController().navigateSafely(
            WPComLoginEmailFragmentDirections
                .actionWPComLoginEmailFragmentToWPComLoginMagicLinkRequestFragment(
                    emailOrUsername = event.emailOrUsername,
                    jetpackStatus = event.jetpackStatus,
                    fallbackButton = event.magicLinkFallbackButton,
                    requestAtStart = event.requestAtStart,
                    isNewWpComAccount = event.isNewWpComAccount
                )
        )
    }
}

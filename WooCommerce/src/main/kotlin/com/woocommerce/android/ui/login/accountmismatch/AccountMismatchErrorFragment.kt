package com.woocommerce.android.ui.login.accountmismatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToHelpScreen
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToSiteAddressEvent
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode

@AndroidEntryPoint
class AccountMismatchErrorFragment : BaseFragment() {
    private val viewModel: AccountMismatchErrorViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AccountMismatchErrorScreen(viewModel)
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
                is NavigateToHelpScreen -> {
                    startActivity(HelpActivity.createIntent(requireContext(), Origin.LOGIN_SITE_ADDRESS, null))
                }
                is NavigateToSiteAddressEvent -> findNavController().navigateSafely(
                    AccountMismatchErrorFragmentDirections
                        .actionAccountMismatchErrorFragmentToSitePickerSiteDiscoveryFragment()
                )
                is Logout -> onLogout()
            }
        }
    }

    private fun onLogout() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            LoginMode.WOO_LOGIN_MODE.putInto(this)
        }
        startActivity(intent)
    }
}

package com.woocommerce.android.ui.login.applicationpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.applicationpassword.ApplicationPasswordTutorialViewModel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationPasswordTutorialFragment : BaseFragment() {
    val viewModel: ApplicationPasswordTutorialViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    ApplicationPasswordTutorialScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is OnContinue -> {}
                is OnContactSupport -> openSupportRequestScreen()
            }
        }
    }

    private fun openSupportRequestScreen() {
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = HelpOrigin.APPLICATION_PASSWORD_TUTORIAL,
            extraTags = ArrayList()
        ).let { activity?.startActivity(it) }
    }
}

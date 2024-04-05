package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.sitecredentials.applicationpassword.ApplicationPasswordTutorialViewModel.OnContactSupport
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationPasswordTutorialFragment : BaseFragment() {
    val viewModel: ApplicationPasswordTutorialViewModel by viewModels()

    private val url: String by lazy { requireArguments().getString(URL_KEY, "") }
    private val errorMessageRes: Int? by lazy {
        requireArguments()
            .getInt(ERROR_MESSAGE_KEY, 0)
            .takeIf { it != 0 }
    }

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
                is OnContactSupport -> openSupportRequestScreen()
                is ExitWithResult<*> -> {
                    setFragmentResult(
                        requestKey = WEB_NAVIGATION_RESULT,
                        result = bundleOf(URL_KEY to it.data)
                    )
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.onWebViewDataAvailable(
            authorizationUrl = url,
            errorMessage = errorMessageRes
        )
    }

    private fun openSupportRequestScreen() {
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = HelpOrigin.APPLICATION_PASSWORD_TUTORIAL,
            extraTags = ArrayList()
        ).let { activity?.startActivity(it) }
    }

    companion object {
        const val TAG = "ApplicationPasswordTutorialFragment"
        const val URL_KEY = "url"
        const val ERROR_MESSAGE_KEY = "error_message"
        const val WEB_NAVIGATION_RESULT = "web_navigation_result"
        fun newInstance(url: String, errorMessageRes: Int) =
            ApplicationPasswordTutorialFragment().apply {
                arguments = bundleOf(
                    URL_KEY to url,
                    ERROR_MESSAGE_KEY to errorMessageRes
                )
            }
    }
}

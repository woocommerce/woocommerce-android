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
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.login.sitecredentials.applicationpassword.ApplicationPasswordTutorialViewModel.OnContactSupport
import com.woocommerce.android.ui.login.sitecredentials.applicationpassword.ApplicationPasswordTutorialViewModel.ShowExitConfirmationDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationPasswordTutorialFragment : BaseFragment() {
    val viewModel: ApplicationPasswordTutorialViewModel by viewModels()

    private val url: String by lazy { requireArguments().getString(URL_KEY, "") }
    private val errorMessage: String? by lazy {
        requireArguments()
            .getString(ERROR_MESSAGE_KEY, "")
            .takeIf { it.isNotEmpty() }
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
                is ExitWithResult<*> -> exitWithResult(it.data as String)
                is ShowExitConfirmationDialog -> showConfirmationDialog()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.onWebViewDataAvailable(
            authorizationUrl = url,
            errorMessage = errorMessage
        )
    }

    private fun showConfirmationDialog() {
        WooDialog.showDialog(
            activity = requireActivity(),
            messageId = R.string.login_app_password_exit_dialog_message,
            positiveButtonId = R.string.login_app_password_exit_dialog_confirmation,
            negativeButtonId = R.string.login_app_password_exit_dialog_cancel,
            posBtnAction = { _, _ -> viewModel.onExitConfirmed() }
        )
    }

    private fun exitWithResult(url: String = "") {
        if (url.isEmpty()) {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_DISMISSED)
        }

        setFragmentResult(
            requestKey = WEB_NAVIGATION_RESULT,
            result = bundleOf(URL_KEY to url)
        )
        parentFragmentManager.popBackStack()
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
        fun newInstance(url: String, errorMessage: String) =
            ApplicationPasswordTutorialFragment().apply {
                arguments = bundleOf(
                    URL_KEY to url,
                    ERROR_MESSAGE_KEY to errorMessage
                )
            }
    }
}

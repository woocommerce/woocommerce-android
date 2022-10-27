package com.woocommerce.android.ui.login.signup

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
import com.woocommerce.android.ui.login.signup.SignUpViewModel.NavigateToNextStep
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnTermsOfServiceClicked
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : BaseFragment() {

    @Inject internal lateinit var urlUtils: UrlUtils
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: SignUpViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    SignUpScreen(viewModel)
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
                is NavigateToNextStep -> navigateToNextStep()
                is OnTermsOfServiceClicked -> openTermsOfServiceUrl()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun navigateToNextStep() {
        findNavController().navigateSafely(
            SignUpFragmentDirections.actionSignUpFragmentToStoreCreationQuestionsFragment()
        )
    }

    private fun openTermsOfServiceUrl() {
        ChromeCustomTabUtils.launchUrl(requireContext(), urlUtils.tosUrlWithLocale)
    }
}

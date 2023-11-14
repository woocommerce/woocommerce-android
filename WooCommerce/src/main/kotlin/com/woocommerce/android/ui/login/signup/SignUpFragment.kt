package com.woocommerce.android.ui.login.signup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.serializable
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnAccountCreated
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnLoginWithEmail
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnTermsOfServiceClicked
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Partial.ThreeQuarters
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : BaseFragment() {
    companion object {
        const val TAG = "SignUpFragment"
        private const val NEXT_STEP_KEY = "next-step"

        fun newInstance(nextStep: NextStep): SignUpFragment = SignUpFragment().apply {
            arguments = bundleOf(
                NEXT_STEP_KEY to nextStep
            )
        }
    }

    interface Listener {
        fun onExistingEmail(email: String?)
        fun onAccountCreated()
    }

    @Inject internal lateinit var urlUtils: UrlUtils
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: SignUpViewModel by viewModels()
    private var signUpEmailFragment: Listener? = null

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val activity = requireActivity()
        require(activity is Listener) {
            "Parent activity has to implement ${Listener::class.java.name}"
        }
        signUpEmailFragment = activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.nextStep =
            requireArguments().serializable(NEXT_STEP_KEY) ?: error("Screen requires passing a NextStep")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    SignUpStepScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onDetach() {
        super.onDetach()

        signUpEmailFragment = null
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnLoginWithEmail -> signUpEmailFragment?.onExistingEmail(event.email)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is OnTermsOfServiceClicked -> openTermsOfServiceUrl()
                is OnAccountCreated -> signUpEmailFragment?.onAccountCreated()
                is Exit -> parentFragmentManager.popBackStack()
            }
        }
    }

    private fun openTermsOfServiceUrl() {
        ChromeCustomTabUtils.launchUrl(requireContext(), urlUtils.tosUrlWithLocale, ThreeQuarters)
    }

    enum class NextStep {
        STORE_CREATION,
        SITE_PICKER
    }
}

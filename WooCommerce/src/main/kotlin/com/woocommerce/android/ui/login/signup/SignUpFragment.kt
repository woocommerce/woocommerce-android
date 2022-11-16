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
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnLoginClicked
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
        fun onAccountCreated(nextStep: NextStep)
        fun onLoginClicked()
    }

    @Inject internal lateinit var urlUtils: UrlUtils
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: SignUpViewModel by viewModels()
    private var signUpListener: Listener? = null

    private lateinit var nextStep: NextStep

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val activity = requireActivity()
        require(activity is Listener) {
            "Parent activity has to implement ${Listener::class.java.name}"
        }
        signUpListener = activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nextStep = requireArguments().serializable(NEXT_STEP_KEY) ?: error("Screen requires passing a NextStep")
        viewModel.nextStep = nextStep
    }

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

    override fun onDetach() {
        super.onDetach()

        signUpListener = null
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnAccountCreated -> signUpListener?.onAccountCreated(nextStep)
                is OnLoginClicked -> signUpListener?.onLoginClicked()
                is OnTermsOfServiceClicked -> openTermsOfServiceUrl()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> parentFragmentManager.popBackStack()
            }
        }
    }

    private fun openTermsOfServiceUrl() {
        ChromeCustomTabUtils.launchUrl(requireContext(), urlUtils.tosUrlWithLocale)
    }

    enum class NextStep {
        STORE_CREATION,
        SITE_PICKER
    }
}

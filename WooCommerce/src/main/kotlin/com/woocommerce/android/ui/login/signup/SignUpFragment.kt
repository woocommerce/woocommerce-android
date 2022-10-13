package com.woocommerce.android.ui.login.signup

import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnTermsOfServiceClicked
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.util.getColorResIdFromAttribute
import org.wordpress.android.util.HtmlUtils
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : BaseFragment() {

    @Inject internal lateinit var urlUtils: UrlUtils
    private val viewModel: SignUpViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    SignUpScreen(viewModel, formattedTermsOfServiceText())
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
                is OnTermsOfServiceClicked -> openTermsOfServiceUrl()
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun formattedTermsOfServiceText(): Spanned {
        val primaryColorResId: Int = requireContext().getColorResIdFromAttribute(R.attr.colorSecondary)
        val primaryColorHtml = HtmlUtils.colorResToHtmlColor(requireContext(), primaryColorResId)
        return HtmlCompat.fromHtml(
            getString(
                R.string.continue_terms_of_service_text,
                "<u><font color='$primaryColorHtml'>", "</font></u>"
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun openTermsOfServiceUrl() {
        ChromeCustomTabUtils.launchUrl(requireContext(), urlUtils.tosUrlWithLocale)
    }
}

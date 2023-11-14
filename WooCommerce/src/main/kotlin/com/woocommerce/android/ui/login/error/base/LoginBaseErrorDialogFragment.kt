package com.woocommerce.android.ui.login.error.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

@AndroidEntryPoint
abstract class LoginBaseErrorDialogFragment : DialogFragment() {
    protected abstract val text: CharSequence
    protected open val illustration: Int = R.drawable.img_woo_generic_error
    protected abstract val helpOrigin: HelpOrigin
    protected open val inlineButtons: List<LoginErrorButton> = emptyList()
    protected open val primaryButton: LoginErrorButton? = null
    protected open val secondaryButton: LoginErrorButton? = LoginErrorButton(
        title = string.login_try_another_account,
        onClick = {
            (requireActivity() as LoginListener).startOver()
        }
    )

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show as fullscreen
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.Woo_Animations_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireActivity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    LoginErrorScreen(
                        text = text,
                        illustration = illustration,
                        onHelpButtonClick = ::openHelpScreen,
                        inlineButtons = inlineButtons,
                        primaryButton = primaryButton,
                        secondaryButton = secondaryButton
                    )

                    OverlayDialog()
                }
            }
        }
    }

    protected open fun openHelpScreen() {
        unifiedLoginTracker.trackClick(Click.SHOW_HELP)
        val flow = unifiedLoginTracker.getFlow()
        val step = unifiedLoginTracker.previousStepBeforeHelpStep
        val intent = HelpActivity.createIntent(requireContext(), helpOrigin, null, flow?.value, step?.value)
        startActivity(intent)
    }

    /**
     * This function allows adding an optional overlay content (progress or error dialogs).
     * When overridden, the function should use a [Dialog] composable function to make sure the content is shown
     * correctly as an Overlay.
     */
    @Composable
    protected open fun OverlayDialog() { }

    data class LoginErrorButton(
        @StringRes val title: Int,
        val onClick: () -> Unit
    )
}

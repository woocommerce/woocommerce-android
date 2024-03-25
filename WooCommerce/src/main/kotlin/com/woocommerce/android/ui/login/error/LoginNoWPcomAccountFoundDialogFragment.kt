package com.woocommerce.android.ui.login.error

import android.os.Bundle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

@AndroidEntryPoint
class LoginNoWPcomAccountFoundDialogFragment : LoginBaseErrorDialogFragment() {
    companion object {
        const val TAG = "LoginNoWPcomAccountFoundFragment"
    }

    @Inject
    internal lateinit var appPrefsWrapper: AppPrefsWrapper

    private val loginListener: LoginListener
        get() = requireActivity() as LoginListener
    override val text: CharSequence
        get() = getString(R.string.login_no_wpcom_account_found)

    override val illustration: Int
        get() = R.drawable.img_wpcom_error

    override val helpOrigin: HelpOrigin
        get() = HelpOrigin.LOGIN_EMAIL

    override val secondaryButton: LoginErrorButton
        get() = LoginErrorButton(
            title = R.string.login_try_another_account,
            onClick = {
                unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                loginListener.startOver()
            }
        )

    override val inlineButtons: List<LoginErrorButton>
        get() = listOf(
            LoginErrorButton(
                title = R.string.what_is_wordpress_link,
                onClick = {
                    ChromeCustomTabUtils.launchUrl(
                        requireContext(),
                        AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT
                    )
                    unifiedLoginTracker.trackClick(Click.WHAT_IS_WORDPRESS_COM_ON_INVALID_EMAIL_SCREEN)
                }
            ),
            LoginErrorButton(
                title = R.string.login_need_help_finding_email,
                onClick = {
                    loginListener.showHelpFindingConnectedEmail()
                }
            )
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            unifiedLoginTracker.track(step = Step.NO_WPCOM_ACCOUNT_FOUND)
        }
    }
}

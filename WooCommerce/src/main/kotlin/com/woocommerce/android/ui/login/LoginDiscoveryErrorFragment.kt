package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentLoginDiscoveryErrorBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

class LoginDiscoveryErrorFragment : Fragment(layout.fragment_login_discovery_error) {
    companion object {
        const val TAG = "LoginDiscoveryErrorFragment"
        const val ARG_SITE_ADDRESS = "SITE-ARG_SITE_ADDRESS"
        private const val ARG_SITE_XMLRPC_ADDRESS = "SITE_XMLRPC_ADDRESS"
        private const val ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME"
        private const val ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD"
        private const val ARG_USER_AVATAR_URL = "ARG_USER_AVATAR_URL"
        const val ARG_ERROR_MESSAGE = "ARG_ERROR_MESSAGE"

        fun newInstance(
            siteAddress: String,
            endpointAddress: String?,
            inputUsername: String,
            inputPassword: String,
            userAvatarUrl: String?,
            errorMessage: Int
        ): LoginDiscoveryErrorFragment {
            val fragment = LoginDiscoveryErrorFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_SITE_XMLRPC_ADDRESS, endpointAddress)
            args.putString(ARG_INPUT_USERNAME, inputUsername)
            args.putString(ARG_INPUT_PASSWORD, inputPassword)
            args.putString(ARG_USER_AVATAR_URL, userAvatarUrl)
            args.putInt(ARG_ERROR_MESSAGE, errorMessage)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var jetpackLoginListener: LoginNoJetpackListener? = null
    @Inject internal lateinit var unifiedLoginTracker: UnifiedLoginTracker

    private var errorMessage: Int? = null

    // Below params are needed when redirecting back to Site credentials screen
    private var siteAddress: String? = null
    private var siteXmlRpcAddress: String? = null
    private var mInputUsername: String? = null
    private var mInputPassword: String? = null
    private var userAvatarUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
            siteXmlRpcAddress = it.getString(ARG_SITE_XMLRPC_ADDRESS, null)
            mInputUsername = it.getString(ARG_INPUT_USERNAME, null)
            mInputPassword = it.getString(ARG_INPUT_PASSWORD, null)
            userAvatarUrl = it.getString(ARG_USER_AVATAR_URL, null)
            errorMessage = it.getInt(ARG_ERROR_MESSAGE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val binding = FragmentLoginDiscoveryErrorBinding.bind(view)
        // TODO: use binding to get toolbar
        binding.toolbarLogin.toolbar
        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        errorMessage?.let { binding.discoveryErrorMessage.text = Html.fromHtml(getString(it)) }

        with(binding.discoveryWordpressOptionView) {
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_DISCOVERY_ERROR_SIGN_IN_WORDPRESS_BUTTON_TAPPED)
                unifiedLoginTracker.trackClick(Click.CONTINUE_WITH_WORDPRESS_COM)
                jetpackLoginListener?.showEmailLoginScreen(siteAddress)
            }
        }

        with(binding.discoveryTroubleshootOptionView) {
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_DISCOVERY_ERROR_TROUBLESHOOT_BUTTON_TAPPED)
                unifiedLoginTracker.trackClick(Click.HELP_TROUBLESHOOTING_TIPS)
                jetpackLoginListener?.showJetpackTroubleshootingTips()
            }
        }

        with(binding.discoveryTryOptionView) {
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_DISCOVERY_ERROR_TRY_AGAIN_TAPPED)
                unifiedLoginTracker.trackClick(Click.TRY_AGAIN)
                jetpackLoginListener?.showUsernamePasswordScreen(
                        siteAddress, siteXmlRpcAddress, mInputUsername, mInputPassword
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            AnalyticsTracker.track(Stat.LOGIN_DISCOVERY_ERROR_MENU_HELP_TAPPED)
            loginListener?.helpSiteAddress(siteAddress)
            return true
        }

        return false
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        // this will throw if parent activity doesn't implement the login listener interface
        loginListener = context as? LoginListener
        jetpackLoginListener = context as? LoginNoJetpackListener
    }

    override fun onDetach() {
        super.onDetach()
        loginListener = null
        jetpackLoginListener = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(Stat.LOGIN_DISCOVERY_ERROR_SCREEN_VIEWED)
        unifiedLoginTracker.track(step = Step.CONNECTION_ERROR)
    }
}

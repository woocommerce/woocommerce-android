@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.login

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_JETPACK_INSTALLATION_SOURCE_WEB
import com.woocommerce.android.databinding.FragmentLoginNoJetpackBinding
import com.woocommerce.android.databinding.ViewLoginNoStoresBinding
import com.woocommerce.android.databinding.ViewLoginUserInfoBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginMode

@AndroidEntryPoint
class LoginNoJetpackFragment : Fragment(layout.fragment_login_no_jetpack) {
    companion object {
        const val TAG = "LoginNoJetpackFragment"
        private const val ARG_SITE_ADDRESS = "SITE-ADDRESS"
        private const val ARG_SITE_XMLRPC_ADDRESS = "SITE-XMLRPC-ADDRESS"
        private const val ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME"
        private const val ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD"
        private const val ARG_USER_AVATAR_URL = "ARG_USER_AVATAR_URL"
        private const val ARG_CHECK_JETPACK_AVAILABILITY = "ARG_CHECK_JETPACK_AVAILABILITY"
        private const val ARG_IS_JETPACK_CONNECT_CUSTOM_TAB_OPENED = "ARG_IS_JETPACK_CONNECT_CUSTOM_TAB_OPENED"

        fun newInstance(
            siteAddress: String,
            endpointAddress: String?,
            inputUsername: String,
            inputPassword: String,
            userAvatarUrl: String?,
            checkJetpackAvailability: Boolean = false
        ): LoginNoJetpackFragment {
            val fragment = LoginNoJetpackFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_SITE_XMLRPC_ADDRESS, endpointAddress)
            args.putString(ARG_INPUT_USERNAME, inputUsername)
            args.putString(ARG_INPUT_PASSWORD, inputPassword)
            args.putString(ARG_USER_AVATAR_URL, userAvatarUrl)
            args.putBoolean(ARG_CHECK_JETPACK_AVAILABILITY, checkJetpackAvailability)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var jetpackLoginListener: LoginNoJetpackListener? = null
    private var siteAddress: String? = null
    private var siteXmlRpcAddress: String? = null
    private var mInputUsername: String? = null
    private var mInputPassword: String? = null
    private var userAvatarUrl: String? = null
    private var isJetpackConnectCustomTabOpened = false

    @Suppress("DEPRECATION") private var progressDialog: ProgressDialog? = null

    /**
     * This flag, when set to true calls the CONNECT_SITE_INFO API to verify if Jetpack is
     * installed/activated/connected to the site. This flag will be set to true only when the
     * discovery process results in an error. The assumption being that certain discovery
     * errors can only take place if Jetpack is not connected to the site. On the off chance
     * that Jetpack is connected, but discovery still fails, we need to verify if Jetpack is
     * available in this class, before redirecting to the site credentials screen and
     * initiating the discovery process again.
     * */
    private var mCheckJetpackAvailability: Boolean = false

    private val viewModel: LoginNoJetpackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
            siteXmlRpcAddress = it.getString(ARG_SITE_XMLRPC_ADDRESS, null)
            mInputUsername = it.getString(ARG_INPUT_USERNAME, null)
            mInputPassword = it.getString(ARG_INPUT_PASSWORD, null)
            userAvatarUrl = it.getString(ARG_USER_AVATAR_URL, null)
            mCheckJetpackAvailability = it.getBoolean(ARG_CHECK_JETPACK_AVAILABILITY)
        }

        savedInstanceState?.let { bundle ->
            isJetpackConnectCustomTabOpened = bundle.getBoolean(ARG_IS_JETPACK_CONNECT_CUSTOM_TAB_OPENED)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        val binding = FragmentLoginNoJetpackBinding.bind(view)
        val btnBinding = binding.loginEpilogueButtonBar
        val noStoresBinding = ViewLoginNoStoresBinding.bind(view)
        val userInfoBinding = ViewLoginUserInfoBinding.bind(view)

        userInfoBinding.textDisplayname.text = mInputUsername
        with(userInfoBinding.textUsername) {
            val logOutText = getString(R.string.signout)
            val usernameText = getString(R.string.login_no_jetpack_username, mInputUsername, logOutText)
            text = usernameText

            val spannable = SpannableString(usernameText)
            spannable.setSpan(
                WooClickableSpan {
                    AnalyticsTracker.track(AnalyticsEvent.LOGIN_NO_JETPACK_LOGOUT_LINK_TAPPED)
                    activity?.setResult(Activity.RESULT_CANCELED)
                    val intent = Intent(activity, LoginActivity::class.java)
                    LoginMode.WOO_LOGIN_MODE.putInto(intent)
                    startActivity(intent)
                    activity?.finish()
                },
                (usernameText.length - logOutText.length),
                usernameText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
        }

        userAvatarUrl?.let {
            GlideApp.with(this)
                .load(it)
                .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.img_gravatar_placeholder))
                .circleCrop()
                .into(userInfoBinding.imageAvatar)
        }

        with(noStoresBinding.noStoresViewText) {
            visibility = View.VISIBLE
            text = getString(R.string.login_no_jetpack, siteAddress)
        }

        noStoresBinding.btnSecondaryAction.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_NO_JETPACK_WHAT_IS_JETPACK_LINK_TAPPED)
            jetpackLoginListener?.showWhatIsJetpackDialog()
        }

        with(btnBinding.buttonPrimary) {
            text = getString(R.string.login_jetpack_install)
            setOnClickListener {
                isJetpackConnectCustomTabOpened = true
                AnalyticsTracker.track(AnalyticsEvent.LOGIN_JETPACK_SETUP_BUTTON_TAPPED)
                jetpackLoginListener?.startJetpackInstall(siteAddress)
            }
        }

        with(btnBinding.buttonSecondary) {
            visibility = View.VISIBLE
            text = getString(R.string.try_again)
            setOnClickListener {
                AnalyticsTracker.track(AnalyticsEvent.LOGIN_NO_JETPACK_TRY_AGAIN_TAPPED)
                if (mCheckJetpackAvailability) {
                    // initiate the CONNECT_SITE_INFO API call
                    siteAddress?.let { viewModel.verifyJetpackAvailable(it) }
                } else {
                    jetpackLoginListener?.showUsernamePasswordScreen(
                        siteAddress, siteXmlRpcAddress, mInputUsername, mInputPassword
                    )
                }
            }
        }

        with(binding.buttonHelp) {
            setOnClickListener {
                AnalyticsTracker.track(AnalyticsEvent.LOGIN_NO_JETPACK_MENU_HELP_TAPPED)
                loginListener?.helpSiteAddress(siteAddress)
            }
        }

        initializeViewModel()
    }

    override fun onAttach(context: Context) {
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
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_NO_JETPACK_SCREEN_VIEWED)

        // This is used to track whether the Jetpack Connect Custom Tab is dismissed
        // before connection is finished.
        if (isJetpackConnectCustomTabOpened) {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_DISMISSED,
                properties = mapOf(KEY_SOURCE to VALUE_JETPACK_INSTALLATION_SOURCE_WEB)
            )
            isJetpackConnectCustomTabOpened = false
        }
    }

    private fun initializeViewModel() {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            showProgressDialog(it)
        }

        viewModel.isJetpackAvailable.observe(viewLifecycleOwner) { isJetpackAvailable ->
            if (isJetpackAvailable) {
                AppPrefs.setLoginUserBypassedJetpackRequired(false)
                redirectToSiteCredentialsScreen()
            } else {
                view?.let {
                    Snackbar.make(
                        it, getString(R.string.login_jetpack_not_found), BaseTransientBottomBar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = ProgressDialog.show(activity, "", getString(R.string.login_verifying_site), true)
            progressDialog?.setCancelable(false)
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.apply {
            if (isShowing) {
                cancel()
                progressDialog = null
            }
        }
    }

    private fun redirectToSiteCredentialsScreen() {
        jetpackLoginListener?.showUsernamePasswordScreen(
            siteAddress, siteXmlRpcAddress, mInputUsername, mInputPassword
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ARG_IS_JETPACK_CONNECT_CUSTOM_TAB_OPENED, isJetpackConnectCustomTabOpened)
        super.onSaveInstanceState(outState)
    }
}

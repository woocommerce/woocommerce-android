package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.login.LoginJetpackRequiredFragment.LoginJetpackRequiredListener
import com.woocommerce.android.widgets.WooClickableSpan
import kotlinx.android.synthetic.main.fragment_login_no_jetpack.*
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import kotlinx.android.synthetic.main.view_login_no_stores.*
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginMode

class LoginNoJetpackFragment : Fragment() {
    companion object {
        const val TAG = "LoginNoJetpackFragment"
        private const val ARG_SITE_ADDRESS = "SITE-ADDRESS"
        private const val ARG_SITE_XMLRPC_ADDRESS = "SITE-XMLRPC-ADDRESS"
        private const val ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME"
        private const val ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD"
        private const val ARG_USER_AVATAR_URL = "ARG_USER_AVATAR_URL"

        fun newInstance(
            siteAddress: String,
            endpointAddress: String?,
            inputUsername: String,
            inputPassword: String,
            userAvatarUrl: String?
        ): LoginNoJetpackFragment {
            val fragment = LoginNoJetpackFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_SITE_XMLRPC_ADDRESS, endpointAddress)
            args.putString(ARG_INPUT_USERNAME, inputUsername)
            args.putString(ARG_INPUT_PASSWORD, inputPassword)
            args.putString(ARG_USER_AVATAR_URL, userAvatarUrl)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var jetpackLoginListener: LoginJetpackRequiredListener? = null
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
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(layout.fragment_login_no_jetpack, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_displayname.text = mInputUsername
        with(text_username) {
            val logOutText = getString(R.string.signout)
            val usernameText = getString(R.string.login_no_jetpack_username, mInputUsername, logOutText)
            text = usernameText

            val spannable = SpannableString(usernameText)
            spannable.setSpan(
                    WooClickableSpan {
                        // TODO: add event here to track when logout is clicked
                        activity?.setResult(Activity.RESULT_CANCELED)
                        val intent = Intent(activity, LoginActivity::class.java)
                        LoginMode.WOO_LOGIN_MODE.putInto(intent)
                        startActivity(intent)
                        activity?.finish()
                    },
                    (usernameText.length - logOutText.length),
                    usernameText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
        }

        userAvatarUrl?.let {
            GlideApp.with(this)
                    .load(it)
                    .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                    .circleCrop()
                    .into(image_avatar)
        }

        with(no_stores_view) {
            visibility = View.VISIBLE
            text = getString(R.string.login_no_jetpack, siteAddress)
        }

        with(button_primary) {
            text = getString(R.string.login_jetpack_view_setup_instructions)
            setOnClickListener {
                // TODO: Add event here to track when primary button is clicked
                jetpackLoginListener?.showJetpackInstructions()
            }
        }

        with(button_secondary) {
            visibility = View.VISIBLE
            text = getString(R.string.try_again)
            setOnClickListener {
                // TODO: Add event here to track when secondary button is clicked
                jetpackLoginListener?.showUsernamePasswordScreen(
                        siteAddress, siteXmlRpcAddress, mInputUsername, mInputPassword
                )
            }
        }

        with(button_help) {
            setOnClickListener {
                // TODO: add tracking here when help is clicked
                loginListener?.helpSiteAddress(siteAddress)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // this will throw if parent activity doesn't implement the login listener interface
        loginListener = context as? LoginListener
        jetpackLoginListener = context as? LoginJetpackRequiredListener
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        // TODO: add tracking here on which screen is viewed
    }
}

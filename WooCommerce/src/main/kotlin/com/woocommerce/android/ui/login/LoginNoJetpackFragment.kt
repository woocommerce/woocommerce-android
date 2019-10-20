package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.login.LoginJetpackRequiredFragment.LoginJetpackRequiredListener
import com.woocommerce.android.widgets.WooClickableSpan
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import kotlinx.android.synthetic.main.view_login_no_stores.*
import org.wordpress.android.login.LoginListener

class LoginNoJetpackFragment : Fragment() {
    companion object {
        const val TAG = "LoginNoJetpackFragment"
        private const val ARG_SITE_ADDRESS = "SITE-ADDRESS"
        private const val ARG_SITE_XMLRPC_ADDRESS = "SITE-XMLRPC-ADDRESS"
        private val ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME"
        private val ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD"

        fun newInstance(
            siteAddress: String,
            endpointAddress: String?,
            inputUsername: String,
            inputPassword: String
        ): LoginNoJetpackFragment {
            val fragment = LoginNoJetpackFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_SITE_XMLRPC_ADDRESS, endpointAddress)
            args.putString(ARG_INPUT_USERNAME, inputUsername)
            args.putString(ARG_INPUT_PASSWORD, inputPassword)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
            siteXmlRpcAddress = it.getString(ARG_SITE_XMLRPC_ADDRESS, null)
            mInputUsername = it.getString(ARG_INPUT_USERNAME, null)
            mInputPassword = it.getString(ARG_INPUT_PASSWORD, null)
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

        with(no_stores_view) {
            val refreshAppText = getString(R.string.login_refresh_app_continue)
            val notConnectedText = getString(
                    R.string.login_not_connected_jetpack,
                    siteAddress,
                    refreshAppText
            )

            val spannable = SpannableString(notConnectedText)
            spannable.setSpan(
                    WooClickableSpan {
                        // TODO: add event here to track when refresh link is clicked
                        // TODO: call the username password fragment
                    },
                    (notConnectedText.length - refreshAppText.length),
                    notConnectedText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
        }

        with(button_primary) {
            text = getString(R.string.login_jetpack_view_setup_instructions)
            setOnClickListener {
                // Add event here to track when primary button is clicked
                jetpackLoginListener?.showJetpackInstructions()
            }
        }

        with(button_secondary) {
            text = getString(R.string.try_again)
            setOnClickListener {
                // Add event here to track when secondary button is clicked
                // TODO: call the username password fragment
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            // TODO: add tracking here when help is clicked
            loginListener?.helpSiteAddress(siteAddress)
            return true
        }

        return false
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

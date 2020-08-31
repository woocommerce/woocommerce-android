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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.widgets.WooClickableSpan
import kotlinx.android.synthetic.main.fragment_login_jetpack_required.*
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import org.wordpress.android.login.LoginListener

/**
 * Fragment shown if the user attempts to login with a site that does not have Jetpack installed which
 * is required for the WCAndroid app.
 */
class LoginJetpackRequiredFragment : Fragment() {
    companion object {
        const val TAG = "LoginJetpackRequiredFragment"
        const val ARG_SITE_ADDRESS = "SITE-ADDRESS"

        fun newInstance(siteAddress: String): LoginJetpackRequiredFragment {
            val fragment = LoginJetpackRequiredFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var jetpackLoginListener: LoginNoJetpackListener? = null
    private var siteAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(layout.fragment_login_jetpack_required, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        with(button_primary) {
            text = getString(R.string.login_jetpack_view_instructions)
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED)
                jetpackLoginListener?.showJetpackInstructions()
            }
        }

        with(button_secondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_refresh_after_install)
            setOnClickListener {
                // TODO AMANDA : track event

                // TODO AMANDA : retry to connect via Jetpack
            }
        }

        btn_what_is_jetpack.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED)
            jetpackLoginListener?.showWhatIsJetpackDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_MENU_HELP_TAPPED)
            loginListener?.helpSiteAddress(siteAddress)
            return true
        }

        return false
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
        AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED)
    }
}

package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import kotlinx.android.synthetic.main.fragment_login_jetpack_required.*
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

    interface LoginJetpackRequiredListener {
        fun showJetpackInstructions()
        fun showWhatIsJetpackDialog()
    }

    private var loginListener: LoginListener? = null
    private var jetpackLoginListener: LoginJetpackRequiredListener? = null
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

        btn_jetpack_instructions.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED)
            jetpackLoginListener?.showJetpackInstructions()
        }

        btn_contact_support.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_CONTACT_SUPPORT_BUTTON_TAPPED)
            loginListener?.helpSiteAddress(siteAddress)
        }

        btn_what_is_jetpack.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED)
            jetpackLoginListener?.showWhatIsJetpackDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // this will throw if parent activity doesn't implement the login listener interface
        loginListener = context as? LoginListener
        jetpackLoginListener = context as? LoginJetpackRequiredListener
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED)
    }
}

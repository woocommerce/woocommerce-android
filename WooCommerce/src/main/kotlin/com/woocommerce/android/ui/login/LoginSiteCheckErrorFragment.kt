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
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_login_generic_error.*
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

class LoginSiteCheckErrorFragment : Fragment() {
    companion object {
        const val TAG = "LoginGenericErrorFragment"
        const val ARG_SITE_ADDRESS = "SITE-ADDRESS"
        const val ARG_ERROR_MESSAGE = "ERROR-MESSAGE"

        fun newInstance(siteAddress: String, errorMsg: String): LoginSiteCheckErrorFragment {
            val fragment = LoginSiteCheckErrorFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_ERROR_MESSAGE, errorMsg)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    private var loginListener: LoginListener? = null
    private var siteAddress: String? = null
    private var errorMsg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(LoginJetpackRequiredFragment.ARG_SITE_ADDRESS, null)
            errorMsg = it.getString(ARG_ERROR_MESSAGE, null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_login_generic_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        login_error_msg.text = errorMsg

        with(button_primary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_store)
            setOnClickListener {
                unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_STORE)

                requireActivity().onBackPressed()
            }
        }

        with(button_secondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_account)
            setOnClickListener {
                unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)

                loginListener?.startOver()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            unifiedLoginTracker.trackClick(Click.SHOW_HELP)
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
    }

    override fun onDetach() {
        loginListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()

        unifiedLoginTracker.track(step = Step.NOT_WORDPRESS_SITE)
    }
}

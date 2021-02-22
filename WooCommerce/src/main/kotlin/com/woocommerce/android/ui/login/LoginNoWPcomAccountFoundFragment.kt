package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentLoginNoWpcomAccountFoundBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.zendesk.util.StringUtils
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

class LoginNoWPcomAccountFoundFragment : Fragment(R.layout.fragment_login_no_wpcom_account_found) {
    companion object {
        const val TAG = "LoginNoWPcomAccountFoundFragment"
        const val ARG_EMAIL_ADDRESS = "email_address"

        fun newInstance(emailAddress: String?): LoginNoWPcomAccountFoundFragment {
            val fragment = LoginNoWPcomAccountFoundFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var emailAddress: String? = null

    @Inject internal lateinit var unifiedLoginTracker: UnifiedLoginTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            emailAddress = it.getString(ARG_EMAIL_ADDRESS, null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val binding = FragmentLoginNoWpcomAccountFoundBinding.bind(view)
        val btnBinding = binding.loginEpilogueButtonBar

        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        binding.noWpAccountMsg.text = getString(R.string.login_no_wpcom_account_found, emailAddress)

        with(btnBinding.buttonPrimary) {
            text = getString(R.string.login_store_address)
            setOnClickListener {
                unifiedLoginTracker.trackClick(Click.LOGIN_WITH_SITE_ADDRESS)

                loginListener?.loginViaSiteAddress()
            }
        }

        with(btnBinding.buttonSecondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_account)
            setOnClickListener {
                unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)

                loginListener?.startOver()
            }
        }

        binding.btnFindConnectedEmail.setOnClickListener {
            loginListener?.showHelpFindingConnectedEmail()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            unifiedLoginTracker.trackClick(Click.SHOW_HELP)
            loginListener?.helpEmailScreen(emailAddress ?: StringUtils.EMPTY_STRING)
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
        super.onDetach()
        loginListener = null
    }

    override fun onResume() {
        super.onResume()

        unifiedLoginTracker.track(step = Step.NO_WPCOM_ACCOUNT_FOUND)
    }
}

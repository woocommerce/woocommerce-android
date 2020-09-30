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
import com.zendesk.util.StringUtils
import kotlinx.android.synthetic.main.fragment_login_no_wpcom_account_found.*
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import org.wordpress.android.login.LoginListener

class LoginNoWPcomAccountFoundFragment : Fragment() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            emailAddress = it.getString(ARG_EMAIL_ADDRESS, null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_login_no_wpcom_account_found, container, false)
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
            text = getString(R.string.login_store_address)
            setOnClickListener {
                // TODO AMANDA - tracks

                loginListener?.loginViaSiteAddress()
            }
        }

        with(button_secondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_account)
            setOnClickListener {
                // TODO AMANDA - tracks

                loginListener?.startOver()
            }
        }

        btn_find_connected_email.setOnClickListener {
            // TODO AMANDA - tracks

            loginListener?.showHelpFindingConnectedEmail()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            // TODO AMANDA - tracks event

            loginListener?.helpEmailScreen(emailAddress ?: StringUtils.EMPTY_STRING)
            return true
        }

        return false
    }

    override fun onAttach(context: Context) {
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

        // TODO AMANDA - tracks event
    }
}

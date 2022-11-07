package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentLoginMagicLinkSentImprovedBinding
import com.woocommerce.android.ui.login.qrcode.QrCodeLoginListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

@AndroidEntryPoint
class LoginMagicLinkSentImprovedFragment : Fragment(R.layout.fragment_login_magic_link_sent_improved), MenuProvider {
    companion object {
        const val TAG = "login_magic_link_sent_fragment_tag"
        private const val ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS"
        private const val ARG_ALLOW_PASSWORD = "ARG_ALLOW_PASSWORD"

        fun newInstance(email: String?, allowPassword: Boolean = true): LoginMagicLinkSentImprovedFragment {
            val fragment = LoginMagicLinkSentImprovedFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, email)
            args.putBoolean(ARG_ALLOW_PASSWORD, allowPassword)
            fragment.arguments = args
            return fragment
        }
    }

    private var mLoginListener: LoginListener? = null
    private lateinit var qrCodeLoginListener: QrCodeLoginListener
    private var mEmail: String? = null
    private var mAllowPassword = false

    @Inject lateinit var mAnalyticsListener: LoginAnalyticsListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            mEmail = args.getString(ARG_EMAIL_ADDRESS)
            mAllowPassword = args.getBoolean(ARG_ALLOW_PASSWORD)
        }
        savedInstanceState?.let {
            mAnalyticsListener.trackLoginMagicLinkOpenEmailClientViewed()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is QrCodeLoginListener) {
            qrCodeLoginListener = activity as QrCodeLoginListener
        }
        if (activity is LoginListener) {
            mLoginListener = activity as LoginListener
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val binding = FragmentLoginMagicLinkSentImprovedBinding.bind(view)
        binding.loginOpenEmailClient.setOnClickListener { mLoginListener?.openEmailClient(true) }
        with(binding.loginEnterPassword) {
            visibility = if (mAllowPassword) View.VISIBLE else View.GONE
            setOnClickListener {
                mAnalyticsListener.trackLoginWithPasswordClick()
                mLoginListener?.usePasswordInstead(mEmail)
            }
        }
        binding.email.text = mEmail
        binding.magicLinkSentScanQrCode.setOnClickListener { qrCodeLoginListener.onScanQrCodeClicked(TAG) }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_login, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.help -> {
                mAnalyticsListener.trackShowHelpClick()
                mLoginListener?.helpMagicLinkSent(mEmail)
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        mAnalyticsListener.magicLinkSentScreenResumed()
    }

    override fun onDetach() {
        super.onDetach()
        mLoginListener = null
    }
}

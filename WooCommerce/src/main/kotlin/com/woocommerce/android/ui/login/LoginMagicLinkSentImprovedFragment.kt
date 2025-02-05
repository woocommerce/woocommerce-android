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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentLoginMagicLinkSentImprovedBinding
import com.woocommerce.android.extensions.serializable
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.MagicLinkFallbackButton
import javax.inject.Inject

@AndroidEntryPoint
class LoginMagicLinkSentImprovedFragment : Fragment(R.layout.fragment_login_magic_link_sent_improved), MenuProvider {
    companion object {
        const val TAG = "login_magic_link_sent_fragment_tag"
        private const val ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS"
        private const val ARG_FALLBACK_BUTTON: String = "ARG_FALLBACK_BUTTON"

        fun newInstance(email: String?, fallbackButton: MagicLinkFallbackButton): LoginMagicLinkSentImprovedFragment {
            val fragment = LoginMagicLinkSentImprovedFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, email)
            args.putSerializable(ARG_FALLBACK_BUTTON, fallbackButton)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var email: String? = null
    private var fallbackButton: MagicLinkFallbackButton = MagicLinkFallbackButton.None

    @Inject lateinit var mAnalyticsListener: LoginAnalyticsListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            email = args.getString(ARG_EMAIL_ADDRESS)
            fallbackButton = args.serializable(ARG_FALLBACK_BUTTON) ?: MagicLinkFallbackButton.None
        }
        savedInstanceState?.let {
            mAnalyticsListener.trackLoginMagicLinkOpenEmailClientViewed()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is LoginListener) {
            loginListener = activity as LoginListener
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val binding = FragmentLoginMagicLinkSentImprovedBinding.bind(view)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        binding.loginOpenEmailClient.setOnClickListener { loginListener?.openEmailClient(true) }
        with(binding.loginMagicLinkFallbackButton) {
            text = if (fallbackButton == MagicLinkFallbackButton.Password) {
                getString(R.string.or_use_password_below_qr_code_scan_option)
            } else {
                getString(R.string.login_use_wpcom_username_instead)
            }
            isVisible = fallbackButton != MagicLinkFallbackButton.None
            setOnClickListener {
                when (fallbackButton) {
                    MagicLinkFallbackButton.Password -> {
                        mAnalyticsListener.trackLoginWithPasswordClick()
                        loginListener?.usePasswordInstead(email)
                    }
                    MagicLinkFallbackButton.UsernameAndPassword -> {
                        mAnalyticsListener.trackLoginWithWpComUsernamePasswordClick()
                        loginListener?.loginViaWpcomUsernameInstead()
                    }
                    MagicLinkFallbackButton.None -> error("This button should not be visible")
                }
            }
        }
        binding.email.text = email
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(org.wordpress.android.login.R.menu.menu_login, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            org.wordpress.android.login.R.id.help -> {
                mAnalyticsListener.trackShowHelpClick()
                loginListener?.helpMagicLinkSent(email)
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
        loginListener = null
    }
}

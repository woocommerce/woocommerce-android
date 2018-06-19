package com.woocommerce.android.ui.login

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.fragment_login_prologue.*

class LoginPrologueFragment : Fragment() {
    companion object {
        private const val JETPACK_HELP_URL = "https://jetpack.com/support/getting-started-with-jetpack/"
        const val TAG = "login-prologue-fragment"
        fun newInstance(): LoginPrologueFragment {
            return LoginPrologueFragment()
        }
    }

    interface PrologueFinishedListener {
        fun onPrologueFinished()
    }

    internal lateinit var prologueFinishedListener: PrologueFinishedListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_prologue, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button_login_jetpack.setOnClickListener({
            prologueFinishedListener?.onPrologueFinished()
        })

        text_config_link.movementMethod = LinkMovementMethod.getInstance()
        val html = String.format(getString(R.string.login_configure_link), "<a href='$JETPACK_HELP_URL'>", "</a>")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text_config_link.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            text_config_link.text = Html.fromHtml(html)
        }
    }
}

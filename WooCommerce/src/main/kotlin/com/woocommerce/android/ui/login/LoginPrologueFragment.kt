package com.woocommerce.android.ui.login

import android.content.Context
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
import org.wordpress.android.util.DisplayUtils

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

    private var prologueFinishedListener: PrologueFinishedListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_prologue, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (activity is PrologueFinishedListener) {
            prologueFinishedListener = activity as PrologueFinishedListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        prologueFinishedListener = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button_login_jetpack.setOnClickListener({
            prologueFinishedListener?.onPrologueFinished()
        })

        val separator = if (DisplayUtils.isLandscape(activity)) " " else "<br><br>"
        val html = getString(R.string.login_jetpack_required) + separator +
                getString(R.string.login_configure_link, "<a href='$JETPACK_HELP_URL'>", "</a>")
        text_jetpack.movementMethod = LinkMovementMethod.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text_jetpack.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            text_jetpack.text = Html.fromHtml(html)
        }
    }
}

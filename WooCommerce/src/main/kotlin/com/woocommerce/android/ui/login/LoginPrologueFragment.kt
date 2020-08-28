package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import kotlinx.android.synthetic.main.fragment_login_prologue.*

class LoginPrologueFragment : androidx.fragment.app.Fragment() {
    companion object {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is PrologueFinishedListener) {
            prologueFinishedListener = activity as PrologueFinishedListener
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDetach() {
        super.onDetach()
        prologueFinishedListener = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button_login_store.setOnClickListener {
            prologueFinishedListener?.onPrologueFinished()
            AnalyticsTracker.track(Stat.LOGIN_PROLOGUE_JETPACK_LOGIN_BUTTON_TAPPED)
        }
    }
}

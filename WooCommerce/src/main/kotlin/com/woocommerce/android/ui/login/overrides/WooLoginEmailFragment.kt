package com.woocommerce.android.ui.login.overrides

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import org.wordpress.android.login.LoginEmailFragment

class WooLoginEmailFragment : LoginEmailFragment() {

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_screen

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
    }
}

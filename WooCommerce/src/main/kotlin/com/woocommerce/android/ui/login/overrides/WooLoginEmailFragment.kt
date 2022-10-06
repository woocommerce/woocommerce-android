package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.showKeyboardWithDelay
import org.wordpress.android.login.LoginEmailFragment
import org.wordpress.android.login.widgets.WPLoginInputRow

class WooLoginEmailFragment : LoginEmailFragment() {
    interface Listener {
        fun onWhatIsWordPressLinkClicked()
    }

    private lateinit var whatIsWordPressLinkClickListener: Listener

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_screen

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        with(rootView) {
            findViewById<Button>(R.id.login_what_is_wordpress).setOnClickListener {
                whatIsWordPressLinkClickListener.onWhatIsWordPressLinkClicked()
            }
            findViewById<WPLoginInputRow>(R.id.login_email_row).editText.showKeyboardWithDelay(0)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            whatIsWordPressLinkClickListener = activity as Listener
        }
    }
}

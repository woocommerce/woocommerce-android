package com.woocommerce.android.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.activity_login_prologue.*
import org.wordpress.android.login.LoginMode

class LoginPrologueActivity : AppCompatActivity() {
    companion object {
        private const val JETPACK_HELP_URL = "https://jetpack.com/support/getting-started-with-jetpack/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_prologue)

        button_login_jetpack.setOnClickListener({
            val intent = Intent(this, LoginActivity::class.java)
            LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
            intent.putExtra(LoginActivity.SHOW_PROLOGUE_ON_BACK_PRESS, true)
            startActivity(intent)
            finish()
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

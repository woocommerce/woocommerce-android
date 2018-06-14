package com.woocommerce.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R
import org.wordpress.android.login.LoginMode

class LoginPrologueActivity : AppCompatActivity() {
    companion object {
        private const val JETPACK_HELP_URL = "https://jetpack.com/support/getting-started-with-jetpack/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_prologue)

        val btnLogin = findViewById<Button>(R.id.button_login_jetpack)
        btnLogin.setOnClickListener({
            val intent = Intent(this, LoginActivity::class.java)
            LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
            startActivity(intent)
            finish()
        })

        val txtConfigLink = findViewById<TextView>(R.id.text_config_link)
        txtConfigLink.movementMethod = LinkMovementMethod.getInstance()
        txtConfigLink.text = Html.fromHtml(String.format(getString(R.string.login_configure_link),
                "<a href='$JETPACK_HELP_URL'>", "</a>"))
    }
}

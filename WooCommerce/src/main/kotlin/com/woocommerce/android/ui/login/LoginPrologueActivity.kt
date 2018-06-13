package com.woocommerce.android.ui.login

import android.app.Activity
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.ActivityUtils

class LoginPrologueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_prologue)

        val btnLogin = findViewById<Button>(R.id.button_login_jetpack)
        btnLogin.setOnClickListener({
            setResult(Activity.RESULT_OK)
            finish()
        })

        val txtConfigLink = findViewById<TextView>(R.id.text_config_link)
        txtConfigLink.paintFlags = txtConfigLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        txtConfigLink.setOnClickListener {
            ActivityUtils.openUrlExternal(this,
                    "https://jetpack.com/support/getting-started-with-jetpack/")
        }
    }
}

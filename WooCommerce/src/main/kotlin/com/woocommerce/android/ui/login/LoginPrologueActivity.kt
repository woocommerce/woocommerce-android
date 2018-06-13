package com.woocommerce.android.ui.login

import android.app.Activity
import android.graphics.Paint
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.ActivityUtils

class LoginPrologueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_prologue)
        setStatusBarColor()

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

    private fun setStatusBarColor() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.wc_purple_status)
        }
    }
}

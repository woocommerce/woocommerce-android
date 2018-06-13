package com.woocommerce.android.ui.login

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.woocommerce.android.R

class LoginPrologueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_prologue)

        val btnLogin = findViewById<Button>(R.id.button_login_jetpack)
        btnLogin.setOnClickListener(View.OnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        })
    }
}

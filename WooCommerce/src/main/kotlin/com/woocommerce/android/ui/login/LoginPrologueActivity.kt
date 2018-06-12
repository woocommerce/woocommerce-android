package com.woocommerce.android.ui.login

import android.app.Activity
import android.os.Bundle
import dagger.android.AndroidInjection

class LoginPrologueActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
    }
}

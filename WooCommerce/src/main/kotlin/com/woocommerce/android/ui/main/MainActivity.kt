package com.woocommerce.android.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.LoginActivity
import dagger.android.AndroidInjection
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100
    }

    @Inject lateinit var presenter: MainContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter.takeView(this)

        if (!presenter.userIsLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_ACCOUNT)
            finish()
            return
        }
    }

    public override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ADD_ACCOUNT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO
                }
            }
        }
    }
}

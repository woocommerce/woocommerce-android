package com.woocommerce.android.ui.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.woocommerce.android.R
import dagger.android.AndroidInjection
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainContract.View {
    @Inject lateinit var presenter: MainContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter.takeView(this)

        if (!presenter.userIsLoggedIn()) {
            // TODO: Login
        }
    }

    public override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }
}

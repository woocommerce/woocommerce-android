package com.woocommerce.android.ui.login

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.util.ActivityUtils
import dagger.android.AndroidInjection
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View {
    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var accountStore: AccountStore

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)
        ActivityUtils.setStatusBarColor(this, R.color.wc_grey)
        presenter.takeView(this)
        updateAvatar()
    }

    override fun updateAvatar() {
        GlideApp.with(this)
                .load(accountStore?.account?.avatarUrl)
                .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                .into(findViewById(R.id.image_avatar))
    }
}

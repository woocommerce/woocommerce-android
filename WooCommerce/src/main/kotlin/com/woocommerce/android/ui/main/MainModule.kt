package com.woocommerce.android.ui.main

import android.app.Activity
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
internal abstract class MainModule {
    companion object {
        // Hilt provides the current activity typed as Activity, this casts it to MainActivity
        @Provides
        fun provideMainActivity(activity: Activity): MainActivity {
            return activity as MainActivity
        }
    }

    @ActivityScoped
    @Binds
    abstract fun provideMainPresenter(mainActivityPresenter: MainPresenter): MainContract.Presenter

    @ActivityScoped
    @Binds
    abstract fun provideUiMessageResolver(mainUIMessageResolver: MainUIMessageResolver): UIMessageResolver
}

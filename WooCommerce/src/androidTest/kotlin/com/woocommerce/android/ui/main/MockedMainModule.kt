package com.woocommerce.android.ui.main

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.di.ActivityScope
import dagger.Module
import dagger.Provides

@Module
object MockedMainModule {
    private var userIsLoggedIn = false

    fun setUserIsLoggedInResponse(userLoggedIn: Boolean) {
        userIsLoggedIn = userLoggedIn
    }

    @JvmStatic
    @ActivityScope
    @Provides
    fun provideMainPresenter(): MainContract.Presenter {
        val mockedMainPresenter = mock<MainPresenter>()
        whenever(mockedMainPresenter.userIsLoggedIn()).thenReturn(userIsLoggedIn)
        return mockedMainPresenter
    }
}

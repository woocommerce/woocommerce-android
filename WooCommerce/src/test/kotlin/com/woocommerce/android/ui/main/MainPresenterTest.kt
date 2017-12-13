package com.woocommerce.android.ui.main

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore

class MainPresenterTest {
    private val mainContractView: MainContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()

    private lateinit var mainPresenter: MainPresenter

    @Test
    fun `Reports AccountStore token status correctly`() {
        mainPresenter = spy(MainPresenter(dispatcher, accountStore))
        mainPresenter.takeView(mainContractView)

        Assert.assertFalse(mainPresenter.userIsLoggedIn())

        doReturn(true).whenever(accountStore).hasAccessToken()
        Assert.assertTrue(mainPresenter.userIsLoggedIn())
    }
}

package com.woocommerce.android.ui.main

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore

class MainPresenterTest {
    @Mock private lateinit var mainContractView: MainContract.View

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var accountStore: AccountStore

    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        // Inject the annotated mocks
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `Reports AccountStore token status correctly`() {
        mainPresenter = spy(MainPresenter(dispatcher, accountStore))
        mainPresenter.takeView(mainContractView)

        Assert.assertFalse(mainPresenter.userIsLoggedIn())

        doReturn(true).whenever(accountStore).hasAccessToken()
        Assert.assertTrue(mainPresenter.userIsLoggedIn())
    }
}

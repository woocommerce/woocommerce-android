package com.woocommerce.android

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore

@OptIn(ExperimentalCoroutinesApi::class)
class AppInitializerTest : BaseUnitTest() {

    private val accountStoreMock: AccountStore = mock()
    private val analyticsTrackerMock: AnalyticsTrackerWrapper = mock()

    private val sut = AppInitializer().apply {
        this.accountStore = accountStoreMock
        this.analyticsTracker = analyticsTrackerMock
    }

    @Test
    fun `given user enabled tracking in API, when account settings fetched, then enable tracking`() {
        // given
        accountStoreMock.stub {
            on { account } doReturn AccountModel().apply {
                tracksOptOut = false
            }
        }

        // when
        sut.onAccountChanged(
            AccountStore.OnAccountChanged().apply { causeOfChange = AccountAction.FETCH_SETTINGS }
        )

        // then
        verify(analyticsTrackerMock).sendUsageStats = false
    }

    @Test
    fun `given user disabled tracking in API, when account settings fetched, then disable tracking`() {
        // given
        accountStoreMock.stub {
            on { account } doReturn AccountModel().apply {
                tracksOptOut = true
            }
        }

        // when
        sut.onAccountChanged(
            AccountStore.OnAccountChanged().apply { causeOfChange = AccountAction.FETCH_SETTINGS }
        )

        // then
        verify(analyticsTrackerMock).sendUsageStats = false
    }
}

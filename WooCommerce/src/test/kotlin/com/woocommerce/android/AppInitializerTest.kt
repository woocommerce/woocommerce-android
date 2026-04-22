package com.woocommerce.android

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.work.impl.WorkManagerImpl
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.background.BackgroundUpdatesDisabled
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class AppInitializerTest : BaseUnitTest() {

    private val accountStoreMock: AccountStore = mock()
    private val analyticsTrackerMock: AnalyticsTrackerWrapper = mock()
    private val application: Application = mock()
    private val networkStatusMock: NetworkStatus = mock()
    private val registerDeviceMock: RegisterDevice = mock()
    private val backgroundUpdatesDisabledMock: BackgroundUpdatesDisabled = mock()
    private val connectionReceiverMock: ConnectionChangeReceiver = mock()
    private val appWidgetManagerMock: AppWidgetManager = mock()
    private val workManagerMock: WorkManagerImpl = mock()
    private val selectedSiteMock: SelectedSite = mock()
    private val wooCommerceStoreMock: WooCommerceStore = mock()

    private lateinit var analyticsTrackerStaticMock: MockedStatic<AnalyticsTracker>

    private lateinit var sut: AppInitializer

    @Before
    fun setup() {
        analyticsTrackerStaticMock = mockStatic(AnalyticsTracker::class.java)

        sut = AppInitializer().apply {
            this.accountStore = accountStoreMock
            this.analyticsTracker = analyticsTrackerMock
            this.networkStatus = networkStatusMock
            this.registerDevice = registerDeviceMock
            this.backgroundUpdatesDisabled = backgroundUpdatesDisabledMock
            this.connectionReceiver = connectionReceiverMock
            this.selectedSite = selectedSiteMock
            this.wooCommerceStore = wooCommerceStoreMock
            this.appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher)
            setPrivateApplication(application)
            setConnectionReceiverRegistered()
        }

        whenever(selectedSiteMock.getIfExists()).thenReturn(null)
    }

    @After
    fun tearDown() {
        analyticsTrackerStaticMock.close()
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
        verify(analyticsTrackerMock).sendUsageStats = true
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

    @Test
    fun `when app comes from background and network is connected, then app foreground trigger registers device`() = testBlocking {
        // GIVEN
        networkStatusMock.stub {
            on { isConnected() } doReturn true
        }
        appWidgetManagerMock.stub {
            on { installedProviders } doReturn emptyList()
        }

        // WHEN
        mockStatic(WorkManagerImpl::class.java).use {
            whenever(WorkManagerImpl.getInstance(application)).thenReturn(workManagerMock)

            mockStatic(AppWidgetManager::class.java).use {
                whenever(AppWidgetManager.getInstance(application)).thenReturn(appWidgetManagerMock)

                sut.onAppComesFromBackground()
                coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()
            }
        }

        // THEN
        verify(registerDeviceMock).kickoff(RegisterDevice.Trigger.APP_FOREGROUND)
    }

    @Test
    fun `when account fetch completes, then push registration is not triggered directly`() = testBlocking {
        // WHEN
        sut.onAccountChanged(
            AccountStore.OnAccountChanged().apply { causeOfChange = AccountAction.FETCH_ACCOUNT }
        )

        // THEN
        verifyBlocking(registerDeviceMock, never()) {
            invoke(any())
        }
    }
    private fun AppInitializer.setPrivateApplication(application: Application) {
        AppInitializer::class.java.getDeclaredField("application").apply {
            isAccessible = true
            set(this@setPrivateApplication, application)
        }
    }

    private fun AppInitializer.setConnectionReceiverRegistered() {
        AppInitializer::class.java.getDeclaredField("connectionReceiverRegistered").apply {
            isAccessible = true
            setBoolean(this@setConnectionReceiverRegistered, true)
        }
    }
}

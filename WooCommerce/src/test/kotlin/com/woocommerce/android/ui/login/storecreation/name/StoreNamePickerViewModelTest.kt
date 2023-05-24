package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToStoreProfiler
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreNamePickerViewModelTest : BaseUnitTest() {
    private lateinit var sut: StoreNamePickerViewModel
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var prefsWrapper: AppPrefsWrapper
    private val savedState = SavedStateHandle()
    private val localNotificationScheduler: LocalNotificationScheduler = mock()
    private val accountStore: AccountStore = mock()
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled = mock()

    @Before
    fun setUp() {
        prefsWrapper = mock()
        analyticsTracker = mock()
        sut = StoreNamePickerViewModel(
            savedStateHandle = savedState,
            newStore = mock(),
            analyticsTrackerWrapper = analyticsTracker,
            prefsWrapper = prefsWrapper,
            localNotificationScheduler,
            isRemoteFeatureFlagEnabled,
            accountStore
        )
    }

    @Test
    fun `when onContinueClicked happens, then Store Profiler event is triggered`() {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onStoreNameChanged("Store name")
        sut.onContinueClicked()

        // Then
        assertThat(latestEvent).isEqualTo(NavigateToStoreProfiler)
    }

    @Test
    fun `when onCancelPressed happens, then the tracks and events are triggered as expected`() {
        // Given
        val storeCreationSource = "test source"
        prefsWrapper = mock {
            on { getStoreCreationSource() } doReturn storeCreationSource
        }
        analyticsTracker = mock()

        sut = StoreNamePickerViewModel(
            savedStateHandle = savedState,
            newStore = mock(),
            analyticsTrackerWrapper = analyticsTracker,
            prefsWrapper = prefsWrapper,
            localNotificationScheduler,
            isRemoteFeatureFlagEnabled,
            accountStore
        )

        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onCancelPressed()

        // Then
        verify(analyticsTracker).track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )

        verify(analyticsTracker).track(
            AnalyticsEvent.SITE_CREATION_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                AnalyticsTracker.KEY_SOURCE to storeCreationSource,
                AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
            )
        )
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when viewModel is created, then the site creation step track is triggered`() {
        verify(analyticsTracker).track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )
    }

    @Test
    fun `when onExitTriggered happens, then trigger Exit event`() {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onExitTriggered()

        // Then
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when onHelpPressed happens, then trigger NavigateToHelpScreen event`() {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onHelpPressed()

        // Then
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.STORE_CREATION))
    }


    companion object {
        val TEST_ACCOUNT = AccountModel().apply {
            userId = 123L
            email = "mail@a8c.com"
            userName = "username"
        }
    }
}

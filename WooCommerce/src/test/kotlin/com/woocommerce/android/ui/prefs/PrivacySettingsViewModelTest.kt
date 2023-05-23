package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    private val fakeSharedPreferencesEmitter = MutableStateFlow(false)

    private val appPrefs: AppPrefsWrapper = mock()
    private val repository: PrivacySettingsRepository = mock {
        on { isUserWPCOM() } doReturn true
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    val analyticsTrackerWrapper: AnalyticsTrackerWrapper = object : AnalyticsTrackerWrapper() {
        override fun observeSendUsageStats(): Flow<Boolean> = fakeSharedPreferencesEmitter

        override var sendUsageStats: Boolean = false
            set(value) {
                runBlocking {
                    fakeSharedPreferencesEmitter.emit(value)
                }
                field = value
            }
    }

    lateinit var sut: PrivacySettingsViewModel

    fun init() {
        sut = PrivacySettingsViewModel(
            mock(),
            appPrefs,
            analyticsTrackerWrapper,
            resourceProvider,
            repository,
        )
        sut.state.observeForever { }
    }

    @Test
    fun `given successful API response, when user turns on analytical events, turn on analytical events and update state`(): Unit =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                onBlocking { updateTracksSetting(true) } doReturn Result.success(Unit)
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isTrue
            assertThat(analyticsTrackerWrapper.sendUsageStats).isTrue
        }

    @Test
    fun `given failed API response, when user turns on tracking analytical events, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                onBlocking { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionSnackbar::class.java)
        }

    @Test
    fun `given failed API response, when user opens the screen, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                onBlocking { updateAccountSettings() } doReturn Result.failure(Exception())
            }

            // when
            init()
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionSnackbar::class.java)
        }

    @Test
    fun `given user is not WPCOM, when user opens the screen, load settings from local preferences`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn false
            }
            analyticsTrackerWrapper.sendUsageStats = false

            // when
            init()
            runCurrent()

            // then
            assertThat(analyticsTrackerWrapper.sendUsageStats).isFalse
            verify(repository, never()).updateAccountSettings()
            assertThat(sut.state.value?.sendUsageStats).isFalse
        }

    @Test
    fun `given failed API response, when user tapps on retry button, retry updating account settings`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                onBlocking { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            with((sut.event.value as MultiLiveEvent.Event.ShowActionSnackbar)) {
                action.onClick(null)
                runCurrent()
            }
            verify(repository, times(2)).updateTracksSetting(true)
        }
}
